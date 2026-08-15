package dev.polurival.mexamen.di

import dev.polurival.mexamen.ui.FlashcardViewModel
import dev.polurival.mexamen.ui.InterviewViewModel
import dev.polurival.mexamen.ui.LegendViewModel
import dev.polurival.mexamen.ui.QuizViewModel
import dev.polurival.mexamen.ui.StatsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { QuizViewModel(repository = get(), statsStore = get(), distractorGenerator = get()) }
    viewModel { FlashcardViewModel(repository = get(), statsStore = get()) }
    viewModel { StatsViewModel(repository = get(), statsStore = get()) }
    viewModel { LegendViewModel(audioPlayer = get()) }
    viewModel { InterviewViewModel(interviewStore = get()) }
}
