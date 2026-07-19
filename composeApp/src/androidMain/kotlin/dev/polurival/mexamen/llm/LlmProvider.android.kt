package dev.polurival.mexamen.llm

import dev.polurival.mexamen.BuildConfig

actual fun deepSeekApiKey(): String? = BuildConfig.DEEPSEEK_API_KEY.ifBlank { null }
