package dev.polurival.mexamen.di

import dev.polurival.mexamen.domain.AskModelUseCase
import dev.polurival.mexamen.domain.ModelVariant
import dev.polurival.mexamen.llm.DistractorGenerator
import dev.polurival.mexamen.llm.FallbackDistractorGenerator
import dev.polurival.mexamen.llm.GroqProvider
import dev.polurival.mexamen.llm.LlmProvider
import dev.polurival.mexamen.llm.LocalLlmProvider
import org.koin.dsl.module

val llmModule = module {
    single<LlmProvider> { GroqProvider() }
    single {
        val client = LocalLlmProvider.defaultClient()
        AskModelUseCase(ModelVariant.entries.associateWith { LocalLlmProvider(model = it.apiName, client = client) })
    }
    single<DistractorGenerator> { FallbackDistractorGenerator() }
}
