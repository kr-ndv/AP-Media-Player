package dev.krndv.openmediaplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.krndv.openmediaplayer.core.design.theme.OpenMediaPlayerTheme
import dev.krndv.openmediaplayer.feature.library.MediaLibraryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OpenMediaPlayerTheme {
                MediaLibraryScreen()
            }
        }
    }
}
