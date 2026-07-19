package dev.polurival.mexamen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.polurival.mexamen.data.defaultQuestionRepository
import dev.polurival.mexamen.llm.DeepSeekProvider
import dev.polurival.mexamen.llm.DistractorGenerator
import dev.polurival.mexamen.ui.HomeScreen
import dev.polurival.mexamen.ui.QuizScreen
import dev.polurival.mexamen.ui.QuizUiState
import dev.polurival.mexamen.ui.QuizViewModel
import dev.polurival.mexamen.ui.ResultScreen

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            val viewModel: QuizViewModel = viewModel {
                QuizViewModel(
                    repository = defaultQuestionRepository(),
                    distractorGenerator = DistractorGenerator(DeepSeekProvider()),
                )
            }
            val state by viewModel.state.collectAsState()
            when (val current = state) {
                is QuizUiState.Home -> HomeScreen(
                    error = current.error,
                    onStart = viewModel::startQuiz,
                )
                is QuizUiState.Loading -> LoadingScreen()
                is QuizUiState.Question -> QuizScreen(
                    state = current,
                    onSelect = viewModel::selectAnswer,
                )
                is QuizUiState.Result -> ResultScreen(
                    result = current.result,
                    onRestart = viewModel::startQuiz,
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
