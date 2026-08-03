package dev.polurival.mexamen.di

import dev.polurival.mexamen.llm.DistractorGenerator
import dev.polurival.mexamen.llm.GroqProvider
import dev.polurival.mexamen.llm.LlmProvider
import org.koin.dsl.module

val llmModule = module {
    single<LlmProvider> { GroqProvider() }
    single { DistractorGenerator(get()) }
}
