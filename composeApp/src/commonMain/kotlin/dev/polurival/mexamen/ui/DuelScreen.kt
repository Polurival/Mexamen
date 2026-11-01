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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.polurival.mexamen.domain.DuelScore
import dev.polurival.mexamen.domain.DuelWinner
import dev.polurival.mexamen.domain.ModelVariant

@Composable
fun DuelQuestionScreen(
    state: DuelUiState.Question,
    onSelectVariant: (ModelVariant) -> Unit,
    onSelectAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Tú vs IA · Pregunta ${state.index + 1} de ${state.total}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onExit) {
                Icon(imageVector = AppIcons.Close, contentDescription = "Cerrar duelo")
            }
        }
        LinearProgressIndicator(
            progress = { (state.index + 1).toFloat() / state.total },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Modelo:", style = MaterialTheme.typography.labelLarge)
            VariantChip("Ajustado", ModelVariant.TUNED, state, onSelectVariant)
            VariantChip("Base", ModelVariant.BASE, state, onSelectVariant)
        }
        Text(
            text = scoreLine(state.score),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(24.dp))
            Text(text = state.question.entry.question, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.question.options.forEachIndexed { index, option ->
                    OptionCard(
                        text = option,
                        highlight = highlightFor(state.selectedIndex, state.question.correctIndex, index),
                        enabled = state.selectedIndex == null,
                        onClick = { onSelectAnswer(index) },
                    )
                }
            }
            if (state.selectedIndex != null) {
                Spacer(Modifier.height(24.dp))
                VerdictBlock(state)
                Spacer(Modifier.height(24.dp))
            }
        }

        if (state.canAdvance) {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Siguiente") }
        }
    }
}

@Composable
fun DuelFinishedScreen(
    state: DuelUiState.Finished,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Resultado del duelo", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(text = "Tú: ${state.score.user} de ${state.score.rounds}", style = MaterialTheme.typography.titleLarge)
        Text(text = "IA: ${state.score.model} de ${state.score.modelAnswered}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            text = when (state.score.winner) {
                DuelWinner.USER -> "¡Ganaste!"
                DuelWinner.MODEL -> "Ganó la IA"
                DuelWinner.DRAW -> "Empate"
                DuelWinner.NONE -> "La IA no respondió a todas las preguntas"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Jugar de nuevo") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Volver al inicio") }
    }
}

@Composable
private fun VariantChip(
    label: String,
    variant: ModelVariant,
    state: DuelUiState.Question,
    onSelect: (ModelVariant) -> Unit,
) {
    FilterChip(
        selected = state.variant == variant,
        onClick = { onSelect(variant) },
        enabled = state.canSwitchVariant,
        label = { Text(label) },
    )
}

@Composable
private fun VerdictBlock(state: DuelUiState.Question) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (state.userCorrect == true) "Tú: ¡correcto!" else "Tú: incorrecto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(text = modelLine(state.model), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun scoreLine(score: DuelScore): String = "Tú ${score.user} · IA ${score.model}"

private fun modelLine(status: ModelStatus): String = when (status) {
    ModelStatus.Thinking -> "IA: pensando…"
    ModelStatus.Unavailable -> "IA: no disponible (¿está encendido el servicio local?)"
    is ModelStatus.Answered -> {
        val verdict = if (status.correct) "correcto" else "incorrecto"
        val choice = status.choice ?: "sin respuesta clara"
        "IA: $choice — $verdict (${status.answer.latencyMillis} ms)"
    }
}
