package dev.polurival.mexamen.domain

/** Текущее время в мс от эпохи; платформенный источник, чтобы логика веса/cooldown была тестируема. */
expect fun currentTimeMillis(): Long
