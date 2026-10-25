package dev.polurival.mexamen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.polurival.mexamen.domain.AnsweredQuestion
import dev.polurival.mexamen.domain.AttemptCounts
import dev.polurival.mexamen.domain.AttemptCountsFormatter
import dev.polurival.mexamen.domain.DistractorSource
import dev.polurival.mexamen.domain.QuizResult

@Composable
fun ResultScreen(
    result: QuizResult,
    attempts: Map<Int, AttemptCounts>,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val passed = result.score >= 8

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (passed) "¡Felicidades!" else "Sigue practicando",
            style = MaterialTheme.typography.headlineMedium,
            color = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (passed) "Has aprobado el examen" else "No has aprobado el examen",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Resultado: ${result.score}/${result.total}",
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(24.dp))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(result.answers) { answered ->
                AnswerCard(answered, attempts[answered.question.entry.id])
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text("Intentar de nuevo")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al inicio")
        }
    }
}

@Composable
internal fun AnswerCard(answered: AnsweredQuestion, attempts: AttemptCounts? = null) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = answered.question.entry.question,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            val chosen = if (answered.isAnswered) answered.question.options[answered.chosenIndex] else null
            if (answered.isCorrect) {
                Text(
                    text = "✓ $chosen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = if (chosen != null) "✗ $chosen" else "✗ Sin respuesta",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "Correcto: ${answered.question.entry.answer}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (attempts != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = AttemptCountsFormatter.format(attempts),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
