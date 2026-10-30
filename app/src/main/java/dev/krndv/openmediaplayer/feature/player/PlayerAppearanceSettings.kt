package dev.krndv.openmediaplayer.feature.player

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class ProgressStyle { BAR, SLIM, WAVY, SQUIGGLY }
enum class PlayerBackgroundStyle { AURORA, GRADIENT, SOLID }

data class PlayerAppearance(
    val progressEnabled: Boolean = true,
    val progressStyle: ProgressStyle = ProgressStyle.WAVY,
    val backgroundEnabled: Boolean = true,
    val backgroundStyle: PlayerBackgroundStyle = PlayerBackgroundStyle.AURORA,
    val wavyPlayPauseEnabled: Boolean = true,
    val keepScreenOnWhilePlaying: Boolean = true,
) {
    fun save(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putBoolean("progress_enabled", progressEnabled)
            .putString("progress_style", progressStyle.name)
            .putBoolean("background_enabled", backgroundEnabled)
            .putString("background_style", backgroundStyle.name)
            .putBoolean("wavy_play_pause_enabled", wavyPlayPauseEnabled)
            .putBoolean("keep_screen_on_while_playing", keepScreenOnWhilePlaying)
            .apply()
    }

    companion object {
        private const val PREFERENCES = "ap_media_player_appearance"

        fun load(context: Context): PlayerAppearance {
            val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            return PlayerAppearance(
                progressEnabled = prefs.getBoolean("progress_enabled", true),
                progressStyle = runCatching {
                    ProgressStyle.valueOf(prefs.getString("progress_style", ProgressStyle.WAVY.name)!!)
                }.getOrDefault(ProgressStyle.WAVY),
                backgroundEnabled = prefs.getBoolean("background_enabled", true),
                backgroundStyle = runCatching {
                    PlayerBackgroundStyle.valueOf(
                        prefs.getString("background_style", PlayerBackgroundStyle.AURORA.name)!!,
                    )
                }.getOrDefault(PlayerBackgroundStyle.AURORA),
                wavyPlayPauseEnabled = prefs.getBoolean("wavy_play_pause_enabled", true),
                keepScreenOnWhilePlaying = prefs.getBoolean("keep_screen_on_while_playing", true),
            )
        }
    }
}

@Composable
fun PlayerAppearanceSettingsScreen(
    appearance: PlayerAppearance,
    onAppearanceChange: (PlayerAppearance) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Player appearance", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Personalize the progress indicator, player backdrop, and playback button. Each effect can be turned off independently.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SettingSwitchRow(
                    title = "Custom progress slider",
                    description = "Show a styled progress indicator while media is playing.",
                    checked = appearance.progressEnabled,
                    onCheckedChange = { onAppearanceChange(appearance.copy(progressEnabled = it)) },
                )
                if (appearance.progressEnabled) {
                    Text("Slider style", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProgressStyle.entries.forEach { style ->
                            FilterChip(
                                selected = appearance.progressStyle == style,
                                onClick = { onAppearanceChange(appearance.copy(progressStyle = style)) },
                                label = { Text(style.label()) },
                            )
                        }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SettingSwitchRow(
                    title = "Player background",
                    description = "Use a decorative backdrop behind the playback stage.",
                    checked = appearance.backgroundEnabled,
                    onCheckedChange = { onAppearanceChange(appearance.copy(backgroundEnabled = it)) },
                )
                if (appearance.backgroundEnabled) {
                    Text("Background style", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlayerBackgroundStyle.entries.forEach { style ->
                            FilterChip(
                                selected = appearance.backgroundStyle == style,
                                onClick = { onAppearanceChange(appearance.copy(backgroundStyle = style)) },
                                label = { Text(style.label()) },
                            )
                        }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SettingSwitchRow(
                    title = "Keep screen on while playing",
                    description = "Prevent the display from sleeping while the player screen is active and media is playing.",
                    checked = appearance.keepScreenOnWhilePlaying,
                    onCheckedChange = { onAppearanceChange(appearance.copy(keepScreenOnWhilePlaying = it)) },
                )
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SettingSwitchRow(
                    title = "Wavy play/pause button",
                    description = "Use an expressive, animated-feeling shape for the main playback action.",
                    checked = appearance.wavyPlayPauseEnabled,
                    onCheckedChange = {
                        onAppearanceChange(appearance.copy(wavyPlayPauseEnabled = it))
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun ProgressStyle.label(): String = when (this) {
    ProgressStyle.BAR -> "Bar"
    ProgressStyle.SLIM -> "Slim"
    ProgressStyle.WAVY -> "Wavy"
    ProgressStyle.SQUIGGLY -> "Squiggly"
}

private fun PlayerBackgroundStyle.label(): String = when (this) {
    PlayerBackgroundStyle.AURORA -> "Aurora"
    PlayerBackgroundStyle.GRADIENT -> "Gradient"
    PlayerBackgroundStyle.SOLID -> "Solid"
}
