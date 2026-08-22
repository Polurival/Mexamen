package dev.polurival.mexamen.domain

@JsFun("() => Date.now()")
external fun dateNow(): Double

actual fun currentTimeMillis(): Long = dateNow().toLong()
