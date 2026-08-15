package dev.polurival.mexamen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LegendDetailScreen(
    state: LegendUiState,
    onSelectAnswer: (Int, Int) -> Unit,
    onPlayAudio: () -> Unit,
    onPauseAudio: () -> Unit,
    onStopAudio: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val legend = state.legend ?: return

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Lectura",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = formatTime(state.timeLeftSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.timeLeftSeconds < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = legend.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (state.audioState == AudioState.PLAYING) onPauseAudio() else onPlayAudio()
                        }
                    ) {
                        val icon = if (state.audioState == AudioState.PLAYING) {
                            AppIcons.Pause
                        } else {
                            Icons.Default.PlayArrow
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = if (state.audioState == AudioState.PLAYING) "Pause Audio" else "Play Audio",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (state.audioState != AudioState.STOPPED) {
                        IconButton(onClick = onStopAudio) {
                            Icon(
                                imageVector = AppIcons.Stop,
                                contentDescription = "Stop Audio",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Legend Text Styled like a Book
                Text(
                    text = legend.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 28.sp,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(48.dp))
                HorizontalDivider()
                Spacer(Modifier.height(32.dp))

                Text(
                    text = "Comprensión de lectura",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Responde las siguientes preguntas basadas en el texto anterior.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                legend.questions.forEachIndexed { qIndex, question ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${qIndex + 1}. ${question.question}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))

                            question.options.forEachIndexed { index, option ->
                                val selected = state.userAnswers[question.id] == index
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = selected,
                                            onClick = { onSelectAnswer(question.id, index) },
                                            role = Role.RadioButton
                                        )
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = null
                                    )
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(40.dp))

                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Finalizar y Calificar", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}"
}
