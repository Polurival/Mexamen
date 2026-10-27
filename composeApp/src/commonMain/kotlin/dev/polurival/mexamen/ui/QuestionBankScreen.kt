package dev.polurival.mexamen.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.polurival.mexamen.data.QuestionEntry

/** «Banco de preguntas»: поиск и список «вопрос — ответ»; фильтрация и раскрытие карточек — в VM через лямбды. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionBankScreen(
    state: QuestionBankUiState,
    onQueryChange: (String) -> Unit,
    onToggleExpanded: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Banco de preguntas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = AppIcons.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        when (state) {
            is QuestionBankUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            is QuestionBankUiState.Loaded -> Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = { Text("Buscar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Preguntas: ${state.results.size} de ${state.totalCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                if (state.results.isEmpty()) {
                    Text(
                        text = "Sin resultados",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(state.results, key = { it.id }) { entry ->
                            QuestionBankItem(
                                entry = entry,
                                expanded = entry.id in state.expandedIds,
                                onClick = { onToggleExpanded(entry.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionBankItem(entry: QuestionEntry, expanded: Boolean, onClick: () -> Unit) {
    val explanation = entry.explanation?.takeIf { it.isNotBlank() }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = explanation != null, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = entry.question, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = entry.answer,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (expanded && explanation != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Explicación",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = explanation, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
