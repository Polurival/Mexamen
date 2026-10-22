package dev.polurival.mexamen.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.polurival.mexamen.domain.StatsSummary
import dev.polurival.mexamen.domain.TopicStatsSummary

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Estadísticas",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center),
            )
            IconButton(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(AppIcons.Refresh, contentDescription = "Reiniciar estadísticas")
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val current = state) {
                is StatsUiState.Loading -> CircularProgressIndicator()
                is StatsUiState.Loaded -> {
                    StatsSummaryContent(current.summary)
                    if (current.topics.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = "Por tema",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            current.topics.forEach { TopicStatCard(it) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al inicio")
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("¿Reiniciar estadísticas?") },
            text = { Text("Se borrará todo el progreso guardado (pesos y contadores). Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    viewModel.resetStats()
                }) { Text("Reiniciar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun StatsSummaryContent(summary: StatsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Preguntas totales", summary.totalQuestions.toString())
        StatCard("Preguntas intentadas", summary.attempted.toString())
        StatCard("Respuestas correctas", summary.totalCorrect.toString())
        StatCard("Respuestas incorrectas", summary.totalWrong.toString())
        StatCard("Preguntas sin intentar", summary.neverAttempted.toString())
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun TopicStatCard(topic: TopicStatsSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = topic.topic, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Preguntas intentadas: ${topic.attemptedQuestions} de ${topic.totalQuestions}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Respuestas: ${topic.answersGiven} · Correctas: ${topic.correctAnswers}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
