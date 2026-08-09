package dev.polurival.mexamen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LegendResultScreen(
    correctCount: Int,
    totalCount: Int,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val passed = correctCount >= 5

    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
            text = "Resultado: $correctCount / $totalCount",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(48.dp))

        Button(onClick = onHome) {
            Text("Volver al inicio")
        }
    }
}
