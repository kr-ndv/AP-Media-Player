package dev.krndv.openmediaplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import dev.krndv.openmediaplayer.core.design.theme.OpenMediaPlayerTheme
import dev.krndv.openmediaplayer.core.playback.LibVlcPlaybackEngine
import dev.krndv.openmediaplayer.feature.library.MediaLibraryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val preferences = remember(context) { context.getSharedPreferences("av_music_player_settings", android.content.Context.MODE_PRIVATE) }
            var themeMode by rememberSaveable { mutableStateOf(preferences.getString("theme_mode", "system") ?: "system") }
            var dynamicColor by rememberSaveable { mutableStateOf(preferences.getBoolean("dynamic_color", false)) }
            val darkTheme = when (themeMode) { "light" -> false; "dark" -> true; else -> isSystemInDarkTheme() }
            val engine = remember(context) { LibVlcPlaybackEngine(context) }
            DisposableEffect(engine) {
                onDispose { engine.release() }
            }

            OpenMediaPlayerTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                MediaLibraryScreen(
                    engine = engine,
                    themeMode = themeMode,
                    onThemeModeChange = { value ->
                        themeMode = value
                        preferences.edit().putString("theme_mode", value).apply()
                    },
                    dynamicColor = dynamicColor,
                    onDynamicColorChange = { enabled ->
                        dynamicColor = enabled
                        preferences.edit().putBoolean("dynamic_color", enabled).apply()
                    },
                )
            }
        }
    }
}
