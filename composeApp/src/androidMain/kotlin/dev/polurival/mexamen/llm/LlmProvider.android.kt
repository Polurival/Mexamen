package dev.polurival.mexamen.llm

import dev.polurival.mexamen.BuildConfig

actual fun deepSeekApiKey(): String? = BuildConfig.DEEPSEEK_API_KEY.ifBlank { null }

actual fun groqApiKey(): String? = BuildConfig.GROQ_API_KEY.ifBlank { null }

actual fun isDebugBuild(): Boolean = BuildConfig.DEBUG
