package dev.krndv.openmediaplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.krndv.openmediaplayer.core.design.theme.OpenMediaPlayerTheme
import dev.krndv.openmediaplayer.core.playback.LibVlcPlaybackEngine
import dev.krndv.openmediaplayer.feature.library.MediaLibraryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val engine = remember(context) { LibVlcPlaybackEngine(context) }
            DisposableEffect(engine) {
                onDispose { engine.release() }
            }

            OpenMediaPlayerTheme {
                MediaLibraryScreen(engine)
            }
        }
    }
}
