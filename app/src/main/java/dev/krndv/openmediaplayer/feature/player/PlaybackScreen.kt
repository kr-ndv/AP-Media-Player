package dev.krndv.openmediaplayer.feature.player

import android.content.Intent
import android.net.Uri
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
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
    appearance: PlayerAppearance = PlayerAppearance(),
    onRequestConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    var streamUrl by rememberSaveable { mutableStateOf("") }
    var playbackSnapshot by remember(engine) { mutableStateOf(engine.playbackState) }
    var feedback by rememberSaveable { mutableStateOf("") }
    var currentMediaTitle by rememberSaveable { mutableStateOf("") }
    var currentMediaIsVideo by rememberSaveable { mutableStateOf(false) }
    var videoLayout by remember { mutableStateOf<VLCVideoLayout?>(null) }
    var currentPositionMs by remember(engine) { mutableStateOf(engine.currentTimeMs) }
    var durationMs by remember(engine) { mutableStateOf(engine.durationMs) }

    LaunchedEffect(engine, playbackSnapshot.status) {
        while (true) {
            currentPositionMs = engine.currentTimeMs
            durationMs = engine.durationMs
            delay(if (playbackSnapshot.status == PlaybackStatus.PLAYING) 350L else 900L)
        }
    }

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
            currentMediaIsVideo = isVideoMedia(context, uri)
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
        currentMediaIsVideo = isVideoMedia(context, uri)
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
                .aspectRatio(if (currentMediaIsVideo) 16f / 9f else 1f)
                .heightIn(min = if (currentMediaIsVideo) 190.dp else 280.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    when {
                        !appearance.backgroundEnabled -> Brush.linearGradient(listOf(Color.Black, Color.Black))
                        appearance.backgroundStyle == PlayerBackgroundStyle.SOLID ->
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                        appearance.backgroundStyle == PlayerBackgroundStyle.GRADIENT ->
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer))
                        else -> Brush.radialGradient(
                            listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary, Color.Black),
                        )
                    },
                )
                .padding(if (appearance.backgroundEnabled) 10.dp else 0.dp),
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
            if (!currentMediaIsVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f),
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(104.dp),
                            shape = RoundedCornerShape(30.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                )
                            }
                        }
                        Text(
                            currentMediaTitle.ifBlank { "Your music, your way" },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                        )
                    }
                }
            }
        }

        if (appearance.progressEnabled && durationMs > 0L) {
            val progressTrackColor = MaterialTheme.colorScheme.surfaceVariant
            val progressActiveColor = MaterialTheme.colorScheme.primary
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .pointerInput(durationMs) {
                            detectTapGestures { point ->
                                if (size.width > 0) {
                                    engine.seekTo((point.x / size.width * durationMs).toLong())
                                }
                            }
                        }
                        .pointerInput(durationMs) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                if (size.width > 0) {
                                    engine.seekTo((change.position.x / size.width * durationMs).toLong())
                                }
                            }
                        },
                ) {
                    val width = size.width
                    val centerY = size.height / 2f
                    val fraction = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    val activeWidth = width * fraction
                    val track = progressTrackColor
                    val active = progressActiveColor
                    val stroke = when (appearance.progressStyle) {
                        ProgressStyle.BAR -> 8.dp.toPx()
                        ProgressStyle.SLIM -> 2.dp.toPx()
                        ProgressStyle.WAVY -> 4.dp.toPx()
                        ProgressStyle.SQUIGGLY -> 3.dp.toPx()
                    }
                    if (appearance.progressStyle == ProgressStyle.BAR || appearance.progressStyle == ProgressStyle.SLIM) {
                        drawLine(track, androidx.compose.ui.geometry.Offset(0f, centerY), androidx.compose.ui.geometry.Offset(width, centerY), strokeWidth = stroke)
                        drawLine(active, androidx.compose.ui.geometry.Offset(0f, centerY), androidx.compose.ui.geometry.Offset(activeWidth, centerY), strokeWidth = stroke)
                    } else {
                        val wave = Path()
                        val activeWave = Path()
                        val cycles = if (appearance.progressStyle == ProgressStyle.WAVY) 3.5f else 10f
                        val amplitude = if (appearance.progressStyle == ProgressStyle.WAVY) 4.dp.toPx() else 2.dp.toPx()
                        for (x in 0..width.toInt().coerceAtLeast(1) step 2) {
                            val y = centerY + kotlin.math.sin(x / width.coerceAtLeast(1f) * cycles * 2f * Math.PI).toFloat() * amplitude
                            if (x == 0) wave.moveTo(x.toFloat(), y) else wave.lineTo(x.toFloat(), y)
                            if (x <= activeWidth.toInt()) {
                                if (x == 0) activeWave.moveTo(x.toFloat(), y) else activeWave.lineTo(x.toFloat(), y)
                            }
                        }
                        drawPath(wave, track, style = Stroke(width = stroke))
                        drawPath(activeWave, active, style = Stroke(width = stroke))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatPlayerTime(currentPositionMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatPlayerTime(durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FamilySpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                shape = if (appearance.wavyPlayPauseEnabled) WavyPlayShape else MaterialTheme.shapes.extraLarge,
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
                            currentMediaIsVideo = isVideoMedia(context, uri)
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

private fun formatPlayerTime(milliseconds: Long): String {
    val seconds = (milliseconds.coerceAtLeast(0L) / 1000L).toInt()
    return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
}

private object WavyPlayShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            moveTo(w * 0.22f, 0f)
            cubicTo(w * 0.02f, 0f, 0f, h * 0.18f, 0f, h * 0.34f)
            cubicTo(-w * 0.02f, h * 0.62f, w * 0.08f, h, w * 0.28f, h)
            cubicTo(w * 0.48f, h * 1.02f, w * 0.55f, h * 0.94f, w * 0.72f, h)
            cubicTo(w * 0.92f, h * 1.02f, w, h * 0.82f, w, h * 0.62f)
            cubicTo(w * 1.02f, h * 0.36f, w * 0.91f, h * 0.08f, w * 0.72f, h * 0.04f)
            cubicTo(w * 0.53f, -h * 0.02f, w * 0.42f, h * 0.04f, w * 0.22f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}


private fun isVideoMedia(context: android.content.Context, uri: Uri): Boolean {
    val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
    if (mimeType?.startsWith("video/", ignoreCase = true) == true) return true
    val path = uri.lastPathSegment.orEmpty().lowercase(Locale.ROOT)
    return listOf(".mp4", ".mkv", ".webm", ".avi", ".mov", ".m4v", ".3gp", ".ts", ".mpeg", ".mpg")
        .any(path::endsWith)
}
