#!/usr/bin/env bash
# Тонкая обёртка над adb для smoke-прогона на физическом устройстве (запасной путь, если MCP claude-in-mobile недоступен).
# Использование: adbx.sh dump | tap "<text|content-desc>" | shot <file.png> | has "<text>" | back | restart | clear
set -euo pipefail
ADB="adb ${ANDROID_SERIAL:+-s $ANDROID_SERIAL}"
PKG=dev.polurival.mexamen
XML=/tmp/mexamen_ui.xml

dump() { $ADB shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; $ADB pull /sdcard/ui.xml "$XML" >/dev/null 2>&1; }

# Центр узла по text/content-desc: сначала точное совпадение, затем подстрока.
center() {
  python3 -I - "$1" <<'PY'
import re, sys, html
needle = sys.argv[1]
xml = open('/tmp/mexamen_ui.xml', encoding='utf-8').read()
nodes = []
for m in re.finditer(r'<node [^>]*>', xml):
    n = m.group(0)
    t = html.unescape((re.search(r' text="([^"]*)"', n) or [None, ''])[1])
    d = html.unescape((re.search(r'content-desc="([^"]*)"', n) or [None, ''])[1])
    b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]', n)
    nodes.append((t, d, tuple(map(int, b.groups()))))
for match in (lambda t, d: needle in (t, d), lambda t, d: needle in t or needle in d):
    for t, d, (x1, y1, x2, y2) in nodes:
        if match(t, d):
            print((x1 + x2) // 2, (y1 + y2) // 2)
            sys.exit(0)
sys.exit(1)
PY
}

case "$1" in
  dump) dump; python3 -I -c "
import re,html
x=open('$XML',encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*>',x):
    n=m.group(0)
    t=html.unescape((re.search(r' text=\"([^\"]*)\"',n) or [0,''])[1]); d=html.unescape((re.search(r'content-desc=\"([^\"]*)\"',n) or [0,''])[1])
    if t or d: print(repr(t), repr(d), re.search(r'bounds=\"([^\"]*)\"',n)[1])
";;
  tap) dump; read -r X Y < <(center "$2") || { echo "NOT FOUND: $2" >&2; exit 1; }; $ADB shell input tap "$X" "$Y";;
  has) dump; center "$2" >/dev/null && echo "OK: '$2'" || { echo "MISSING: '$2'" >&2; exit 1; };;
  shot) $ADB exec-out screencap -p > "$2";;
  back) $ADB shell input keyevent KEYCODE_BACK;;
  restart) $ADB shell am force-stop $PKG; $ADB shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1;;
  clear) $ADB shell pm clear $PKG >/dev/null;;
  *) echo "unknown: $1" >&2; exit 2;;
esac
