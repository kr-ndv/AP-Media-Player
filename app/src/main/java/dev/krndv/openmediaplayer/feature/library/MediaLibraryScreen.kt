package dev.krndv.openmediaplayer.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.krndv.openmediaplayer.R
import dev.krndv.openmediaplayer.core.design.tokens.FamilySpacing
import dev.krndv.openmediaplayer.core.playback.LibVlcPlaybackEngine
import dev.krndv.openmediaplayer.feature.player.PlaybackScreen

private enum class PlayerDestination(val labelRes: Int, val symbol: String) {
    Library(R.string.nav_library, "▦"),
    Player(R.string.nav_player, "▶"),
    Network(R.string.nav_network, "⇄"),
    Playlists(R.string.nav_playlists, "≡"),
    Settings(R.string.nav_settings, "⚙"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaLibraryScreen(engine: LibVlcPlaybackEngine) {
    var selectedDestination by rememberSaveable { mutableIntStateOf(0) }
    val destination = PlayerDestination.entries[selectedDestination]
    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(destination.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                PlayerDestination.entries.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedDestination == index,
                        onClick = { selectedDestination = index },
                        icon = {
                            Text(
                                text = item.symbol,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        label = { Text(stringResource(item.labelRes), maxLines = 1) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = FamilySpacing.pageHorizontal,
                    vertical = FamilySpacing.large,
                ),
            verticalArrangement = Arrangement.spacedBy(FamilySpacing.large),
        ) {
            when (destination) {
                PlayerDestination.Library -> LibraryLanding()
                PlayerDestination.Player -> PlaybackScreen(engine)
                PlayerDestination.Network -> NetworkLanding()
                PlayerDestination.Playlists -> EmptyDestination(
                    title = stringResource(R.string.playlists_empty_title),
                    description = stringResource(R.string.playlists_empty_description),
                    symbol = "♫",
                )
                PlayerDestination.Settings -> EmptyDestination(
                    title = stringResource(R.string.settings_title),
                    description = stringResource(R.string.settings_description),
                    symbol = "⚙",
                )
            }
        }
    }
}

@Composable
private fun LibraryLanding() {
    val colors = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(FamilySpacing.small)) {
        Text(
            text = stringResource(R.string.library_headline),
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = stringResource(R.string.library_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FamilySpacing.large),
            horizontalArrangement = Arrangement.spacedBy(FamilySpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = colors.primary,
                contentColor = colors.onPrimary,
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Text("♫", style = MaterialTheme.typography.headlineMedium)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.library_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.library_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer,
                )
            }
        }
    }

    Text(
        text = stringResource(R.string.browse_media_title),
        style = MaterialTheme.typography.titleLarge,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(FamilySpacing.medium)) {
        MediaTypeCard(
            title = stringResource(R.string.audio_title),
            description = stringResource(R.string.audio_description),
            symbol = "♫",
            modifier = Modifier.weight(1f),
        )
        MediaTypeCard(
            title = stringResource(R.string.video_title),
            description = stringResource(R.string.video_description),
            symbol = "▶",
            modifier = Modifier.weight(1f),
        )
    }
    MediaTypeCard(
        title = stringResource(R.string.folders_title),
        description = stringResource(R.string.folders_description),
        symbol = "▰",
        modifier = Modifier.fillMaxWidth(),
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
    ) {
        Text(
            text = stringResource(R.string.foundation_status),
            modifier = Modifier.padding(FamilySpacing.medium),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun NetworkLanding() {
    Column(verticalArrangement = Arrangement.spacedBy(FamilySpacing.small)) {
        Text(
            text = stringResource(R.string.network_headline),
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = stringResource(R.string.network_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    listOf(
        R.string.network_url_title to R.string.network_url_description,
        R.string.network_share_title to R.string.network_share_description,
        R.string.network_discovery_title to R.string.network_discovery_description,
    ).forEach { (title, description) ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(FamilySpacing.large),
                verticalArrangement = Arrangement.spacedBy(FamilySpacing.small),
            ) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MediaTypeCard(
    title: String,
    description: String,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FamilySpacing.large),
            verticalArrangement = Arrangement.spacedBy(FamilySpacing.small),
        ) {
            Text(symbol, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyDestination(title: String, description: String, symbol: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FamilySpacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FamilySpacing.medium),
    ) {
        Text(symbol, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
