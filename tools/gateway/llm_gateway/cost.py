"""Cost tracking: токены из `usage` ответа Anthropic → стоимость запроса в USD.

Цены — $ за 1M токенов, снимок от 2026-10-06 (таблица моделей из claude-api skill).
Что НЕ подтверждено источником и посчитано по допущению (помечается `estimated`):
  * cache write = 1.25 × input, если цена записи в кэш не задана явно;
  * cache read = 0.1 × input, если цена чтения кэша не задана явно.
Haiku 5.5: цена указана для промптов до 100K токенов (выше — дороже, здесь не учитывается).
Свои цены: переменная GATEWAY_PRICING_FILE (JSON {"model-id": {"input":..,"output":..,"cache_read":..,"cache_write":..}}).
"""

from __future__ import annotations

import json
import threading
from dataclasses import dataclass
from typing import Optional

CACHE_WRITE_MULTIPLIER = 1.25
CACHE_READ_MULTIPLIER = 0.1


@dataclass(frozen=True)
class Price:
    input: float
    output: float
    cache_read: Optional[float] = None
    cache_write: Optional[float] = None


DEFAULT_PRICES: dict[str, Price] = {
    "claude-fable-5-1": Price(10.0, 50.0, cache_read=0.25),
    "claude-mythos-5-1": Price(10.0, 50.0),
    "claude-fable-5": Price(10.0, 50.0),
    "claude-opus-5-5": Price(4.0, 20.0, cache_read=0.20),
    "claude-opus-5": Price(5.0, 25.0),
    "claude-opus-4-8": Price(5.0, 25.0),
    "claude-opus-4-7": Price(5.0, 25.0),
    "claude-opus-4-6": Price(5.0, 25.0),
    "claude-sonnet-5-5": Price(2.0, 10.0, cache_read=0.20),
    "claude-sonnet-5": Price(2.0, 10.0),
    "claude-sonnet-4-6": Price(3.0, 15.0),
    "claude-haiku-5-5": Price(0.10, 0.50),
    "claude-haiku-4-5": Price(1.0, 5.0),
}


@dataclass(frozen=True)
class Usage:
    input_tokens: int = 0
    output_tokens: int = 0
    cache_creation_input_tokens: int = 0
    cache_read_input_tokens: int = 0

    @staticmethod
    def from_dict(d: Optional[dict]) -> "Usage":
        d = d or {}

        def num(key: str) -> int:
            v = d.get(key)
            return v if isinstance(v, int) else 0

        return Usage(
            num("input_tokens"),
            num("output_tokens"),
            num("cache_creation_input_tokens"),
            num("cache_read_input_tokens"),
        )

    @property
    def total(self) -> int:
        return self.input_tokens + self.output_tokens + self.cache_creation_input_tokens + self.cache_read_input_tokens

    def to_log(self) -> dict:
        return {
            "input_tokens": self.input_tokens,
            "output_tokens": self.output_tokens,
            "cache_creation_input_tokens": self.cache_creation_input_tokens,
            "cache_read_input_tokens": self.cache_read_input_tokens,
        }


@dataclass(frozen=True)
class Cost:
    usd: Optional[float]  # None — цена модели неизвестна (угадывать не будем)
    estimated: bool = False
    note: str = ""

    def to_log(self) -> dict:
        return {"usd": None if self.usd is None else round(self.usd, 8), "estimated": self.estimated, "note": self.note}


def load_prices(path: str = "") -> dict[str, Price]:
    prices = dict(DEFAULT_PRICES)
    if path:
        with open(path, encoding="utf-8") as fh:
            for model, p in json.load(fh).items():
                prices[model] = Price(p["input"], p["output"], p.get("cache_read"), p.get("cache_write"))
    return prices


def find_price(model: str, prices: dict[str, Price]) -> Optional[Price]:
    if model in prices:
        return prices[model]
    # Датированные варианты (claude-opus-5-5-2026xxxx): берём самый длинный префикс.
    for known in sorted(prices, key=len, reverse=True):
        if model.startswith(known):
            return prices[known]
    return None


def compute_cost(model: str, usage: Usage, prices: dict[str, Price]) -> Cost:
    price = find_price(model, prices)
    if price is None:
        return Cost(None, note=f"нет цены для модели {model!r}")
    estimated = False
    read_price = price.cache_read
    write_price = price.cache_write
    if usage.cache_read_input_tokens and read_price is None:
        read_price, estimated = price.input * CACHE_READ_MULTIPLIER, True
    if usage.cache_creation_input_tokens and write_price is None:
        write_price, estimated = price.input * CACHE_WRITE_MULTIPLIER, True
    usd = (
        usage.input_tokens * price.input
        + usage.output_tokens * price.output
        + usage.cache_read_input_tokens * (read_price or 0.0)
        + usage.cache_creation_input_tokens * (write_price or 0.0)
    ) / 1_000_000
    return Cost(usd, estimated, "кэш посчитан по допущению (×0.1 чтение / ×1.25 запись)" if estimated else "")


class CostTracker:
    """Нарастающий итог по процессу: запросы, токены, USD (в разрезе моделей)."""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self.requests = 0
        self.total_usd = 0.0
        self.total_tokens = 0
        self.by_model: dict[str, float] = {}

    def add(self, model: str, usage: Usage, cost: Cost) -> float:
        with self._lock:
            self.requests += 1
            self.total_tokens += usage.total
            if cost.usd is not None:
                self.total_usd += cost.usd
                self.by_model[model] = self.by_model.get(model, 0.0) + cost.usd
            return self.total_usd

    def snapshot(self) -> dict:
        with self._lock:
            return {
                "requests_with_usage": self.requests,
                "total_tokens": self.total_tokens,
                "total_usd": round(self.total_usd, 8),
                "by_model_usd": {k: round(v, 8) for k, v in self.by_model.items()},
            }
