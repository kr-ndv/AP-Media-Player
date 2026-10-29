package dev.krndv.openmediaplayer.feature.player

import android.content.Intent
import android.net.Uri
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.krndv.openmediaplayer.R
import dev.krndv.openmediaplayer.core.design.tokens.FamilySpacing
import dev.krndv.openmediaplayer.core.playback.LibVlcPlaybackEngine
import dev.krndv.openmediaplayer.core.playback.PlaybackSnapshot
import dev.krndv.openmediaplayer.core.playback.PlaybackStateListener
import dev.krndv.openmediaplayer.core.playback.PlaybackStatus
import java.util.Locale
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun PlaybackScreen(
    engine: LibVlcPlaybackEngine,
    requestedUri: Uri? = null,
    requestedTitle: String? = null,
    onRequestConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    var streamUrl by rememberSaveable { mutableStateOf("") }
    var playbackSnapshot by remember(engine) { mutableStateOf(engine.playbackState) }
    var feedback by rememberSaveable { mutableStateOf("") }
    var currentMediaTitle by rememberSaveable { mutableStateOf("") }
    var videoLayout by remember { mutableStateOf<VLCVideoLayout?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                // Some document providers do not grant persistable permissions.
            }

            currentMediaTitle = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected media"
            feedback = ""
            runCatching { engine.open(uri) }
                .onFailure {
                    feedback = context.getString(
                        R.string.player_open_failed,
                        it.localizedMessage ?: it.javaClass.simpleName,
                    )
                }
        }
    }

    DisposableEffect(engine) {
        val listener = PlaybackStateListener { snapshot ->
            playbackSnapshot = snapshot
        }
        engine.addPlaybackStateListener(listener)
        onDispose { engine.removePlaybackStateListener(listener) }
    }

    // Attach the LibVLC output surface before autoplaying a library selection.
    DisposableEffect(videoLayout) {
        val layout = videoLayout
        if (layout != null) engine.attachVideoLayout(layout)
        onDispose {
            if (layout != null) engine.detachVideoLayout(layout)
        }
    }

    LaunchedEffect(requestedUri, videoLayout) {
        val uri = requestedUri ?: return@LaunchedEffect
        if (videoLayout == null) return@LaunchedEffect

        feedback = ""
        currentMediaTitle = requestedTitle ?: uri.lastPathSegment ?: "Selected media"
        runCatching { engine.open(uri, autoplay = true) }
            .onFailure {
                feedback = context.getString(
                    R.string.player_open_failed,
                    it.localizedMessage ?: it.javaClass.simpleName,
                )
            }
        onRequestConsumed()
    }

    val playbackStateMessage = when (playbackSnapshot.status) {
        PlaybackStatus.IDLE -> ""
        PlaybackStatus.OPENING -> stringResource(R.string.player_state_opening)
        PlaybackStatus.PLAYING -> stringResource(R.string.player_state_playing)
        PlaybackStatus.PAUSED -> stringResource(R.string.player_state_paused)
        PlaybackStatus.STOPPED -> stringResource(R.string.player_state_stopped)
        PlaybackStatus.ENDED -> stringResource(R.string.player_state_ended)
        PlaybackStatus.ERROR -> playbackSnapshot.errorMessage
            ?.takeIf { it.isNotBlank() }
            ?.let { context.getString(R.string.player_open_failed, it) }
            ?: stringResource(R.string.player_state_error)
    }
    val statusText = feedback.ifBlank { playbackStateMessage }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FamilySpacing.large),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(FamilySpacing.small)) {
            Text(
                text = "NOW PLAYING",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = currentMediaTitle.ifBlank {
                    stringResource(R.string.player_headline)
                },
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = if (currentMediaTitle.isBlank()) {
                    stringResource(R.string.player_intro)
                } else {
                    "Ready when you are"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .heightIn(min = 190.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(Color.Black),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    VLCVideoLayout(viewContext).also {
                        it.layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        )
                        it.setBackgroundColor(android.graphics.Color.BLACK)
                        videoLayout = it
                    }
                },
                update = { layout ->
                    if (videoLayout !== layout) videoLayout = layout
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FamilySpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    feedback = ""
                    runCatching {
                        if (playbackSnapshot.status == PlaybackStatus.PLAYING) {
                            engine.pause()
                        } else {
                            engine.play()
                        }
                    }.onFailure {
                        feedback = context.getString(
                            R.string.player_open_failed,
                            it.localizedMessage ?: it.javaClass.simpleName,
                        )
                    }
                },
                enabled = playbackSnapshot.status != PlaybackStatus.IDLE &&
                    playbackSnapshot.status != PlaybackStatus.OPENING,
                modifier = Modifier.weight(1.35f).heightIn(min = 56.dp),
            ) {
                Text(
                    if (playbackSnapshot.status == PlaybackStatus.PLAYING) {
                        stringResource(R.string.player_pause)
                    } else {
                        stringResource(R.string.player_play)
                    },
                )
            }
            OutlinedButton(
                onClick = {
                    feedback = ""
                    runCatching { engine.stop() }
                        .onFailure {
                            feedback = context.getString(
                                R.string.player_open_failed,
                                it.localizedMessage ?: it.javaClass.simpleName,
                            )
                        }
                },
                enabled = playbackSnapshot.status == PlaybackStatus.OPENING ||
                    playbackSnapshot.status == PlaybackStatus.PLAYING ||
                    playbackSnapshot.status == PlaybackStatus.PAUSED,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.player_stop))
            }
        }

        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier.padding(FamilySpacing.large),
                verticalArrangement = Arrangement.spacedBy(FamilySpacing.medium),
            ) {
                Text(
                    text = stringResource(R.string.player_local_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.player_local_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { filePicker.launch(arrayOf("audio/*", "video/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.player_choose_file))
                }
            }
        }

        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier.padding(FamilySpacing.large),
                verticalArrangement = Arrangement.spacedBy(FamilySpacing.medium),
            ) {
                Text(
                    text = stringResource(R.string.player_network_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = { streamUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.player_url_label)) },
                    placeholder = { Text("https://example.com/media.mp4") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                )
                Button(
                    onClick = {
                        val value = streamUrl.trim()
                        val uri = runCatching { Uri.parse(value) }.getOrNull()
                        val scheme = uri?.scheme?.lowercase(Locale.ROOT)
                        if (
                            value.isBlank() || uri == null || scheme == null ||
                            scheme !in setOf("http", "https", "rtsp", "rtmp", "udp", "rtp")
                        ) {
                            feedback = context.getString(R.string.player_invalid_url)
                        } else {
                            currentMediaTitle = value
                            feedback = ""
                            runCatching { engine.open(uri) }
                                .onFailure {
                                    feedback = context.getString(
                                        R.string.player_open_failed,
                                        it.localizedMessage ?: it.javaClass.simpleName,
                                    )
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.player_open_stream))
                }
            }
        }

        if (statusText.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(FamilySpacing.medium),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = stringResource(R.string.player_prototype_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
