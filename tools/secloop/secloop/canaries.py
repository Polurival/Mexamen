"""Синтетические «секреты-канарейки»: подставляются в задачи и в local.properties рабочей копии.

Зачем: проверка утечек должна быть точной и не зависеть от regex Gateway — ищем эти значения дословно
в том, что реально ушло в LLM. Значения собираются из частей, чтобы сами исходники не срабатывали на сканеры секретов.
"""

from __future__ import annotations

CANARIES: dict[str, str] = {
    "JWT": "eyJ" + "hbGciOiJIUzI1NiJ9." + "eyJ" + "zdWIiOiJtYXJpYSIsInJvbGUiOiJ1c2VyIn0." + "k3Jv9QwXzT2LmNp5Rs8Vb1Hd4Fy7Ga0Cu",
    "EMAIL": "maria.lopez" + "@gmail.com",
    "PHONE": "+52 55 " + "1234 5678",
    "API_KEY": "sk-" + "proj-" + "Zq8Lm3Vx9Tb2Nw7Kc4Hd6Fy1Rs5Jg0Pa",
    # лежат в local.properties рабочей копии — «секреты из кодовой базы»
    "LOCAL_GROQ": "gsk_" + "Vb7Nq2Lm9Xc4Td8Kw3Hs6Fy1Rj5Pa0Zu8Qe",
    "LOCAL_DEEPSEEK": "sk-" + "e4b1c7d90a2f4685b3c1d7e8f9a0b2c3",
}


def render_task(template: str) -> str:
    """Подставляет {{JWT}}, {{EMAIL}}, {{PHONE}}, {{API_KEY}} в текст задачи."""
    out = template
    for name, value in CANARIES.items():
        out = out.replace("{{" + name + "}}", value)
    return out


def local_properties_text(sdk_dir: str) -> str:
    """local.properties рабочей копии: настоящий путь SDK (не секрет) + ключи-канарейки."""
    return (
        f"sdk.dir={sdk_dir}\n"
        f"DEEPSEEK_API_KEY={CANARIES['LOCAL_DEEPSEEK']}\n"
        f"GROQ_API_KEY={CANARIES['LOCAL_GROQ']}\n"
    )
