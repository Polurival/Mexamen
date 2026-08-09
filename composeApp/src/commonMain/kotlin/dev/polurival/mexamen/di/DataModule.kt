package dev.polurival.mexamen.di

import dev.polurival.mexamen.data.InterviewStore
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.data.defaultInterviewStore
import dev.polurival.mexamen.data.defaultQuestionRepository
import dev.polurival.mexamen.data.defaultQuestionStatsStore
import org.koin.dsl.module

val dataModule = module {
    single<QuestionRepository> { defaultQuestionRepository() }
    single<QuestionStatsStore> { defaultQuestionStatsStore() }
    single<InterviewStore> { defaultInterviewStore() }
}
