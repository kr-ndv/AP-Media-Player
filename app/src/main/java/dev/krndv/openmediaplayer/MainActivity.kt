package dev.krndv.openmediaplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.krndv.openmediaplayer.core.design.theme.OpenMediaPlayerTheme
import dev.krndv.openmediaplayer.feature.designsystem.DesignSystemScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var dynamicColorEnabled by rememberSaveable { mutableStateOf(true) }

            OpenMediaPlayerTheme(dynamicColor = dynamicColorEnabled) {
                DesignSystemScreen(
                    dynamicColorEnabled = dynamicColorEnabled,
                    onDynamicColorChange = { dynamicColorEnabled = it },
                )
            }
        }
    }
}
