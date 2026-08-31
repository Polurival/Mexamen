package dev.polurival.mexamen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.polurival.mexamen.di.appModules
import dev.polurival.mexamen.ui.FlashcardCardScreen
import dev.polurival.mexamen.ui.FlashcardDoneScreen
import dev.polurival.mexamen.ui.FlashcardUiState
import dev.polurival.mexamen.ui.FlashcardViewModel
import dev.polurival.mexamen.ui.HomeScreen
import dev.polurival.mexamen.ui.InterviewScreen
import dev.polurival.mexamen.ui.InterviewViewModel
import dev.polurival.mexamen.ui.LegendDetailScreen
import dev.polurival.mexamen.ui.LegendResultScreen
import dev.polurival.mexamen.ui.LegendViewModel
import dev.polurival.mexamen.ui.LegendsListScreen
import dev.polurival.mexamen.ui.MexamenTheme
import dev.polurival.mexamen.ui.QuizScreen
import dev.polurival.mexamen.ui.QuizUiState
import dev.polurival.mexamen.ui.QuizViewModel
import dev.polurival.mexamen.ui.ResultScreen
import dev.polurival.mexamen.ui.StatsScreen
import dev.polurival.mexamen.ui.StatsViewModel
import dev.polurival.mexamen.ui.TopicsListScreen
import dev.polurival.mexamen.ui.TopicsViewModel
import dev.polurival.mexamen.ui.navigation.Route
import dev.polurival.mexamen.ui.navigation.SessionType
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

private val navigationSerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(Route.Home::class, Route.Home.serializer())
        subclass(Route.TopicsList::class, Route.TopicsList.serializer())
        subclass(Route.Quiz::class, Route.Quiz.serializer())
        subclass(Route.Flashcards::class, Route.Flashcards.serializer())
        subclass(Route.Stats::class, Route.Stats.serializer())
        subclass(Route.LegendsList::class, Route.LegendsList.serializer())
        subclass(Route.LegendDetail::class, Route.LegendDetail.serializer())
        subclass(Route.LegendResult::class, Route.LegendResult.serializer())
        subclass(Route.Interview::class, Route.Interview.serializer())
    }
}

private val navigationConfig = SavedStateConfiguration {
    serializersModule = navigationSerializersModule
}

@Composable
fun App() {
    // Паддинг внутри Surface: фон уходит под статус-бар (edge-to-edge), контент — нет
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(appModules) }),
        content = {
            MexamenTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Паддинг внутри Surface: фон уходит под статус-бар (edge-to-edge), контент — нет
                    Box(modifier = Modifier.safeDrawingPadding()) {
                        AppContent()
                    }
                }
            }
        })
}

@Composable
private fun AppContent() {
    val backStack = rememberNavBackStack(configuration = navigationConfig, Route.Home)
    val viewModelStoreDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()
    val saveableStateDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(saveableStateDecorator, viewModelStoreDecorator),
        onBack = { 
            if (backStack.size > 1) {
                backStack.removeAt(backStack.size - 1)
            }
        },
        entryProvider = { key ->
            when (key) {
                Route.Home -> NavEntry(key) {
                    HomeScreen(
                        onStartQuiz = { sessionType -> backStack.add(Route.TopicsList(sessionType)) },
                        onStartFlashcards = { sessionType -> backStack.add(Route.TopicsList(sessionType)) },
                        onOpenLegends = { backStack.add(Route.LegendsList) },
                        onOpenInterview = { backStack.add(Route.Interview) },
                        onOpenStats = { backStack.add(Route.Stats) },
                    )
                }
                is Route.TopicsList -> NavEntry(key) {
                    val viewModel: TopicsViewModel = koinViewModel()
                    TopicsListScreen(
                        viewModel = viewModel,
                        sessionType = key.sessionType,
                        onTopicSelected = { topic ->
                            val nextRoute = when (key.sessionType) {
                                SessionType.QUIZ -> Route.Quiz(topic)
                                SessionType.FLASHCARDS -> Route.Flashcards(topic)
                            }
                            backStack.add(nextRoute)
                        },
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        }
                    )
                }
                is Route.Quiz -> NavEntry(key) {
                    QuizFlow(topic = key.topic, onExit = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    })
                }
                is Route.Flashcards -> NavEntry(key) {
                    FlashcardFlow(topic = key.topic, onExit = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    })
                }
                Route.Stats -> NavEntry(key) {
                    val viewModel: StatsViewModel = koinViewModel()
                    StatsScreen(viewModel = viewModel, onBack = { 
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    })
                }
                Route.LegendsList -> NavEntry(key) {
                    LegendsListScreen(
                        onSelectLegend = { legendId -> backStack.add(Route.LegendDetail(legendId)) },
                        onBack = { 
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        }
                    )
                }
                is Route.LegendDetail -> NavEntry(key) {
                    LegendFlow(legendId = key.legendId, backStack = backStack)
                }
                is Route.LegendResult -> NavEntry(key) {
                    LegendResultScreen(
                        result = key.result,
                        onHome = { 
                            while (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        }
                    )
                }
                Route.Interview -> NavEntry(key) {
                    val viewModel: InterviewViewModel = koinViewModel()
                    val state by viewModel.state.collectAsState()
                    InterviewScreen(
                        state = state,
                        onUpdateAnswer = viewModel::updateAnswer,
                        onSaveAnswer = viewModel::saveAnswer,
                        onBack = { 
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        }
                    )
                }
                else -> error("Unknown key: $key")
            }
        }
    )
}

@Composable
private fun QuizFlow(topic: String?, onExit: () -> Unit) {
    val viewModel: QuizViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    LaunchedEffect(topic) { viewModel.startQuiz(topic) }
    when (val current = state) {
        is QuizUiState.Home -> QuizErrorScreen(
            error = current.error,
            onRetry = { viewModel.startQuiz(topic) },
            onHome = onExit,
        )
        is QuizUiState.Loading -> LoadingScreen()
        is QuizUiState.Question -> QuizScreen(
            state = current,
            onSelect = viewModel::selectAnswer,
            onNext = viewModel::advanceQuestion,
            onExit = onExit,
        )
        is QuizUiState.Result -> ResultScreen(
            result = current.result,
            onRestart = { viewModel.startQuiz(topic) },
            onHome = onExit,
        )
    }
}

@Composable
private fun FlashcardFlow(topic: String?, onExit: () -> Unit) {
    val viewModel: FlashcardViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    LaunchedEffect(topic) { viewModel.start(topic) }
    when (val current = state) {
        is FlashcardUiState.Loading -> LoadingScreen()
        is FlashcardUiState.Error -> QuizErrorScreen(
            error = current.message,
            onRetry = { viewModel.start(topic) },
            onHome = onExit,
        )
        is FlashcardUiState.Card -> FlashcardCardScreen(
            state = current,
            onFlip = viewModel::flip,
            onGrade = viewModel::grade,
            onExit = onExit,
        )
        is FlashcardUiState.Done -> FlashcardDoneScreen(
            state = current,
            onHome = onExit,
        )
    }
}

@Composable
private fun LegendFlow(legendId: String, backStack: MutableList<NavKey>) {
    val viewModel: LegendViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(legendId) {
        viewModel.selectLegend(legendId)
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            val result = viewModel.getResult()
            backStack.add(Route.LegendResult(result))
        }
    }

    LegendDetailScreen(
        state = state,
        onSelectAnswer = viewModel::selectAnswer,
        onPlayAudio = viewModel::playAudio,
        onPauseAudio = viewModel::pauseAudio,
        onStopAudio = viewModel::stopAudio,
        onSubmit = viewModel::finishLegend,
        onBack = { 
            if (backStack.size > 1) {
                backStack.removeAt(backStack.size - 1)
            }
        }
    )
}

@Composable
private fun QuizErrorScreen(error: String?, onRetry: () -> Unit, onHome: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(24.dp))
        }
        Button(onClick = onRetry) { Text("Reintentar") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onHome) { Text("Volver al inicio") }
    }
}

@Composable
private fun LoadingScreen() {
    // NFR-1: плейсхолдер на время LLM-генерации (до 30 с, дальше fallback)
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Generando preguntas…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
