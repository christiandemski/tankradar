package de.codekorb.tankradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TankTheme { App() } }
    }
}

@Composable
private fun App() {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    // Ohne Schlüssel geht es gleich in die Einstellungen
    var settings by rememberSaveable { mutableStateOf(store.apiKey.isBlank()) }
    if (settings) SettingsScreen(store, onClose = { settings = false })
    else PricesScreen(store, onSettings = { settings = true })
}

// Farben wie im Dashboard
private val Dark = darkColorScheme(
    primary = Color(0xFF7CC4FF), onPrimary = Color(0xFF06365E),
    primaryContainer = Color(0xFF12405F), onPrimaryContainer = Color(0xFFCFE8FF),
    secondaryContainer = Color(0xFF12405F), onSecondaryContainer = Color(0xFFCFE8FF),
    background = Color(0xFF0F1316), surface = Color(0xFF0F1316),
    surfaceContainer = Color(0xFF1A2027), surfaceContainerHigh = Color(0xFF242B34),
    onBackground = Color(0xFFE4E8EC), onSurface = Color(0xFFE4E8EC), onSurfaceVariant = Color(0xFF98A2AD),
    outline = Color(0xFF4A5560), outlineVariant = Color(0xFF2C343E), error = Color(0xFFFF8A80))
private val Light = lightColorScheme(
    primary = Color(0xFF0B65AD), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E8FB), onPrimaryContainer = Color(0xFF06365E),
    secondaryContainer = Color(0xFFD3E8FB), onSecondaryContainer = Color(0xFF06365E),
    background = Color(0xFFEEF2F6), surface = Color(0xFFEEF2F6),
    surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFEEF2F6),
    onBackground = Color(0xFF191C1F), onSurface = Color(0xFF191C1F), onSurfaceVariant = Color(0xFF5B6672),
    outlineVariant = Color(0xFFDDE3EA), error = Color(0xFFC62828))

@Composable
fun goodColor() = if (isSystemInDarkTheme()) Color(0xFF7DDC9A) else Color(0xFF1B8A47)

@Composable
fun TankTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
