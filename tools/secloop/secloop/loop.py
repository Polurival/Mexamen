"""Execution loop: промпт → генерация → тесты → security review → коммит.

Логика цикла не знает ни про claude, ни про Gradle, ни про git: всё внешнее приходит через `LoopDeps`
(в тестах — фейки, в проде — `runners.py`). Так решения цикла проверяются без LLM и без сборки.
"""

from __future__ import annotations

import json
import time
from dataclasses import dataclass, field
from typing import Callable, Optional

from .review import (
    ReviewFinding,
    ReviewResult,
    decide,
    gateway_block_findings,
    security_feedback,
)

# Итоговые статусы задачи
COMMITTED_CLEAN = "committed_clean"
COMMITTED_WITH_WARNINGS = "committed_with_warnings"
TESTS_FAILED = "tests_failed"  # лимит итераций, тесты так и не позеленели → не коммитим
SECURITY_FAILED = "security_failed"  # лимит итераций, Critical/High остались → не коммитим
REVIEW_ERROR = "review_error"  # ревью не удалось получить/разобрать → fail-closed, не коммитим
NO_CHANGES = "no_changes"  # агент ничего не изменил → коммитить и ревьюить нечего


@dataclass
class GenResult:
    ok: bool
    text: str = ""
    cost_usd: float = 0.0
    turns: int = 0
    error: str = ""


@dataclass
class TestResult:
    ok: bool
    tests: int = 0
    failures: int = 0
    errors: int = 0
    summary: str = ""  # выжимка для фидбека генератору (ошибки компиляции/упавшие тесты)


@dataclass
class ReviewMaterial:
    diff: str
    files: dict  # path -> content


@dataclass
class ReviewOutcome:
    """Либо результат ревью, либо блокировка шлюзом (kinds), либо ошибка (error)."""

    result: Optional[ReviewResult] = None
    gateway_blocked_kinds: Optional[list] = None
    error: str = ""
    cost_usd: float = 0.0


@dataclass
class LoopDeps:
    generate: Callable[[str, bool], GenResult]  # (prompt, first_call)
    run_tests: Callable[[], TestResult]
    review_material: Callable[[], ReviewMaterial]  # git add -A + diff + файлы
    review: Callable[[ReviewMaterial], ReviewOutcome]
    commit: Callable[[str], str]  # message -> sha
    drain_gateway_events: Callable[[str], list]  # stage ('gen'|'review') -> новые security events
    scan_secrets: Callable[[str], list]  # текст -> findings (для file:line при блокировке шлюзом)
    log: Callable[[dict], None] = lambda event: None
    clock: Callable[[], float] = time.time


@dataclass
class Iteration:
    n: int
    gen: GenResult
    gateway_gen_events: list
    tests: Optional[TestResult] = None
    review: Optional[ReviewOutcome] = None
    gateway_review_events: list = field(default_factory=list)
    findings: list = field(default_factory=list)  # ReviewFinding
    decision: str = ""


@dataclass
class TaskResult:
    task_id: str
    status: str
    iterations: list
    commit_sha: str = ""
    warnings: list = field(default_factory=list)  # Medium/Low, прошедшие с предупреждением
    final_blocking: list = field(default_factory=list)  # что осталось блокирующим при неудаче
    seconds: float = 0.0

    def summary(self) -> dict:
        return {
            "task_id": self.task_id,
            "status": self.status,
            "iterations": len(self.iterations),
            "commit": self.commit_sha,
            "warnings": [w.to_dict() for w in self.warnings],
            "final_blocking": [f.to_dict() for f in self.final_blocking],
            "seconds": round(self.seconds, 1),
        }


def _test_feedback(tests: TestResult, gen_blocked: list) -> str:
    lines = ["Проверки не прошли — исправь и добавь/почини тесты, не ослабляя их:", tests.summary.strip()]
    if gen_blocked:
        kinds = ", ".join(sorted({f"{e.get('category') or e.get('kind')}" for e in gen_blocked}))
        lines.append(f"Заметка: LLM Gateway заблокировал часть твоего прошлого ответа ({kinds}).")
    return "\n".join(lines)


def run_task(task_id: str, title: str, task_prompt: str, deps: LoopDeps, max_iterations: int = 4) -> TaskResult:
    started = deps.clock()
    iterations: list[Iteration] = []
    prompt = task_prompt
    last_blocking: list[ReviewFinding] = []
    last_status = TESTS_FAILED

    for n in range(1, max_iterations + 1):
        deps.log({"event": "generate_start", "task": task_id, "iteration": n, "followup": n > 1})
        gen = deps.generate(prompt, n == 1)
        gen_events = deps.drain_gateway_events("gen")
        it = Iteration(n=n, gen=gen, gateway_gen_events=gen_events)
        iterations.append(it)
        deps.log(
            {
                "event": "generate_done",
                "task": task_id,
                "iteration": n,
                "ok": gen.ok,
                "cost_usd": gen.cost_usd,
                "gateway_events": len(gen_events),
                "error": gen.error,
            }
        )
        blocked_out = [e for e in gen_events if e.get("direction") == "output" and e.get("action") == "blocked"]

        tests = deps.run_tests()
        it.tests = tests
        deps.log(
            {
                "event": "tests",
                "task": task_id,
                "iteration": n,
                "ok": tests.ok,
                "tests": tests.tests,
                "failures": tests.failures,
                "errors": tests.errors,
            }
        )
        if not tests.ok:
            last_status = TESTS_FAILED
            prompt = _test_feedback(tests, blocked_out)
            it.decision = "tests_failed"
            continue

        material = deps.review_material()
        if not material.diff.strip():
            deps.log({"event": "no_changes", "task": task_id, "iteration": n})
            it.decision = "no_changes"
            return TaskResult(task_id, NO_CHANGES, iterations, seconds=deps.clock() - started)
        outcome = deps.review(material)
        it.review = outcome
        it.gateway_review_events = deps.drain_gateway_events("review")

        if outcome.error:
            deps.log({"event": "review_error", "task": task_id, "iteration": n, "error": outcome.error})
            it.decision = "review_error"
            return TaskResult(task_id, REVIEW_ERROR, iterations, seconds=deps.clock() - started)

        if outcome.gateway_blocked_kinds is not None:
            findings = gateway_block_findings(material.files, outcome.gateway_blocked_kinds, deps.scan_secrets)
            if not findings:
                msg = (
                    f"Gateway заблокировал материал ревью ({', '.join(outcome.gateway_blocked_kinds)}), но в изменённых файлах "
                    "ничего не найдено: блокировка вызвана не кодом (контекст клиента/конфигурация шлюза). Fail-closed."
                )
                deps.log({"event": "review_error", "task": task_id, "iteration": n, "error": msg})
                it.decision = "review_error"
                return TaskResult(task_id, REVIEW_ERROR, iterations, seconds=deps.clock() - started)
            deps.log(
                {
                    "event": "review_blocked_by_gateway",
                    "task": task_id,
                    "iteration": n,
                    "kinds": outcome.gateway_blocked_kinds,
                    "located": [f"{f.file}:{f.line}" for f in findings],
                }
            )
        else:
            findings = outcome.result.findings if outcome.result else []
        it.findings = findings

        decision = decide(findings)
        it.decision = decision.action
        deps.log(
            {
                "event": "security_decision",
                "task": task_id,
                "iteration": n,
                "decision": decision.action,
                "findings": [f.to_dict() for f in findings],
            }
        )

        if decision.action == "fix":
            last_status = SECURITY_FAILED
            last_blocking = decision.blocking
            prompt = security_feedback(decision.blocking)
            continue

        for w in decision.warnings:
            deps.log(
                {
                    "event": "security_warning",
                    "task": task_id,
                    "severity": w.severity,
                    "where": f"{w.file}:{w.line}",
                    "title": w.title,
                }
            )
        sha = deps.commit(_commit_message(task_id, title, decision.action, decision.warnings, n))
        deps.log({"event": "commit", "task": task_id, "sha": sha, "decision": decision.action})
        status = COMMITTED_WITH_WARNINGS if decision.warnings else COMMITTED_CLEAN
        return TaskResult(
            task_id, status, iterations, commit_sha=sha, warnings=decision.warnings, seconds=deps.clock() - started
        )

    return TaskResult(
        task_id,
        last_status,
        iterations,
        final_blocking=last_blocking if last_status == SECURITY_FAILED else [],
        seconds=deps.clock() - started,
    )


def _commit_message(task_id: str, title: str, action: str, warnings: list, iterations: int) -> str:
    head = f"secloop({task_id}): {title}"
    body = [f"security review: {'чисто' if action == 'commit_clean' else 'пройден с предупреждениями'}; итераций: {iterations}"]
    for w in warnings:
        body.append(f"- [{w.severity.upper()}] {w.title} ({w.file}:{w.line})")
    return head + "\n\n" + "\n".join(body) + "\n\nCo-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"


def dumps_event(event: dict) -> str:
    return json.dumps(event, ensure_ascii=False)
