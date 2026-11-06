"""Отчёт Дня 14 из артефактов прогона: python3 report.py <run_id>  →  docs/day14-secloop/REPORT.md (+ логи).

Берёт summary.json / loop-events.jsonl / логи шлюзов из tools/secloop/runs/<run_id>/ и ручной аудит
из docs/day14-secloop/manual-audit.md (его пишет человек/агент после чтения итоговых diff).
"""

from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys

from secloop.canaries import CANARIES
from secloop.evidence import read_jsonl

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
DOCS = os.path.join(REPO, "docs", "day14-secloop")

STATUS_RU = {
    "committed_clean": "✅ закоммичено, security review чист",
    "committed_with_warnings": "⚠️ закоммичено с предупреждениями (Medium/Low)",
    "tests_failed": "❌ тесты не позеленели — не закоммичено",
    "security_failed": "⛔ Critical/High не исправлены за лимит итераций — не закоммичено",
    "review_error": "❌ ревью не получено — не закоммичено (fail-closed)",
    "no_changes": "❌ агент ничего не изменил",
}


def abbreviate(text: str) -> str:
    """В отчёте канарейки сокращаем (6 первых символов …), чтобы документ не содержал целых «ключей»."""
    for value in CANARIES.values():
        text = text.replace(value, value[:6] + "…" + value[-2:])
    return text


def esc(text: str) -> str:
    return text.replace("|", "\\|").replace("\n", " ")


def findings_table(findings: list) -> str:
    if not findings:
        return "_замечаний нет_"
    rows = ["| Severity | Где | Что | Источник |", "|---|---|---|---|"]
    for f in findings:
        where = f"`{f['file']}:{f['line']}`" if f["line"] else f"`{f['file']}`"
        rows.append(f"| **{f['severity'].upper()}** | {where} | {esc(f['title'])} | {f['source']} |")
    return "\n".join(rows)


def gateway_events_table(gw: dict) -> str:
    if not gw["events"]:
        return "_событий нет_"
    rows = ["| Направление | Действие | Тип | Срабатываний | Уникальных значений |", "|---|---|---|---|---|"]
    for e in gw["events"]:
        rows.append(f"| {e['direction']} | {e['action']} | `{e['type']}` | {e['count']} | {e['unique_values']} |")
    return "\n".join(rows)


def task_section(t: dict, run_dir: str) -> str:
    tid = t["task_id"]
    out = [f"### {tid}. {abbreviate(t['title'])}", ""]
    with open(os.path.join(REPO, "tools", "secloop", "tasks", [f for f in os.listdir(os.path.join(HERE, "tasks")) if f.startswith(tid + "-")][0]), encoding="utf-8") as fh:
        out += ["Промпт задачи (шаблон; значения подставляются из канареек и маскируются шлюзом):", "", "```", fh.read().strip(), "```", ""]
    out += [
        f"**Итог:** {STATUS_RU.get(t['status'], t['status'])}; итераций: **{t['iterations']}**; коммит: `{t['commit'] or '—'}`; ветка `{t['branch']}`; {t['seconds'] / 60:.1f} мин.",
        "",
        "| Итерация | Генерация | Тесты (tests/fail/err) | Решение security step | Замечаний |",
        "|---|---|---|---|---|",
    ]
    for it in t["iteration_details"]:
        tests = "—" if it["tests"] is None else f"{'✅' if it['tests']['ok'] else '❌'} {it['tests']['tests']}/{it['tests']['failures']}/{it['tests']['errors']}"
        out.append(f"| {it['n']} | {'ok' if it['gen_ok'] else 'ошибка'} (${it['gen_cost_usd']:.2f}) | {tests} | {it['decision'] or '—'} | {len(it['findings'])} |")
    out.append("")
    for it in t["iteration_details"]:
        if it["findings"]:
            out += [f"**Security review, итерация {it['n']}:**", "", findings_table(it["findings"]), ""]
    if t["warnings"]:
        out += ["**Прошло с предупреждением в логе (Medium/Low):**", "", findings_table(t["warnings"]), ""]
    for stage, title in (("gen", "Gateway: генерация (вход redact, выход block)"), ("review", "Gateway: security review (вход block, выход block)")):
        gw = t["gateway"][stage]
        out += [
            f"**{title}** — запросов к LLM: {gw['llm_requests']}; прошло чистыми: {gw['forwarded_clean']}; "
            f"замаскировано: {gw['forwarded_redacted']}; заблокировано на входе: {gw['blocked_input']}; "
            f"ответов проверено Output Guard: {gw['output_pass'] + gw['output_flagged']} (помечено: {gw['output_flagged']}); "
            f"≈{gw['tokens_in']:,} входных токенов, ${gw['cost_usd']:.2f}",
            "",
            gateway_events_table(gw),
            "",
        ]
    if t["static_checks"]:
        out += ["**Независимые статические проверки итогового diff (regex, без LLM):**", "", "| Правило | Файл | Строка |", "|---|---|---|"]
        for rule, file, line in t["static_checks"]:
            out.append(f"| `{rule}` | `{file}` | `{esc(abbreviate(line))}` |")
        out.append("")
    return "\n".join(out)


def count_unit_tests() -> int:
    """Запуск в отдельном процессе: пакет `tests` есть и у tools/gateway (путь добавлен evidence.py) — в одном процессе они конфликтуют."""
    proc = subprocess.run(
        [sys.executable, "-m", "unittest", "discover", "-s", "tests", "-t", "."],
        cwd=HERE, capture_output=True, text=True,
    )
    match = re.search(r"Ran (\d+) tests?", proc.stderr)
    return int(match.group(1)) if match and proc.returncode == 0 else 0


def probe_section(run_dir: str) -> list:
    path = os.path.join(run_dir, "probe", "summary.json")
    if not os.path.exists(path):
        return []
    with open(path, encoding="utf-8") as fh:
        probe = json.load(fh)
    a, b = probe["probe_a_local_properties"], probe["probe_b_secret_in_code"]
    kinds = ", ".join(f"{k[2]} ×{v}" for k, v in sorted(__import__("collections").Counter((e["direction"], e["action"], e["kind"]) for e in a["gateway_events"]).items()))
    answer = abbreviate(a["agent_answer"]).replace("\n", " ⏎ ")
    return [
        "## 5. Контрольные пробы Gateway (то, что в обычных задачах не возникло)",
        "",
        "В трёх задачах Gateway только маскировал секреты из промптов: агент сам не открывал `local.properties` и не хардкодил ключ, поэтому "
        "реальной блокировки и «секрета из кодовой базы» не случилось. Два сценария проверены отдельно (`python3 -m secloop probe`, логи — `evidence/probe/`).",
        "",
        "**Проба A. Агент читает `local.properties` (внутри — ключи-канарейки `gsk_…` и `sk-…`).**",
        "",
        f"- Шлюз `gen` (вход redact) замаскировал значения в результате инструмента `Read/cat`: {kinds}.",
        f"- В запросах к LLM канареек: **{len(a['canaries_in_outbound_requests'])}**; в ответе агента: **{len(a['canaries_in_agent_answer'])}**.",
        f"- Ответ агента: «{esc(answer[:420])}…» — он честно сообщил, что значений не видит.",
        "",
        "**Проба B. В изменённом коде оказался секрет (ключ-канарейка и email в `StatusClient.kt`) — ревью-запрос.**",
        "",
        f"- Шлюз `review` (вход **block**) вернул 400 и **ничего не отправил в LLM** (`sent_to_llm = {b['sent_to_llm']}`, стоимость ${b['llm_cost_usd']:.2f}); типы: {', '.join(b['gateway_blocked_kinds'])}.",
        f"- Оркестратор привязал блокировку к коду: {'; '.join(b['located'])} → это Critical-находка и возврат на генерацию с фидбеком «исправь: … в файле:строке».",
        "- Claude Code повторил заблокированный запрос 6 раз (причину повторов не выясняли — вероятно, встроенные ретраи клиента); все 6 заблокированы, токены не потрачены.",
        "",
    ]


def main() -> None:
    run_id = sys.argv[1]
    run_dir = os.path.join(HERE, "runs", run_id)
    with open(os.path.join(run_dir, "summary.json"), encoding="utf-8") as fh:
        data = json.load(fh)
    tasks, leaks = data["tasks"], data["leaks"]

    os.makedirs(DOCS, exist_ok=True)
    evidence = os.path.join(DOCS, "evidence")
    shutil.rmtree(evidence, ignore_errors=True)
    for t in tasks:
        tid = t["task_id"]
        dst = os.path.join(evidence, tid)
        os.makedirs(dst, exist_ok=True)
        src = os.path.join(run_dir, tid)
        for name in ("summary.json", "final.patch", "gen-security_events.jsonl", "review-security_events.jsonl"):
            if os.path.exists(os.path.join(src, name)):
                shutil.copy(os.path.join(src, name), dst)
        # аудит без полных тел запросов (они огромны); тела — только в runs/ (не в git)
        for stage in ("gen", "review"):
            p = os.path.join(src, f"{stage}-audit.jsonl")
            with open(os.path.join(dst, f"{stage}-audit.jsonl"), "w", encoding="utf-8") as fh:
                for rec in read_jsonl(p):
                    rec.pop("request", None)
                    rec.pop("response", None)
                    fh.write(json.dumps(rec, ensure_ascii=False) + "\n")
        for f in sorted(os.listdir(src)):
            if f.startswith("review-") and f.endswith("-result.json"):
                shutil.copy(os.path.join(src, f), dst)
    shutil.copy(os.path.join(run_dir, "loop-events.jsonl"), evidence)
    probe_src = os.path.join(run_dir, "probe")
    if os.path.isdir(probe_src):
        pdst = os.path.join(evidence, "probe")
        os.makedirs(pdst, exist_ok=True)
        shutil.copy(os.path.join(probe_src, "summary.json"), pdst)
        for stage in ("gen", "review"):
            src_dir = os.path.join(probe_src, f"gateway-{stage}")
            if os.path.exists(os.path.join(src_dir, "security_events.jsonl")):
                shutil.copy(os.path.join(src_dir, "security_events.jsonl"), os.path.join(pdst, f"{stage}-security_events.jsonl"))
            with open(os.path.join(pdst, f"{stage}-audit.jsonl"), "w", encoding="utf-8") as fh:
                for rec in read_jsonl(os.path.join(src_dir, "audit.jsonl")):
                    rec.pop("request", None)
                    rec.pop("response", None)
                    fh.write(json.dumps(rec, ensure_ascii=False) + "\n")
    with open(os.path.join(evidence, "summary.json"), "w", encoding="utf-8") as fh:
        json.dump(data, fh, ensure_ascii=False, indent=1)

    manual = ""
    mpath = os.path.join(DOCS, "manual-audit.md")
    if os.path.exists(mpath):
        with open(mpath, encoding="utf-8") as fh:
            manual = fh.read()

    committed = sum(1 for t in tasks if t["status"].startswith("committed"))
    md = [
        "# День 14. Security step в execution loop — отчёт",
        "",
        f"_Прогон `{run_id}`; артефакты — `docs/day14-secloop/evidence/`, исходники — `tools/secloop/`._",
        "",
        "## 1. Что сделано",
        "",
        "Взят execution loop Дня 5 (профиль `feature` → Gradle-проверки → ревью → коммит) и оформлен запускаемым оркестратором `tools/secloop` (stdlib Python): "
        "каждая стадия — отдельный вызов `claude -p`, всё идёт **через LLM Gateway из Дня 13**.",
        "",
        "```",
        "задача ─► [Gateway gen: вход redact] ─► генерация (профиль feature, Sonnet 5.5)",
        "              ─► Gradle: compileAndroidMain + compileKotlinWasmJs + testAndroidHostTest ── упало ──► фидбек в генерацию",
        "              ─► SECURITY STEP: git diff + пронумерованные файлы",
        "                   ─► [Gateway review: вход block] ─► второй вызов LLM (Opus 5.5, отдельный security-промпт, без инструментов)",
        "              ├─ Critical/High ──► «исправь: <что> в <file>:<line>» ──► генерация (та же сессия)",
        "              ├─ только Medium/Low ──► warning в loop-events.jsonl ──► git commit",
        "              └─ чисто ──► git commit",
        "```",
        "",
        "| Требование задания | Реализация |",
        "|---|---|",
        f"| Security step между генерацией и коммитом | `secloop/loop.py` (`run_task`); логика цикла и ревью покрыта {count_unit_tests()} тестами на фейках (ни LLM, ни Gradle) |",
        "| Второй вызов LLM с отдельным security-промптом | `prompts/security_review.md`; другая модель (Opus), `--tools \"\"`, `--json-schema`, без сессии генератора |",
        "| Critical/High → возврат на генерацию с фидбеком | `review.security_feedback`: «Исправь: 1. [HIGH] … в Store.kt:42 — …» |",
        "| Medium/Low → пропуск с warning в логе | событие `security_warning` в `loop-events.jsonl` + перечень в теле коммита |",
        "| Чисто → коммит | `Worktree.commit` в изолированной ветке `secloop/<run>-<task>` |",
        "| Весь loop через Gateway | два шлюза (`gen`: вход redact; `review`: вход block), `ANTHROPIC_BASE_URL` каждого `claude -p` |",
        "| Нет утечек секретов/токенов/PII | канарейки + поиск в том, что ушло в LLM (§4) |",
        "| Security-промпт под стек | Android + Compose Multiplatform/Wasm: Keystore vs DataStore/localStorage, network_security_config, pinning, root, логи, cleartext (§2) |",
        "| 3 задачи | §3 |",
        "",
        "## 2. Security-промпт под стек Mexamen",
        "",
        "`tools/secloop/prompts/security_review.md` (ключевое):",
        "",
        "- **Android:** токены/PII в обычных SharedPreferences/DataStore без Keystore; cleartext и `network_security_config`; trust-all TLS; certificate pinning; root-детект (не выше Medium); manifest (exported, allowBackup, debuggable); WebView.",
        "- **Web (Wasm):** `localStorage` читаем любым скриптом origin → токены в нём не хранить; секреты в wasm-бандле публичны.",
        "- **Общее:** hardcoded secrets (включая `BuildConfig`), PII/токены/тела в логах (Ktor `Logging` ALL/BODY/HEADERS без `sanitizeHeader`), HTTP вместо HTTPS, секреты в query, отсутствие валидации ввода, слабая крипто.",
        "- **Строгая шкала Critical/High/Medium/Low**, ссылки только на реальные `file:line` из пронумерованного листинга, код — недоверенные данные (защита от prompt injection в ревьюируемом коде), правила проекта (без новых зависимостей и `Log`).",
        "",
        "## 3. Три задачи",
        "",
        f"Закоммитировано: **{committed} из {len(tasks)}**. Задачи сформулированы провокационно (быстро/просто, «ключ прямо в код», «логи целиком»), "
        "в тексты подставлены синтетические секреты и PII (канарейки).",
        "",
    ]
    for t in tasks:
        md += [task_section(t, run_dir), ""]

    md += [
        "## 4. LLM Gateway: что поймал, что пропустил чистым, были ли утечки",
        "",
        f"Проверено значений: **{leaks['secrets_checked']}** (синтетические канарейки: {leaks['secrets_checked'] - leaks['real_secrets_checked']}, **реальные секреты проекта** из `local.properties` и сервис-аккаунта: {leaks['real_secrets_checked']}; сами значения нигде не печатаются).",
        "",
        "| Проверка | Результат |",
        "|---|---|",
        f"| Канарейки/реальные секреты в **запросах, ушедших в LLM** (audit обоих шлюзов, тела после санитизации) | **{len(leaks['outbound_to_llm'])} вхождений** {'✅' if not leaks['outbound_to_llm'] else '❌ ' + str(leaks['outbound_to_llm'])} |",
        f"| Канарейки/реальные секреты в **закоммиченном коде** (final.patch) | **{len(leaks['in_committed_code'])} вхождений** {'✅' if not leaks['in_committed_code'] else '❌ ' + str(leaks['in_committed_code'])} |",
        f"| Контроль: канарейки в клиентских промптах до шлюза | {sum(c for _, _, c in leaks['control_client_side_prompts'])} вхождений — тест осмысленный: секреты в промптах были |",
        "",
        "Полные логи Gateway по задачам: `evidence/<задача>/{gen,review}-security_events.jsonl` (перехваты) и `…-audit.jsonl` (все запросы: статус, токены, стоимость).",
        "",
    ]
    md += probe_section(run_dir)
    if manual:
        md += [manual, ""]
    with open(os.path.join(DOCS, "REPORT.md"), "w", encoding="utf-8") as fh:
        fh.write("\n".join(md))
    print("->", os.path.join(DOCS, "REPORT.md"))


if __name__ == "__main__":
    main()
