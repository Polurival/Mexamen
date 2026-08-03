package dev.polurival.mexamen.di

import dev.polurival.mexamen.ui.QuizViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { QuizViewModel(repository = get(), distractorGenerator = get()) }
}
