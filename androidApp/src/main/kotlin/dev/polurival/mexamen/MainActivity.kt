package dev.polurival.mexamen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.polurival.mexamen.android.BuildConfig
import dev.polurival.mexamen.config.AppConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize KMP AppConfig from Android BuildConfig
        AppConfig.groqApiKey = BuildConfig.GROQ_API_KEY
        AppConfig.isDebug = BuildConfig.DEBUG

        AppContext.instance = applicationContext
        setContent {
            App()
        }
    }
}
