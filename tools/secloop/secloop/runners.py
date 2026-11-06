"""Внешний мир цикла: Gateway-процессы, `claude -p`, Gradle, git-worktree.

Весь трафик к LLM идёт через Gateway: каждому `claude` задаётся ANTHROPIC_BASE_URL на свой шлюз.
Два шлюза — разные политики по этапам:
  gen    — вход `redact` (секреты маскируются, работа продолжается), выход `block`;
  review — вход `block` (секрет в коде → ревью не уходит в LLM, это Critical), выход `block`.
"""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import time
import urllib.request
import uuid
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from typing import Optional

from .evidence import read_jsonl
from .loop import GenResult, ReviewMaterial, ReviewOutcome, TestResult
from .review import CODE_SUFFIXES, REVIEW_SCHEMA, ReviewParseError, build_review_input, parse_review

GATEWAY_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "gateway")

GEN_ALLOWED_TOOLS = ",".join(
    [
        "Read", "Grep", "Glob", "Edit", "Write",
        "Bash(./gradlew *)", "Bash(cd *)", "Bash(grep *)", "Bash(ls *)", "Bash(cat *)", "Bash(find *)", "Bash(wc *)",
        "Bash(rm -rf composeApp/build/test-results*)", "Bash(git status*)", "Bash(git diff*)",
        "Bash(python3 *)",
    ]
)


# ------------------------------------------------------------------ Gateway


class GatewayProc:
    def __init__(self, name: str, port: int, input_mode: str, output_mode: str, log_dir: str, email_allow: str = "") -> None:
        self.name, self.port, self.input_mode, self.output_mode, self.log_dir = name, port, input_mode, output_mode, log_dir
        self.email_allow = email_allow
        self.proc: Optional[subprocess.Popen] = None
        self._offset = 0
        self.buffer: list[dict] = []

    @property
    def url(self) -> str:
        return f"http://127.0.0.1:{self.port}"

    @property
    def audit_path(self) -> str:
        return os.path.join(self.log_dir, "audit.jsonl")

    @property
    def events_path(self) -> str:
        return os.path.join(self.log_dir, "security_events.jsonl")

    def start(self) -> None:
        os.makedirs(self.log_dir, exist_ok=True)
        env = {**os.environ, "GATEWAY_LOG_MAX_CHARS": "0", "GATEWAY_EMAIL_ALLOW": self.email_allow}  # полные санитизированные тела — для проверки утечек
        out = open(os.path.join(self.log_dir, "gateway.out"), "w", encoding="utf-8")
        self.proc = subprocess.Popen(
            [
                sys.executable, "-m", "llm_gateway", "--port", str(self.port),
                "--input-mode", self.input_mode, "--output-mode", self.output_mode,
                "--rate-limit", "0", "--log-dir", self.log_dir,
            ],
            cwd=GATEWAY_DIR, env=env, stdout=out, stderr=subprocess.STDOUT,
        )
        for _ in range(50):
            try:
                with urllib.request.urlopen(self.url + "/_gateway/health", timeout=1):
                    return
            except OSError:
                time.sleep(0.2)
        raise RuntimeError(f"gateway {self.name} не поднялся на порту {self.port}")

    def stop(self) -> None:
        if self.proc and self.proc.poll() is None:
            self.proc.terminate()
            try:
                self.proc.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self.proc.kill()

    def poll(self) -> None:
        events = read_jsonl(self.events_path)
        self.buffer.extend(events[self._offset :])
        self._offset = len(events)

    def drain(self) -> list[dict]:
        self.poll()
        out, self.buffer = self.buffer, []
        return out


# ------------------------------------------------------------------ claude -p


def _claude_json(proc: subprocess.CompletedProcess) -> dict:
    try:
        data = json.loads(proc.stdout)
        if isinstance(data, dict):
            return data
    except ValueError:
        pass
    return {"is_error": True, "result": (proc.stderr or proc.stdout or "")[-2000:]}


@dataclass
class ClaudeCall:
    base_url: str
    cwd: str
    model: str
    timeout: int = 1800

    def run(
        self,
        prompt: str,
        *,
        agent: Optional[str] = None,
        system_prompt: Optional[str] = None,
        tools: Optional[str] = None,
        allowed_tools: Optional[str] = None,
        json_schema: Optional[dict] = None,
        session_id: Optional[str] = None,
        resume: bool = False,
        persist: bool = True,
        append_system_prompt: Optional[str] = None,
        max_budget_usd: float = 8.0,
        extra_env: Optional[dict] = None,
    ) -> dict:
        cmd = [
            "claude", "-p", "--model", self.model, "--output-format", "json",
            "--setting-sources", "project", "--strict-mcp-config", "--max-budget-usd", str(max_budget_usd),
        ]
        if agent:
            cmd += ["--agent", agent]
        if system_prompt is not None:
            cmd += ["--system-prompt", system_prompt]
        if append_system_prompt:
            cmd += ["--append-system-prompt", append_system_prompt]
        if tools is not None:
            cmd += ["--tools", tools]
        if allowed_tools:
            cmd += ["--allowedTools", allowed_tools, "--permission-mode", "dontAsk"]
        if json_schema is not None:
            cmd += ["--json-schema", json.dumps(json_schema)]
        if session_id:
            cmd += ["--resume" if resume else "--session-id", session_id]
        if not persist:
            cmd += ["--no-session-persistence"]
        env = {**os.environ, "ANTHROPIC_BASE_URL": self.base_url, **(extra_env or {})}
        try:
            proc = subprocess.run(
                cmd, input=prompt, capture_output=True, text=True, cwd=self.cwd, env=env, timeout=self.timeout
            )
        except subprocess.TimeoutExpired:
            return {"is_error": True, "result": f"таймаут {self.timeout} с"}
        return _claude_json(proc)


# ------------------------------------------------------------------ Gradle


def parse_test_xml(results_dir: str) -> tuple[int, int, int]:
    tests = failures = errors = 0
    if not os.path.isdir(results_dir):
        return 0, 0, 0
    for name in os.listdir(results_dir):
        if name.endswith(".xml"):
            try:
                root = ET.parse(os.path.join(results_dir, name)).getroot()
            except ET.ParseError:
                continue
            tests += int(root.get("tests", 0))
            failures += int(root.get("failures", 0))
            errors += int(root.get("errors", 0))
    return tests, failures, errors


_FAIL_LINE = re.compile(r"^(e: |.*FAILED|.*Unresolved reference|.*AssertionError|.*expected:|Caused by|\* What went wrong)")


def summarize_gradle_failure(output: str, limit: int = 70) -> str:
    lines = output.split("\n")
    picked: list[str] = []
    for i, line in enumerate(lines):
        if _FAIL_LINE.match(line.strip()) or line.startswith("e: "):
            picked.append(line)
            if line.startswith("* What went wrong"):
                picked.extend(lines[i + 1 : i + 6])
    picked = picked[:limit] or lines[-limit:]
    return "\n".join(picked)


class GradleRunner:
    def __init__(self, cwd: str, tasks: list[str], timeout: int = 3000, log_dir: str = "") -> None:
        self.cwd, self.tasks, self.timeout, self.log_dir = cwd, tasks, timeout, log_dir
        self.runs = 0

    def run(self) -> TestResult:
        results = os.path.join(self.cwd, "composeApp/build/test-results/testAndroidHostTest")
        subprocess.run(["rm", "-rf", results], check=False)
        self.runs += 1
        try:
            proc = subprocess.run(
                ["./gradlew", *self.tasks, "--console=plain"],
                cwd=self.cwd, capture_output=True, text=True, timeout=self.timeout,
            )
            output, code = proc.stdout + "\n" + proc.stderr, proc.returncode
        except subprocess.TimeoutExpired:
            output, code = f"таймаут Gradle {self.timeout} с", 124
        if self.log_dir:
            os.makedirs(self.log_dir, exist_ok=True)
            with open(os.path.join(self.log_dir, f"gradle-{self.runs}.txt"), "w", encoding="utf-8") as fh:
                fh.write(output)
        tests, failures, errors = parse_test_xml(results)
        ok = code == 0 and failures == 0 and errors == 0 and tests > 0
        summary = "" if ok else summarize_gradle_failure(output)
        if code == 0 and tests == 0:
            summary = "Gradle завершился успешно, но XML-отчётов тестов нет (tests=0)."
        return TestResult(ok, tests, failures, errors, summary)


# ------------------------------------------------------------------ git worktree


class Worktree:
    def __init__(self, repo: str, path: str) -> None:
        self.repo, self.path = repo, path

    def _git(self, *args: str, cwd: Optional[str] = None, check: bool = True) -> str:
        proc = subprocess.run(["git", *args], cwd=cwd or self.path, capture_output=True, text=True, check=False)
        if check and proc.returncode != 0:
            raise RuntimeError(f"git {' '.join(args)}: {proc.stderr.strip()}")
        return proc.stdout

    def prepare(self, branch: str, base: str = "main") -> None:
        """Чистая рабочая копия от `base` на новой ветке. build/ и .gradle сохраняются — кэш сборки."""
        if not os.path.isdir(os.path.join(self.path, ".git")) and not os.path.isfile(os.path.join(self.path, ".git")):
            os.makedirs(os.path.dirname(self.path), exist_ok=True)
            self._git("worktree", "add", "-B", branch, self.path, base, cwd=self.repo)
        else:
            self._git("reset", "--hard", "-q")
            self._git("clean", "-fdq")
            self._git("checkout", "-q", "-B", branch, base)

    def write_file(self, rel: str, text: str) -> None:
        with open(os.path.join(self.path, rel), "w", encoding="utf-8") as fh:
            fh.write(text)

    def stage_and_diff(self) -> tuple[str, dict]:
        self._git("add", "-A")
        diff = self._git("diff", "--cached", "-U5")
        names = self._git("diff", "--cached", "--name-only", "--diff-filter=AMR").split()
        files: dict[str, str] = {}
        for rel in names:
            if rel.endswith(CODE_SUFFIXES) and "/build/" not in rel:
                try:
                    with open(os.path.join(self.path, rel), encoding="utf-8") as fh:
                        files[rel] = fh.read()
                except (OSError, UnicodeDecodeError):
                    continue
        # Контекст безопасности, нужный ревьюеру, даже если файлы не менялись
        for rel in ("androidApp/src/main/AndroidManifest.xml", "composeApp/src/androidMain/AndroidManifest.xml"):
            if rel not in files and os.path.exists(os.path.join(self.path, rel)):
                with open(os.path.join(self.path, rel), encoding="utf-8") as fh:
                    files[rel] = fh.read()
        return diff, files

    def commit(self, message: str) -> str:
        self._git("add", "-A")
        self._git("commit", "-q", "-m", message)
        return self._git("rev-parse", "--short", "HEAD").strip()

    def full_diff_vs(self, base: str = "main") -> str:
        return self._git("diff", f"{base}...HEAD", "-U3")


# ------------------------------------------------------------------ сборка LoopDeps


def build_gen_prompt(first: bool, prompt: str) -> str:
    if not first:
        return prompt
    return (
        "Это автоматический запуск execution loop. Вместо GitHub issue — задача ниже (`gh issue view` не нужен).\n"
        "Коммит сделает оркестратор, ты не коммитишь. Следуй CLAUDE.md и своему профилю; после правок прогони Gradle-проверки.\n"
        "Текущий каталог — корень проекта: запускай команды прямо оттуда (`./gradlew …`), без `cd` и без составных команд через `&&`.\n\n"
        "## Задача\n" + prompt
    )


def make_generate(claude: ClaudeCall, run_dir: str):
    session = {"id": str(uuid.uuid4()), "n": 0}

    def generate(prompt: str, first: bool) -> GenResult:
        session["n"] += 1
        full = build_gen_prompt(first, prompt)
        with open(os.path.join(run_dir, f"gen-{session['n']}-prompt.md"), "w", encoding="utf-8") as fh:
            fh.write(full)
        data = claude.run(
            full, agent="feature", allowed_tools=GEN_ALLOWED_TOOLS, session_id=session["id"], resume=not first,
            max_budget_usd=8.0,
        )
        with open(os.path.join(run_dir, f"gen-{session['n']}-result.json"), "w", encoding="utf-8") as fh:
            json.dump(data, fh, ensure_ascii=False, indent=1)
        return GenResult(
            ok=not data.get("is_error", False),
            text=str(data.get("result", ""))[:4000],
            cost_usd=float(data.get("total_cost_usd") or 0),
            turns=int(data.get("num_turns") or 0),
            error=str(data.get("result", ""))[:500] if data.get("is_error") else "",
        )

    return generate


def make_review(claude: ClaudeCall, gw: GatewayProc, system_prompt: str, run_dir: str):
    counter = {"n": 0}

    def review(material: ReviewMaterial) -> ReviewOutcome:
        counter["n"] += 1
        user_input = "Review this change.\n\n" + build_review_input(material.diff, material.files)
        with open(os.path.join(run_dir, f"review-{counter['n']}-input.md"), "w", encoding="utf-8") as fh:
            fh.write(user_input)
        data = claude.run(
            user_input, system_prompt=system_prompt, tools="", json_schema=REVIEW_SCHEMA, persist=False,
            max_budget_usd=4.0, extra_env={"CLAUDE_CODE_DISABLE_CLAUDE_MDS": "1"},
        )
        with open(os.path.join(run_dir, f"review-{counter['n']}-result.json"), "w", encoding="utf-8") as fh:
            json.dump(data, fh, ensure_ascii=False, indent=1)
        cost = float(data.get("total_cost_usd") or 0)

        gw.poll()
        blocked = [e for e in gw.buffer if e.get("direction") == "input" and e.get("action") == "blocked"]
        if blocked:
            return ReviewOutcome(gateway_blocked_kinds=sorted({e.get("kind", "") for e in blocked}), cost_usd=cost)
        if data.get("is_error"):
            return ReviewOutcome(error=f"ревью-вызов упал: {str(data.get('result', ''))[:300]}", cost_usd=cost)
        try:
            payload = data.get("structured_output") or data.get("result", "")
            return ReviewOutcome(result=parse_review(payload), cost_usd=cost)
        except ReviewParseError as exc:
            return ReviewOutcome(error=str(exc), cost_usd=cost)

    return review
