#!/usr/bin/env python3
"""Раннер smoke-сценариев: парсит ```steps``` из smoke/scenarios/*.md и гоняет их на устройстве через adb.
Это запасной путь для CI/терминала; в сессии с MCP claude-in-mobile агент выполняет те же шаги инструментами MCP.

Использование: run_smoke.py [--out smoke/reports/<имя>] [S1 S3 ...]
Выход: <out>/report.md + <out>/*.png (скриншот после каждого шага). Код возврата 1, если что-то упало.
"""
import argparse, datetime, html, pathlib, re, subprocess, sys, time

ROOT = pathlib.Path(__file__).resolve().parent.parent
ADBX = str(ROOT / "tools" / "adbx.sh")
XML = pathlib.Path("/tmp/mexamen_ui.xml")


def sh(*args, check=True):
    return subprocess.run(args, capture_output=True, text=True, check=check)


def dump():
    sh(ADBX, "dump")
    return XML.read_text(encoding="utf-8")


def texts(xml):
    out = []
    for m in re.finditer(r"<node [^>]*>", xml):
        n = m.group(0)
        t = html.unescape((re.search(r' text="([^"]*)"', n) or [0, ""])[1])
        d = html.unescape((re.search(r'content-desc="([^"]*)"', n) or [0, ""])[1])
        out.append((t, d))
    return out


def visible(xml, needle):
    return any(needle in t or needle in d for t, d in texts(xml))


def wait_for(needle, present=True, timeout=8):
    end = time.time() + timeout
    while time.time() < end:
        if visible(dump(), needle) == present:
            return True
        time.sleep(0.7)
    return False


def tap_option(n):
    xml = dump()
    boxes = []
    for m in re.finditer(r"<node [^>]*>", xml):
        node = m.group(0)
        if 'clickable="true"' in node:
            x1, y1, x2, y2 = map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]', node).groups())
            if y1 > 500:  # ниже верхней панели (кнопка «Cerrar» не входит)
                boxes.append(((x1 + x2) // 2, (y1 + y2) // 2))
    if len(boxes) < n:
        raise AssertionError(f"нет варианта #{n}, найдено кликабельных: {len(boxes)}")
    x, y = boxes[n - 1]
    sh("adb", "shell", "input", "tap", str(x), str(y))


def value_after(xml, label):
    ts = [t for t, d in texts(xml) if t]
    for i, t in enumerate(ts):
        if label in t and i + 1 < len(ts):
            return ts[i + 1]
    return None


def run_step(step):
    cmd, _, arg = step.partition(":")
    arg = arg.strip()
    if step == "clear":
        sh(ADBX, "clear")
    elif step == "launch":
        sh(ADBX, "restart"); time.sleep(3)
    elif step == "back":
        sh(ADBX, "back"); time.sleep(1)
    elif cmd == "tap":
        if not wait_for(arg):
            raise AssertionError(f"не найден элемент для tap: «{arg}»")
        sh(ADBX, "tap", arg); time.sleep(1.2)
    elif cmd == "tap_option":
        wait_for("Pregunta"); tap_option(int(arg)); time.sleep(1.2)
    elif cmd == "type":
        # ввод в поле с фокусом (сначала tap по полю); adb input text — только ASCII, пробел = %s
        sh("adb", "shell", "input", "text", arg.replace(" ", "%s")); time.sleep(1.2)
    elif cmd == "expect":
        if not wait_for(arg):
            raise AssertionError(f"ожидался текст «{arg}», на экране его нет")
    elif cmd == "expect_absent":
        if not wait_for(arg, present=False):
            raise AssertionError(f"текст «{arg}» не должен быть на экране")
    elif cmd == "expect_text_value":
        label, _, want = arg.partition("=")
        got = value_after(dump(), label.strip())
        if got != want.strip():
            raise AssertionError(f"«{label.strip()}»: ожидалось {want.strip()}, на экране {got}")
    else:
        raise ValueError(f"неизвестный шаг: {step}")


def parse(path):
    block = re.search(r"```steps\n(.*?)```", path.read_text(encoding="utf-8"), re.S).group(1)
    steps, i, lines = [], 0, [l.rstrip() for l in block.splitlines() if l.strip()]
    while i < len(lines):
        m = re.match(r"repeat (\d+):$", lines[i])
        if m:
            body = []
            i += 1
            while i < len(lines) and lines[i].startswith("  "):
                body.append(lines[i].strip()); i += 1
            steps += body * int(m.group(1))
        else:
            steps.append(lines[i].strip()); i += 1
    return steps


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=str(ROOT / "reports" / datetime.date.today().isoformat()))
    ap.add_argument("ids", nargs="*")
    a = ap.parse_args()
    out = pathlib.Path(a.out); out.mkdir(parents=True, exist_ok=True)
    files = sorted((ROOT / "scenarios").glob("S*.md"))
    if a.ids:
        files = [f for f in files if f.name.split("-")[0] in a.ids]
    model = sh("adb", "shell", "getprop", "ro.product.model").stdout.strip()
    rows, sections, failed = [], [], 0
    for f in files:
        sid = f.name.split("-")[0]
        title = re.sub(r"^S\d+ · ", "", f.read_text(encoding="utf-8").splitlines()[0].lstrip("# ").strip())
        steps, status, err, shots = parse(f), "PASS", "", []
        for n, step in enumerate(steps, 1):
            try:
                run_step(step)
                ok = True
            except Exception as e:  # noqa: BLE001 — раннер должен зафиксировать любую причину падения
                ok, status, err = False, "FAIL", f"шаг {n} `{step}`: {e}"
            shot = f"{sid}-{n:03d}.png"
            sh(ADBX, "shot", str(out / shot))
            sh("convert", str(out / shot), "-resize", "360x", "-colors", "64", str(out / shot))  # компактно для git
            shots.append((n, step, ok, shot))
            if not ok:
                break
        crash = sh("adb", "logcat", "-d", "-b", "crash").stdout
        if "FATAL EXCEPTION" in crash and "dev.polurival.mexamen" in crash:
            status, err = "FAIL", err + " | краш приложения (logcat -b crash)"
            sh("adb", "logcat", "-c")
        failed += status == "FAIL"
        rows.append((sid, title, status, len(steps), err))
        body = [f"### {sid} · {title} — **{status}**", ""]
        if err:
            body += [f"> Причина: {err}", ""]
        body += ["| # | Шаг | Результат | Скриншот |", "|---|---|---|---|"]
        for n, step, ok, shot in shots:
            body.append(f"| {n} | `{step}` | {'✅' if ok else '❌'} | ![]({shot}) |")
        sections.append("\n".join(body))
    head = [f"# Smoke-отчёт Mexamen — {datetime.datetime.now():%Y-%m-%d %H:%M}", "",
            f"Устройство: {model} (физическое, USB). Прогнано сценариев: {len(rows)}, упало: {failed}.", "",
            "| Сценарий | Статус | Шагов | Причина |", "|---|---|---|---|"]
    head += [f"| {s} · {t} | {'✅ PASS' if st == 'PASS' else '❌ FAIL'} | {n} | {e} |" for s, t, st, n, e in rows]
    (out / "report.md").write_text("\n".join(head) + "\n\n" + "\n\n".join(sections) + "\n", encoding="utf-8")
    print(f"report: {out/'report.md'}  failed={failed}")
    sys.exit(1 if failed else 0)


main()
