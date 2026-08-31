package dev.polurival.mexamen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.polurival.mexamen.domain.DistractorSource

@Composable
fun QuizScreen(
    state: QuizUiState.Question,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Pregunta ${state.index + 1} de ${state.total}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatTime(state.timeLeftSeconds),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (state.timeLeftSeconds < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            //if (state.question.source == DistractorSource.FALLBACK) {
            //    OfflineBadge()
            //}
            IconButton(onClick = onExit) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = "Cerrar quiz"
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (state.index + 1).toFloat() / state.total },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = state.question.entry.question,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.question.options.forEachIndexed { index, option ->
                OptionCard(
                    text = option,
                    highlight = highlightFor(state, index),
                    enabled = state.selectedIndex == null,
                    onClick = { onSelect(index) },
                )
            }
        }

        if (state.selectedIndex != null) {
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(24.dp))
            ExplanationBlock(
                explanation = state.question.entry.explanation,
                relatedEntry = state.relatedEntry
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Siguiente")
            }
        }
    }
}

@Composable
private fun ExplanationBlock(
    explanation: String?,
    relatedEntry: dev.polurival.mexamen.data.QuestionEntry?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!explanation.isNullOrBlank()) {
            Text(
                text = "💡 Explicación:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
        }

        if (relatedEntry != null) {
            Text(
                text = "🔗 ¿Sabías que...?",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${relatedEntry.question} — ${relatedEntry.answer}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}"
}

private enum class OptionHighlight { NONE, CORRECT, WRONG }

private fun highlightFor(state: QuizUiState.Question, index: Int): OptionHighlight = when {
    state.selectedIndex == null -> OptionHighlight.NONE
    index == state.question.correctIndex -> OptionHighlight.CORRECT
    index == state.selectedIndex -> OptionHighlight.WRONG
    else -> OptionHighlight.NONE
}

@Composable
private fun OptionCard(
    text: String,
    highlight: OptionHighlight,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    // Карточки после выбора выключены (enabled=false) — задаём и disabled-цвета,
    // иначе подсветка верного/неверного не видна
    val (container, content) = when (highlight) {
        OptionHighlight.NONE -> MaterialTheme.colorScheme.surfaceVariant to
            MaterialTheme.colorScheme.onSurfaceVariant
        OptionHighlight.CORRECT -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        OptionHighlight.WRONG -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
    }
    val colors = CardDefaults.cardColors(
        containerColor = container,
        contentColor = content,
        disabledContainerColor = container,
        disabledContentColor = content,
    )
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}
