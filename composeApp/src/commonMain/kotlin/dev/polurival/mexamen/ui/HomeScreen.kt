package dev.polurival.mexamen.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import dev.polurival.mexamen.ui.navigation.SessionType

@Composable
fun HomeScreen(
    onStartQuiz: (SessionType) -> Unit,
    onStartFlashcards: (SessionType) -> Unit,
    onOpenLegends: () -> Unit,
    onOpenInterview: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenQuestionBank: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Mexamen",
            style = MaterialTheme.typography.displayMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Prepárate para el examen de naturalización",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        CategoryCard(title = "Historia y cultura") {
            Button(
                onClick = { onStartQuiz(SessionType.QUIZ) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Comenzar quiz")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onStartFlashcards(SessionType.FLASHCARDS) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Comenzar tarjetas didácticas")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenStats,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Estadísticas")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenQuestionBank,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Banco de preguntas")
            }
        }

        Spacer(Modifier.height(16.dp))

        CategoryCard(title = "Idioma") {
            Button(
                onClick = onOpenLegends,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Leyendas")
            }
        }

        Spacer(Modifier.height(16.dp))

        CategoryCard(title = "Entrevista") {
            Button(
                onClick = onOpenInterview,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Preguntas")
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun CategoryCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
