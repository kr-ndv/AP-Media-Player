package dev.krndv.openmediaplayer.feature.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.krndv.openmediaplayer.core.playback.LibVlcPlaybackEngine
import dev.krndv.openmediaplayer.feature.player.PlaybackScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class MediaDestination(
    val label: String,
    val icon: ImageVector,
    val kind: MediaKind? = null,
    val isPlayer: Boolean = false,
) {
    LIBRARY("Library", Icons.Outlined.Home),
    MUSIC("Music", Icons.Outlined.MusicNote, MediaKind.AUDIO),
    VIDEOS("Videos", Icons.Outlined.Movie, MediaKind.VIDEO),
    PLAYER("Player", Icons.Outlined.PlayCircle, isPlayer = true),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaLibraryScreen(engine: LibVlcPlaybackEngine) {
    val context = LocalContext.current
    val repository = remember(context) { MediaRepository(context) }

    var selectedDestinationName by rememberSaveable { mutableStateOf(MediaDestination.LIBRARY.name) }
    val destination = MediaDestination.entries.firstOrNull { it.name == selectedDestinationName }
        ?: MediaDestination.LIBRARY

    var mediaItems by remember { mutableStateOf<List<LocalMediaItem>>(emptyList()) }
    var scanVersion by remember { mutableIntStateOf(-1) }
    var isScanning by remember { mutableStateOf(false) }
    var hasAudioPermission by remember { mutableStateOf(hasAudioPermission(context)) }
    var hasVideoPermission by remember { mutableStateOf(hasVideoPermission(context)) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var requestedUri by remember { mutableStateOf<Uri?>(null) }
    var requestedTitle by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasAudioPermission = hasAudioPermission(context)
        hasVideoPermission = hasVideoPermission(context)
        if (hasAudioPermission || hasVideoPermission) {
            scanVersion += 1
        } else {
            isScanning = false
            mediaItems = emptyList()
        }
    }

    val requestMediaAccess: () -> Unit = {
        val missing = mediaPermissions(context)
            .filterNot { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        if (missing.isEmpty()) {
            scanVersion += 1
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        val missing = mediaPermissions(context)
            .filterNot { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        if (missing.isEmpty()) {
            scanVersion = 0
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(scanVersion) {
        if (scanVersion < 0) return@LaunchedEffect
        hasAudioPermission = hasAudioPermission(context)
        hasVideoPermission = hasVideoPermission(context)
        if (!hasAudioPermission && !hasVideoPermission) {
            mediaItems = emptyList()
            isScanning = false
            return@LaunchedEffect
        }

        isScanning = true
        mediaItems = withContext(Dispatchers.IO) {
            repository.loadMedia(
                includeAudio = hasAudioPermission,
                includeVideo = hasVideoPermission,
            )
        }
        isScanning = false
    }

    val playMedia: (LocalMediaItem) -> Unit = { item ->
        requestedUri = item.uri
        requestedTitle = item.title
        selectedDestinationName = MediaDestination.PLAYER.name
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text(
                            text = "OpenMediaPlayer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val canReadAnything = hasAudioPermission(context) || hasVideoPermission(context)
                            if (canReadAnything) scanVersion += 1 else requestMediaAccess()
                        },
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Rescan media")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                MediaDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { selectedDestinationName = item.name },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label, maxLines = 1) },
                    )
                }
            }
        },
    ) { innerPadding ->
        if (destination.isPlayer) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 18.dp),
            ) {
                PlaybackScreen(
                    engine = engine,
                    requestedUri = requestedUri,
                    requestedTitle = requestedTitle,
                    onRequestConsumed = {
                        requestedUri = null
                        requestedTitle = null
                    },
                )
            }
        } else {
            val visibleItems = mediaItems
                .asSequence()
                .filter { destination.kind == null || it.kind == destination.kind }
                .filter { item ->
                    val query = searchQuery.trim()
                    query.isEmpty() ||
                        item.title.contains(query, ignoreCase = true) ||
                        item.artist.orEmpty().contains(query, ignoreCase = true) ||
                        item.album.orEmpty().contains(query, ignoreCase = true)
                }
                .toList()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = when (destination) {
                                MediaDestination.MUSIC -> "Your music"
                                MediaDestination.VIDEOS -> "Your videos"
                                else -> "Your media, all together."
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = when (destination) {
                                MediaDestination.MUSIC -> "Albums, songs, recordings and spoken audio on this device."
                                MediaDestination.VIDEOS -> "Clips, films and shows already stored on your device."
                                else -> "A private library for the audio and video already on your phone."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    LibraryHeroCard(
                        total = visibleItems.size,
                        isScanning = isScanning,
                        onRefresh = {
                            if (hasAudioPermission(context) || hasVideoPermission(context)) {
                                scanVersion += 1
                            } else {
                                requestMediaAccess()
                            }
                        },
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MediaCountCard(
                            modifier = Modifier.weight(1f),
                            label = "Audio",
                            count = mediaItems.count { it.kind == MediaKind.AUDIO },
                            icon = Icons.Outlined.MusicNote,
                        )
                        MediaCountCard(
                            modifier = Modifier.weight(1f),
                            label = "Video",
                            count = mediaItems.count { it.kind == MediaKind.VIDEO },
                            icon = Icons.Outlined.Movie,
                        )
                    }
                }

                if (!hasAudioPermission && !hasVideoPermission) {
                    item {
                        PermissionCard(onGrant = requestMediaAccess)
                    }
                } else {
                    if (!hasAudioPermission || !hasVideoPermission) {
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = "Some media access is off. Allow both audio and video access to build the complete library.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Search your library") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            shape = MaterialTheme.shapes.extraLarge,
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (destination) {
                                        MediaDestination.MUSIC -> "Audio files"
                                        MediaDestination.VIDEOS -> "Video files"
                                        else -> "Recently added"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${visibleItems.size} item${if (visibleItems.size == 1) "" else "s"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            }
                        }
                    }

                    if (!isScanning && visibleItems.isEmpty()) {
                        item {
                            EmptyLibraryCard(
                                title = if (searchQuery.isBlank()) "Nothing here yet" else "No matches",
                                description = if (searchQuery.isBlank()) {
                                    "Media that Android can index will appear here automatically. Add a file to your device, then rescan."
                                } else {
                                    "Try another title, artist or album name."
                                },
                                onRefresh = { scanVersion += 1 },
                            )
                        }
                    } else {
                        items(
                            items = visibleItems,
                            key = { "${it.kind.name}_${it.id}" },
                        ) { item ->
                            MediaItemCard(item = item, onClick = { playMedia(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeroCard(
    total: Int,
    isScanning: Boolean,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (isScanning) "Looking for media…" else "$total items in your library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = if (isScanning) {
                        "Checking the music and video indexed on this device."
                    } else {
                        "Found on your device. Nothing is uploaded."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

@Composable
private fun MediaCountCard(
    modifier: Modifier,
    label: String,
    count: Int,
    icon: ImageVector,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PermissionCard(onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Find the media on your phone",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Allow audio and video access so OpenMediaPlayer can automatically build your local library when it opens. Your files stay on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Allow media access")
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    title: String,
    description: String,
    onRefresh: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Rescan")
            }
        }
    }
}

@Composable
private fun MediaItemCard(
    item: LocalMediaItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (item.kind == MediaKind.AUDIO) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (item.kind == MediaKind.AUDIO) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (item.kind == MediaKind.AUDIO) Icons.Outlined.MusicNote else Icons.Outlined.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val detail = when (item.kind) {
                    MediaKind.AUDIO -> listOfNotNull(item.artist, item.album).joinToString(" · ")
                    MediaKind.VIDEO -> "Video"
                }.ifBlank { if (item.kind == MediaKind.AUDIO) "Audio" else "Video" }
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        item.durationMs.takeIf { it > 0 }?.let(::formatDuration),
                        item.sizeBytes.takeIf { it > 0 }?.let(::formatSize),
                    ).joinToString(" · ").ifBlank { if (item.kind == MediaKind.AUDIO) "Audio file" else "Video file" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalIconButton(onClick = onClick) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = "Play ${item.title}")
            }
        }
    }
}

private fun mediaPermissions(context: Context): List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        },
    ) == PackageManager.PERMISSION_GRANTED

private fun hasVideoPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        },
    ) == PackageManager.PERMISSION_GRANTED

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, seconds)
    }
}

private fun formatSize(sizeBytes: Long): String = when {
    sizeBytes >= 1024L * 1024L * 1024L ->
        String.format(Locale.getDefault(), "%.1f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0))
    sizeBytes >= 1024L * 1024L ->
        String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024.0 * 1024.0))
    else -> String.format(Locale.getDefault(), "%.0f KB", sizeBytes / 1024.0)
}
