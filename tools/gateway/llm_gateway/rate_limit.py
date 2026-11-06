"""Rate limiting: не больше N запросов в минуту с одного IP (скользящее окно)."""

from __future__ import annotations

import threading
import time
from collections import deque
from typing import Callable

WINDOW_SECONDS = 60.0


class RateLimiter:
    def __init__(self, limit_per_minute: int, clock: Callable[[], float] = time.monotonic) -> None:
        self.limit = limit_per_minute
        self._clock = clock
        self._hits: dict[str, deque[float]] = {}
        self._lock = threading.Lock()

    def check(self, ip: str) -> tuple[bool, int]:
        """(разрешено, через сколько секунд повторить). limit <= 0 — лимит выключен."""
        if self.limit <= 0:
            return True, 0
        now = self._clock()
        with self._lock:
            hits = self._hits.setdefault(ip, deque())
            while hits and now - hits[0] >= WINDOW_SECONDS:
                hits.popleft()
            if len(hits) >= self.limit:
                retry = int(WINDOW_SECONDS - (now - hits[0])) + 1
                return False, max(retry, 1)
            hits.append(now)
            return True, 0
