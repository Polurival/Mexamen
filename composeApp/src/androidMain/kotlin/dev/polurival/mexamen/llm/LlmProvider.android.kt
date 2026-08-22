package dev.polurival.mexamen.llm

import dev.polurival.mexamen.config.AppConfig

actual fun groqApiKey(): String? = AppConfig.groqApiKey?.ifBlank { null }

actual fun isDebugBuild(): Boolean = AppConfig.isDebug
