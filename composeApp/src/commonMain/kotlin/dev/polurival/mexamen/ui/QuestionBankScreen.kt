package dev.polurival.mexamen.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

/**
 * «Banco de preguntas»: весь гид списком «вопрос — ответ» с поиском; тап по карточке
 * раскрывает explicación. Stateless: фильтрация и раскрытие — во ViewModel, сюда приходит [state].
 */
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (state) {
                is QuestionBankUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is QuestionBankUiState.Loaded -> QuestionBankContent(state, onQueryChange, onToggleExpanded)
            }
        }
    }
}

@Composable
private fun QuestionBankContent(
    state: QuestionBankUiState.Loaded,
    onQueryChange: (String) -> Unit,
    onToggleExpanded: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = { Text("Buscar por pregunta o respuesta") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
        Text(
            text = "${state.results.size} de ${state.totalCount} preguntas",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        if (state.results.isEmpty()) {
            Text(
                text = "Sin resultados",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.results, key = { it.id }) { entry ->
                    QuestionBankCard(
                        entry = entry,
                        expanded = entry.id in state.expandedIds,
                        onClick = { onToggleExpanded(entry.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionBankCard(
    entry: QuestionEntry,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = entry.question, style = MaterialTheme.typography.titleSmall)
            Text(
                text = entry.answer,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (expanded) {
                Text(
                    text = entry.explanation?.takeIf { it.isNotBlank() } ?: "Sin explicación",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
