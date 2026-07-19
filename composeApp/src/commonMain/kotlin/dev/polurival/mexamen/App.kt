package dev.polurival.mexamen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.polurival.mexamen.data.defaultQuestionRepository
import dev.polurival.mexamen.llm.DeepSeekProvider
import dev.polurival.mexamen.llm.DistractorGenerator
import dev.polurival.mexamen.ui.HomeScreen
import dev.polurival.mexamen.ui.MexamenTheme
import dev.polurival.mexamen.ui.QuizScreen
import dev.polurival.mexamen.ui.QuizUiState
import dev.polurival.mexamen.ui.QuizViewModel
import dev.polurival.mexamen.ui.ResultScreen

@Composable
fun App() {
    MexamenTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // Паддинг внутри Surface: фон уходит под статус-бар (edge-to-edge), контент — нет
            Box(modifier = Modifier.safeDrawingPadding()) {
                AppContent()
            }
        }
    }
}

@Composable
private fun AppContent() {
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

@Composable
private fun LoadingScreen() {
    // NFR-1: плейсхолдер на время LLM-генерации (до 30 с, дальше fallback)
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Generando preguntas…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
