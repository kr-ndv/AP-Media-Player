package dev.krndv.openmediaplayer.feature.player

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
    initialTitle: String = "",
    initialArtist: String = "",
    initialIsVideo: Boolean = false,
    initialUri: Uri? = null,
    appearance: PlayerAppearance = PlayerAppearance(),
    onRequestConsumed: () -> Unit = {},
    onMediaChanged: (title: String, isVideo: Boolean) -> Unit = { _, _ -> },
    onExitPlayer: () -> Unit = {},
    onPreviousTrack: () -> Unit = {},
    onNextTrack: () -> Unit = {},
) {
    val context = LocalContext.current
    var streamUrl by rememberSaveable { mutableStateOf("") }
    var playbackSnapshot by remember(engine) { mutableStateOf(engine.playbackState) }
    var feedback by rememberSaveable { mutableStateOf("") }
    var currentMediaTitle by remember(engine, initialTitle) { mutableStateOf(initialTitle) }
    var currentMediaArtist by remember(engine, initialArtist) { mutableStateOf(initialArtist) }
    var currentMediaIsVideo by remember(engine, initialIsVideo) { mutableStateOf(initialIsVideo) }
    var currentMediaUri by remember(engine, initialUri) { mutableStateOf(initialUri) }
    var artworkBitmap by remember(currentMediaUri, currentMediaIsVideo) { mutableStateOf<Bitmap?>(null) }
    var videoLayout by remember { mutableStateOf<VLCVideoLayout?>(null) }
    var currentPositionMs by remember(engine) { mutableStateOf(engine.currentTimeMs) }
    var durationMs by remember(engine) { mutableStateOf(engine.durationMs) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var isFavorite by rememberSaveable { mutableStateOf(false) }
    var shuffleEnabled by rememberSaveable { mutableStateOf(false) }
    var repeatEnabled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentMediaUri, currentMediaIsVideo) {
        val uri = currentMediaUri
        artworkBitmap = if (uri == null) null else withContext(Dispatchers.IO) {
            runCatching {
                MediaMetadataRetriever().let { retriever ->
                    try {
                        retriever.setDataSource(context, uri)
                        if (currentMediaIsVideo) {
                            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        } else {
                            retriever.embeddedPicture?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                        }
                    } finally {
                        retriever.release()
                    }
                }
            }.getOrNull()
        }
    }

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
            currentMediaArtist = "On this device"
            currentMediaUri = uri
            currentMediaIsVideo = isVideoMedia(context, uri)
            onMediaChanged(currentMediaTitle, currentMediaIsVideo)
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

    // Use an immersive landscape orientation for video; restore the user's
    // previous orientation when leaving the video player.
    DisposableEffect(currentMediaIsVideo) {
        val activity = context.findActivity()
        val previousOrientation = activity?.requestedOrientation
        if (currentMediaIsVideo && activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity.window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }
        onDispose {
            if (activity != null) {
                activity.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                activity.window.decorView.systemUiVisibility = 0
            }
        }
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
        if (currentMediaIsVideo && videoLayout == null) return@LaunchedEffect

        feedback = ""
        currentMediaTitle = requestedTitle ?: uri.lastPathSegment ?: "Selected media"
        currentMediaArtist = initialArtist
        currentMediaUri = uri
        currentMediaIsVideo = isVideoMedia(context, uri)
        onMediaChanged(currentMediaTitle, currentMediaIsVideo)
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

    if (currentMediaIsVideo) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onExitPlayer) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Exit video player", tint = Color.White)
                }
                Text(
                    currentMediaTitle.ifBlank { "Video player" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                IconButton(onClick = { feedback = if (feedback.isBlank()) "Video settings are coming soon." else "" }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Video options")
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
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
                    update = { layout -> if (videoLayout !== layout) videoLayout = layout },
                )
                if (playbackSnapshot.status != PlaybackStatus.PLAYING) {
                    FilledIconButton(
                        onClick = { runCatching { engine.play() } },
                        modifier = Modifier.align(Alignment.Center).size(68.dp),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = "Play video", modifier = Modifier.size(36.dp))
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f))))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(formatPlayerTime(currentPositionMs), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    Canvas(
                        modifier = Modifier.weight(1f).height(24.dp)
                            .pointerInput(durationMs) {
                                detectTapGestures { point ->
                                    if (durationMs > 0 && size.width > 0) engine.seekTo((point.x / size.width * durationMs).toLong())
                                }
                            },
                    ) {
                        val fraction = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                        drawLine(Color.White.copy(alpha = 0.45f), androidx.compose.ui.geometry.Offset(0f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width, size.height / 2), strokeWidth = 4.dp.toPx())
                        drawLine(Color(0xFFD4B8FF), androidx.compose.ui.geometry.Offset(0f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * fraction, size.height / 2), strokeWidth = 4.dp.toPx())
                    }
                    Text(formatPlayerTime(durationMs), color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { engine.seekTo((currentPositionMs - 10_000L).coerceAtLeast(0L)) }) {
                    Text("−10", style = MaterialTheme.typography.labelLarge)
                }
                FilledIconButton(
                    onClick = {
                        if (playbackSnapshot.status == PlaybackStatus.PLAYING) engine.pause() else engine.play()
                    },
                    modifier = Modifier.size(60.dp),
                ) {
                    Icon(
                        if (playbackSnapshot.status == PlaybackStatus.PLAYING) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = "Play or pause",
                        modifier = Modifier.size(30.dp),
                    )
                }
                IconButton(onClick = { engine.seekTo((currentPositionMs + 10_000L).coerceAtMost(durationMs)) }) {
                    Text("+10", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (statusText.isNotBlank()) Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(selected = !showLyrics, onClick = { showLyrics = false }, label = { Text("Song") })
                androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
                FilterChip(selected = showLyrics, onClick = { showLyrics = true }, label = { Text("Lyrics") })
            }

            if (showLyrics) {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 360.dp).padding(horizontal = 10.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text(currentMediaTitle.ifBlank { "Lyrics" }, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Lyrics aren't available for this local track yet. Add a synced or plain-text lyrics source to display them here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.78f),
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val artwork = artworkBitmap
                    if (artwork != null) {
                        Image(
                            bitmap = artwork.asImageBitmap(),
                            contentDescription = "Artwork for ${currentMediaTitle.ifBlank { "current track" }}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(
                                modifier = Modifier.size(112.dp),
                                shape = RoundedCornerShape(34.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.20f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(60.dp))
                                }
                            }
                            Text(
                                "AP MEDIA PLAYER",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            currentMediaTitle.ifBlank { "Nothing playing" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                        )
                        Text(currentMediaArtist.ifBlank { "On this device" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { feedback = "Track options are coming soon." }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Track options")
                    }
                }

                if (durationMs > 0L) {
                    val audioProgressTrack = MaterialTheme.colorScheme.surfaceVariant
                    val audioProgressActive = MaterialTheme.colorScheme.primary
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Canvas(
                            modifier = Modifier.fillMaxWidth().height(28.dp)
                                .pointerInput(durationMs) {
                                    detectTapGestures { point ->
                                        if (size.width > 0) engine.seekTo((point.x / size.width * durationMs).toLong())
                                    }
                                }
                                .pointerInput(durationMs) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        if (size.width > 0) engine.seekTo((change.position.x / size.width * durationMs).toLong())
                                    }
                                },
                        ) {
                            val width = size.width
                            val fraction = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                            val y = size.height / 2f
                            drawLine(audioProgressTrack, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(width, y), strokeWidth = 3.dp.toPx())
                            drawLine(audioProgressActive, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(width * fraction, y), strokeWidth = 4.dp.toPx())
                            drawCircle(audioProgressActive, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(width * fraction, y))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatPlayerTime(currentPositionMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatPlayerTime(durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { shuffleEnabled = !shuffleEnabled }) {
                        Icon(Icons.Outlined.Shuffle, contentDescription = "Shuffle", tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onPreviousTrack) {
                        Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous track")
                    }
                    FilledIconButton(
                        onClick = {
                            feedback = ""
                            runCatching {
                                if (playbackSnapshot.status == PlaybackStatus.PLAYING) engine.pause() else engine.play()
                            }.onFailure { feedback = it.localizedMessage ?: "Playback action failed" }
                        },
                        modifier = Modifier.size(76.dp),
                    ) {
                        Icon(
                            if (playbackSnapshot.status == PlaybackStatus.PLAYING) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = "Play or pause",
                            modifier = Modifier.size(38.dp),
                        )
                    }
                    IconButton(onClick = onNextTrack) {
                        Icon(Icons.Outlined.SkipNext, contentDescription = "Next track")
                    }
                    IconButton(onClick = { repeatEnabled = !repeatEnabled }) {
                        Icon(Icons.Outlined.Repeat, contentDescription = "Repeat", tint = if (repeatEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { feedback = "Queue controls are coming soon." },
                        label = { androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.QueueMusic, contentDescription = null); androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp)); Text("Queue") } },
                    )
                }
            }
            if (statusText.isNotBlank()) {
                Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (currentMediaTitle.isBlank()) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(modifier = Modifier.padding(FamilySpacing.large), verticalArrangement = Arrangement.spacedBy(FamilySpacing.medium)) {
                        Text(stringResource(R.string.player_local_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.player_local_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { filePicker.launch(arrayOf("audio/*", "video/*")) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.player_choose_file))
                        }
                    }
                }
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(modifier = Modifier.padding(FamilySpacing.large), verticalArrangement = Arrangement.spacedBy(FamilySpacing.medium)) {
                        Text(stringResource(R.string.player_network_title), style = MaterialTheme.typography.titleLarge)
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
                                if (value.isBlank() || uri == null || scheme == null || scheme !in setOf("http", "https", "rtsp", "rtmp", "udp", "rtp")) {
                                    feedback = context.getString(R.string.player_invalid_url)
                                } else {
                                    currentMediaTitle = value
                                    currentMediaArtist = "Network stream"
                                    currentMediaUri = uri
                                    currentMediaIsVideo = isVideoMedia(context, uri)
                                    onMediaChanged(currentMediaTitle, currentMediaIsVideo)
                                    feedback = ""
                                    runCatching { engine.open(uri) }.onFailure { feedback = it.localizedMessage ?: "Unable to open stream" }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.player_open_stream)) }
                    }
                }
            }
        }
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

private fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
