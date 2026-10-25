import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import dev.polurival.mexamen.App
import dev.polurival.mexamen.data.ThemeMode
import dev.polurival.mexamen.data.defaultThemeModeStore
import dev.polurival.mexamen.ui.MexamenTheme
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        // Рамка вокруг колонки App — тот же режим, что внутри (тот же инстанс store на странице)
        val themeModeFlow = remember { defaultThemeModeStore().themeMode }
        val themeMode by themeModeFlow.collectAsState(ThemeMode.SYSTEM)
        MexamenTheme(themeMode = themeMode) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxSize()
                    ) {
                        App()
                    }
                }
            }
        }
    }
}
