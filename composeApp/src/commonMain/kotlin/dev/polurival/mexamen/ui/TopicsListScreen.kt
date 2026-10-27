package dev.polurival.mexamen.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.polurival.mexamen.ui.navigation.SessionType

/** Список тем для выбора сессии: только отрисовка [state], выбор темы и «назад» — через лямбды. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsListScreen(
    state: TopicsUiState,
    sessionType: SessionType,
    onTopicSelected: (String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (sessionType) {
        SessionType.QUIZ -> "Temas del Quiz"
        SessionType.FLASHCARDS -> "Temas de Tarjetas"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                items(10) {
                    TopicItemShimmer()
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            } else {
                item {
                    TopicItem(
                        title = "Todos los temas",
                        count = state.totalCount,
                        onClick = { onTopicSelected(null) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                item {
                    TopicItem(
                        title = state.mistakes.name,
                        count = state.mistakes.questionCount,
                        enabled = state.hasMistakes,
                        disabledLabel = "Sin errores todavía",
                        onClick = { onTopicSelected(state.mistakes.name) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
                
                item {
                    Text(
                        text = "Por Tipo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                items(state.virtualTopics) { topic ->
                    TopicItem(
                        title = topic.name,
                        count = topic.questionCount,
                        onClick = { onTopicSelected(topic.name) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                item {
                    Text(
                        text = "Por Sección",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                items(state.topics) { topic ->
                    TopicItem(
                        title = topic.name,
                        count = topic.questionCount,
                        onClick = { onTopicSelected(topic.name) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun TopicItem(
    title: String,
    count: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
    disabledLabel: String? = null,
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        supportingContent = {
            Text(
                text = if (!enabled && disabledLabel != null) disabledLabel else "$count preguntas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp)
    )
}
