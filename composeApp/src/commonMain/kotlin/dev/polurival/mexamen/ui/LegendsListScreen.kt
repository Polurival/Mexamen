package dev.polurival.mexamen.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.polurival.mexamen.data.allLegends

@Composable
fun LegendsListScreen(
    onSelectLegend: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(AppIcons.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Leyendas Mexicanas",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(allLegends) { legend ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectLegend(legend.id) }
                        .padding(16.dp)
                ) {
                    Text(
                        text = legend.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
