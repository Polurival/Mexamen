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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.polurival.mexamen.domain.QuizResult

@Composable
fun LegendResultScreen(
    result: QuizResult,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val passed = result.score >= 5

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
            text = "Has aprobado el test de español" + if (passed) "" else " (no)",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Resultado: ${result.score} / ${result.total}",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(result.answers) { answered ->
                AnswerCard(answered)
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al inicio")
        }
    }
}
