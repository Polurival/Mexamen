# LLM Gateway (День 13)

Прокси между клиентом (в т. ч. Claude Code) и Anthropic Messages API с Input/Output Guard, rate limiting, cost tracking и аудитом.
Только стандартная библиотека Python 3.10+ — устанавливать ничего не нужно. Не часть приложения Mexamen (как и остальной `tools/`).

Готовый отчёт с результатами тест-кейсов и логами: **[results/RESULTS.md](results/RESULTS.md)**.

## Запуск

```bash
cd tools/gateway
python3 -m llm_gateway --port 8080                 # upstream: https://api.anthropic.com
ANTHROPIC_BASE_URL=http://127.0.0.1:8080 claude     # клиент ходит через шлюз
```

Тесты и отчёт (без сети и расходов — upstream подменён фейком):

```bash
python3 -m unittest discover -s tests -t .          # 57 тестов
python3 report.py                                   # пересобирает results/RESULTS.md и results/logs/
```

## Настройки

Флаги CLI или переменные окружения `GATEWAY_*` (см. `llm_gateway/config.py`):

| Параметр | По умолчанию | Смысл |
|---|---|---|
| `--input-mode` / `GATEWAY_INPUT_MODE` | `redact` | `block` — 400 и ничего в LLM; `redact` — `[REDACTED_<KIND>]` и дальше |
| `--output-mode` / `GATEWAY_OUTPUT_MODE` | `block` | `block` — подмена ответа; `redact` — маскирует секреты/URL; `log` — только журнал |
| `--rate-limit` / `GATEWAY_RATE_LIMIT_PER_MIN` | `60` | запросов/мин с IP, `0` — выкл. Не считаются `GET`/`HEAD`/`OPTIONS` (служебные пробы клиента) |
| `GATEWAY_LOG_DIR` | `tools/gateway/logs` | куда писать `audit.jsonl` и `security_events.jsonl` |
| `GATEWAY_DISABLED_DETECTORS` | — | например `EMAIL,PHONE` (типы через запятую) |
| `GATEWAY_EMAIL_ALLOW` | — | email-шаблоны (fnmatch, через запятую), которые Input Guard не маскирует, например `me@mail.dev,*@corp.io`. Всегда разрешены `noreply@*` и `no-reply@*` |
| `GATEWAY_CANARY` | — | маркер: если он появился в ответе — это утечка system prompt |
| `GATEWAY_PRICING_FILE` | — | JSON со своими ценами за 1M токенов |
| `GATEWAY_TRUST_XFF` | — | `1` — брать IP из `X-Forwarded-For` (только за доверенным прокси) |

Клиент может ужесточить режим заголовком `x-guard-mode: block`, ослабить — нет.

Служебные эндпоинты: `GET /_gateway/health`, `GET /_gateway/stats` (нарастающая стоимость).

## Устройство

```
llm_gateway/
  detectors.py     regex-детекторы + валидаторы (Luhn, энтропия), склейка "a"+"b", base64 (2 уровня)
  input_guard.py   обход system/messages/tool_result/tool_use.input, block/redact, разрезанные секреты
  output_guard.py  секреты, утечка system prompt, подозрительные URL, опасные команды
  anthropic_wire.py разбор/сборка SSE и сообщений (ответ проверяется целиком до отдачи)
  rate_limit.py    скользящее окно на IP
  cost.py          usage → USD (цены и допущения — в докстринге)
  audit.py         JSONL-логи без сырых секретов и без заголовков авторизации
  gateway.py       ядро: rate limit → Input → upstream → Output → cost → аудит
  server.py        http.server + вызов upstream
tests/             cases.py — единый набор кейсов для тестов и отчёта
```

## Что важно знать

- Output Guard буферизует ответ целиком → стриминг доходит до клиента разом.
- В режиме `block` секрет в истории диалога блокирует все следующие запросы сессии (клиент шлёт историю каждый раз) — для чата используйте `redact`.
- Regex-детекция не ловит кодирования hex/ROT13, разнесённые пробелами символы, «разорванные» через текст части ключа — список в отчёте (§6).
- Для Claude Code свой email добавьте в `GATEWAY_EMAIL_ALLOW`: он приходит в служебном контексте (`userEmail`), иначе будет замаскирован.
- Проверено вручную на живом Claude Code с логином по подписке (2026-11-05): работает; детали и найденные проблемы — в отчёте, §7.
