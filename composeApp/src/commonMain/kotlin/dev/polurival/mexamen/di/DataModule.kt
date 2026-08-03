package dev.polurival.mexamen.di

import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.defaultQuestionRepository
import org.koin.dsl.module

val dataModule = module {
    single<QuestionRepository> { defaultQuestionRepository() }
}
