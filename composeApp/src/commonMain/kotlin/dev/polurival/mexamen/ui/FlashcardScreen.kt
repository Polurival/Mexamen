package dev.polurival.mexamen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.polurival.mexamen.domain.FlashcardGrade

@Composable
fun FlashcardCardScreen(
    state: FlashcardUiState.Card,
    onFlip: () -> Unit,
    onGrade: (FlashcardGrade) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Tarjeta ${state.index + 1} de ${state.total}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onExit) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = "Cerrar tarjetas"
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (state.index + 1).toFloat() / state.total },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))
        Card(
            onClick = onFlip,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = state.entry.question,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                if (state.revealed) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = state.entry.answer,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    
                    if (!state.entry.explanation.isNullOrBlank() || state.relatedEntry != null) {
                        Spacer(Modifier.height(24.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (!state.entry.explanation.isNullOrBlank()) {
                                Text(
                                    text = "💡 ${state.entry.explanation}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start
                                )
                            }
                            if (state.relatedEntry != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "🔗 ¿Sabías que...?",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${state.relatedEntry.question} — ${state.relatedEntry.answer}",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Toca para ver la respuesta",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (state.revealed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GradeButton("Otra vez", MaterialTheme.colorScheme.error, Modifier.weight(1f)) { onGrade(FlashcardGrade.AGAIN) }
                GradeButton("Difícil", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f)) { onGrade(FlashcardGrade.HARD) }
                GradeButton("Bien", MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { onGrade(FlashcardGrade.GOOD) }
                GradeButton("Fácil", MaterialTheme.colorScheme.secondary, Modifier.weight(1f)) { onGrade(FlashcardGrade.EASY) }
            }
        }
    }
}

@Composable
private fun GradeButton(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun FlashcardDoneScreen(
    state: FlashcardUiState.Done,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "¡Sesión completada!",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(24.dp))
        FlashcardGrade.entries.forEach { grade ->
            Text(
                text = "${gradeLabel(grade)}: ${state.counts[grade] ?: 0}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onHome) {
            Text("Volver al inicio")
        }
    }
}

private fun gradeLabel(grade: FlashcardGrade): String = when (grade) {
    FlashcardGrade.AGAIN -> "Otra vez"
    FlashcardGrade.HARD -> "Difícil"
    FlashcardGrade.GOOD -> "Bien"
    FlashcardGrade.EASY -> "Fácil"
}
