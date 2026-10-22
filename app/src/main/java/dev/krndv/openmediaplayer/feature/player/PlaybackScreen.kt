package dev.krndv.openmediaplayer.feature.player

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.FrameLayout
import dev.krndv.openmediaplayer.R
import dev.krndv.openmediaplayer.core.design.tokens.FamilySpacing
import dev.krndv.openmediaplayer.core.playback.LibVlcPlaybackEngine
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun PlaybackScreen(engine: LibVlcPlaybackEngine) {
    val context = LocalContext.current
    var streamUrl by rememberSaveable { mutableStateOf("") }
    var playing by remember { mutableStateOf(engine.isPlaying) }
    var status by rememberSaveable { mutableStateOf("") }
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
            runCatching { engine.open(uri) }
                .onSuccess {
                    playing = true
                    status = context.getString(R.string.player_open_requested)
                }
                .onFailure {
                    playing = false
                    status = context.getString(R.string.player_open_failed, it.localizedMessage ?: it.javaClass.simpleName)
                }
        }
    }

    DisposableEffect(videoLayout) {
        val layout = videoLayout
        if (layout != null) engine.attachVideoLayout(layout)
        onDispose {
            if (layout != null) engine.detachVideoLayout(layout)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FamilySpacing.medium),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(FamilySpacing.small)) {
            Text(
                text = stringResource(R.string.player_headline),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.player_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            factory = { viewContext ->
                VLCVideoLayout(viewContext).also {
                    it.layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                    videoLayout = it
                }
            },
            update = { layout -> if (videoLayout !== layout) videoLayout = layout },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(FamilySpacing.small)) {
            Button(
                onClick = {
                    if (playing) {
                        engine.pause()
                        playing = false
                    } else {
                        runCatching { engine.play() }
                            .onSuccess { playing = true }
                            .onFailure {
                                status = context.getString(
                                    R.string.player_open_failed,
                                    it.localizedMessage ?: it.javaClass.simpleName,
                                )
                            }
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (playing) stringResource(R.string.player_pause) else stringResource(R.string.player_play))
            }
            OutlinedButton(
                onClick = {
                    engine.stop()
                    playing = false
                    status = context.getString(R.string.player_stopped)
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.player_stop))
            }
        }

        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(FamilySpacing.medium),
                verticalArrangement = Arrangement.spacedBy(FamilySpacing.medium),
            ) {
                Text(
                    text = stringResource(R.string.player_local_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.player_local_description),
                    style = MaterialTheme.typography.bodySmall,
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

        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(FamilySpacing.medium),
                verticalArrangement = Arrangement.spacedBy(FamilySpacing.medium),
            ) {
                Text(
                    text = stringResource(R.string.player_network_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = { streamUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.player_url_label)) },
                    placeholder = { Text("https://example.com/media.mp4") },
                    singleLine = true,
                )
                Button(
                    onClick = {
                        val value = streamUrl.trim()
                        val uri = runCatching { Uri.parse(value) }.getOrNull()
                        if (uri == null || uri.scheme !in setOf("http", "https", "rtsp", "rtmp", "udp", "rtp")) {
                            status = context.getString(R.string.player_invalid_url)
                        } else {
                            runCatching { engine.open(uri) }
                                .onSuccess {
                                    playing = true
                                    status = context.getString(R.string.player_open_requested)
                                }
                                .onFailure {
                                    playing = false
                                    status = context.getString(
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

        if (status.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(FamilySpacing.medium),
                    style = MaterialTheme.typography.bodySmall,
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
