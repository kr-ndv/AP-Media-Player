package dev.krndv.openmediaplayer.feature.library

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import dev.krndv.openmediaplayer.core.playback.PlaybackStateListener
import dev.krndv.openmediaplayer.core.playback.PlaybackStatus
import dev.krndv.openmediaplayer.feature.player.MusicQueueEntry
import dev.krndv.openmediaplayer.feature.player.PlaybackScreen
import dev.krndv.openmediaplayer.feature.player.PlayerAppearance
import dev.krndv.openmediaplayer.feature.player.PlayerAppearanceSettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class MediaDestination(
    val label: String,
    val icon: ImageVector,
    val kind: MediaKind? = null,
    val isPlayer: Boolean = false,
    val isSettings: Boolean = false,
    val isFavorites: Boolean = false,
) {
    LIBRARY("Home", Icons.Outlined.Home),
    MUSIC("Music", Icons.Outlined.MusicNote, MediaKind.AUDIO),
    VIDEOS("Videos", Icons.Outlined.Movie, MediaKind.VIDEO),
    COLLECTIONS("Library", Icons.Outlined.FolderOpen),
    FAVORITES("Favorites", Icons.Outlined.Favorite, kind = MediaKind.AUDIO, isFavorites = true),
    PLAYER("Player", Icons.Outlined.PlayCircle, isPlayer = true),
    SETTINGS("Settings", Icons.Outlined.Settings, isSettings = true),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaLibraryScreen(
    engine: LibVlcPlaybackEngine,
    themeMode: String = "system",
    onThemeModeChange: (String) -> Unit = {},
    dynamicColor: Boolean = false,
    onDynamicColorChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember(context) { MediaRepository(context) }
    val playlistRepository = remember(context) { PlaylistRepository(context) }
    var playlists by remember(context) { mutableStateOf(playlistRepository.load()) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var newPlaylistName by rememberSaveable { mutableStateOf("") }
    var musicCategory by rememberSaveable { mutableStateOf("Songs") }

    var selectedDestinationName by rememberSaveable { mutableStateOf(MediaDestination.LIBRARY.name) }
    var previousDestinationName by rememberSaveable { mutableStateOf(MediaDestination.LIBRARY.name) }
    val destination = MediaDestination.entries.firstOrNull { it.name == selectedDestinationName }
        ?: MediaDestination.LIBRARY

    var mediaItems by remember { mutableStateOf<List<LocalMediaItem>>(emptyList()) }
    var scanVersion by remember { mutableIntStateOf(-1) }
    var isScanning by remember { mutableStateOf(false) }
    var hasAudioPermission by remember { mutableStateOf(canReadAudioMedia(context)) }
    var hasVideoPermission by remember { mutableStateOf(canReadVideoMedia(context)) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var requestedUri by remember { mutableStateOf<Uri?>(null) }
    var requestedTitle by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = destination.isPlayer) {
        requestedUri = null
        requestedTitle = null
        selectedDestinationName = MediaDestination.entries
            .firstOrNull { it.name == previousDestinationName && !it.isPlayer }
            ?.name ?: MediaDestination.LIBRARY.name
    }
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it.take(60) },
                    label = { Text("Playlist name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val created = playlistRepository.create(newPlaylistName)
                        if (created != null) {
                            playlists = playlistRepository.load()
                            selectedPlaylistId = created.id
                            musicCategory = "Playlists"
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    enabled = newPlaylistName.isNotBlank() && playlists.none { it.name.equals(newPlaylistName.trim(), ignoreCase = true) },
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") } },
        )
    }
    var activeMediaTitle by rememberSaveable { mutableStateOf("") }
    var activeMediaArtist by rememberSaveable { mutableStateOf("") }
    var activeMediaIsVideo by rememberSaveable { mutableStateOf(false) }
    var activeMediaUri by remember { mutableStateOf<Uri?>(null) }
    var playbackSnapshot by remember(engine) { mutableStateOf(engine.playbackState) }
    var playerAppearance by remember(context) { mutableStateOf(PlayerAppearance.load(context)) }
    val videoPreferences = remember(context) { context.getSharedPreferences("ap_video_player", Context.MODE_PRIVATE) }
    var videoOrientation by remember(context) { mutableStateOf(videoPreferences.getString("orientation", "auto") ?: "auto") }
    val updatePlayerAppearance: (PlayerAppearance) -> Unit = { updated ->
        playerAppearance = updated
        updated.save(context)
    }

    DisposableEffect(engine) {
        val listener = PlaybackStateListener { playbackSnapshot = it }
        engine.addPlaybackStateListener(listener)
        onDispose { engine.removePlaybackStateListener(listener) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasAudioPermission = canReadAudioMedia(context)
        hasVideoPermission = canReadVideoMedia(context)
        if (hasAudioPermission || hasVideoPermission) {
            scanVersion += 1
        } else {
            isScanning = false
            mediaItems = emptyList()
        }
    }

    val requestMediaAccess: () -> Unit = {
        val missing = mediaPermissions()
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
        val missing = mediaPermissions()
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
        hasAudioPermission = canReadAudioMedia(context)
        hasVideoPermission = canReadVideoMedia(context)
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
        activeMediaTitle = item.title
        activeMediaArtist = item.artist.orEmpty()
        activeMediaIsVideo = item.kind == MediaKind.VIDEO
        activeMediaUri = item.uri
        // Audio and video selections both enter their dedicated player directly.
        // Video waits for VLCVideoLayout attachment; audio can start immediately.
        previousDestinationName = selectedDestinationName
        requestedUri = item.uri
        requestedTitle = item.title
        selectedDestinationName = MediaDestination.PLAYER.name
    }

    Scaffold(
        topBar = {
            if (!destination.isPlayer) TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text(
                            text = "AV Music Player",
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
                    if (!destination.isSettings) {
                        IconButton(onClick = { selectedDestinationName = MediaDestination.SETTINGS.name }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    }
                    IconButton(
                        onClick = {
                            val canReadAnything = canReadAudioMedia(context) || canReadVideoMedia(context)
                            if (canReadAnything) scanVersion += 1 else requestMediaAccess()
                        },
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Rescan media")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        },
        bottomBar = {
            if (!destination.isPlayer) {
                Column {
                    if (activeMediaTitle.isNotBlank() &&
                        playbackSnapshot.status !in setOf(PlaybackStatus.IDLE, PlaybackStatus.ERROR)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 3.dp,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        previousDestinationName = selectedDestinationName
                                        selectedDestinationName = MediaDestination.PLAYER.name
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val miniItem = mediaItems.firstOrNull { it.uri == activeMediaUri }
                                    if (miniItem != null) {
                                        MediaArtwork(miniItem, Modifier.fillMaxSize())
                                    } else {
                                        Icon(
                                            if (activeMediaIsVideo) Icons.Outlined.Movie else Icons.Outlined.MusicNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        activeMediaTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        if (playbackSnapshot.status == PlaybackStatus.PLAYING) "Now playing" else "Paused",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = {
                                    if (playbackSnapshot.status == PlaybackStatus.PLAYING) engine.pause() else engine.play()
                                }) {
                                    Icon(
                                        if (playbackSnapshot.status == PlaybackStatus.PLAYING) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                        contentDescription = if (playbackSnapshot.status == PlaybackStatus.PLAYING) "Pause" else "Play",
                                    )
                                }
                            }
                        }
                    }
                    NavigationBar {
                        MediaDestination.entries.filterNot { it.isPlayer || it.isSettings }.forEach { item ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { selectedDestinationName = item.name },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(item.label, maxLines = 1) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        if (destination.isSettings) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            ) {
                SettingsSectionCard(title = "Appearance", subtitle = "Choose how AV Music Player looks on this device.") {
                    Text("Theme", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                            FilterChip(selected = themeMode == value, onClick = { onThemeModeChange(value) }, label = { Text(label) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Dynamic color", style = MaterialTheme.typography.titleSmall)
                            Text("Use colors from your wallpaper on Android 12 and newer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FilterChip(selected = dynamicColor, onClick = { onDynamicColorChange(!dynamicColor) }, label = { Text(if (dynamicColor) "On" else "Off") })
                    }
                }
                SettingsSectionCard(title = "Video playback", subtitle = "Default orientation applied when opening a video.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("auto" to "Auto", "landscape" to "Landscape", "portrait" to "Portrait").forEach { (value, label) ->
                            FilterChip(
                                selected = videoOrientation == value,
                                onClick = {
                                    videoOrientation = value
                                    videoPreferences.edit().putString("orientation", value).apply()
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                }
                SettingsSectionCard(title = "Library", subtitle = "Your media is discovered locally using Android MediaStore.") {
                    Text("${mediaItems.count { it.kind == MediaKind.AUDIO }} audio tracks", style = MaterialTheme.typography.bodyMedium)
                    Text("${mediaItems.count { it.kind == MediaKind.VIDEO }} videos", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { scanVersion += 1 }, enabled = !isScanning) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (isScanning) "Scanning…" else "Rescan media")
                    }
                }
                SettingsSectionCard(title = "Player appearance", subtitle = "Customize the Now Playing experience.") {
                    PlayerAppearanceSettingsScreen(
                        appearance = playerAppearance,
                        onAppearanceChange = updatePlayerAppearance,
                    )
                }
                SettingsSectionCard(title = "About", subtitle = "AV Music Player · version 0.1.0") {
                    Text("Local-first media player. Your library is scanned on your device and is not uploaded.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Background playback, system media controls and a persistent playback service are not yet implemented.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (destination.isPlayer) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(
                        if (activeMediaIsVideo) Modifier
                        else Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaybackScreen(
                    engine = engine,
                    requestedUri = requestedUri,
                    requestedTitle = requestedTitle,
                    initialTitle = activeMediaTitle,
                    initialArtist = activeMediaArtist,
                    initialIsVideo = activeMediaIsVideo,
                    initialUri = activeMediaUri,
                    appearance = playerAppearance,
                    onRequestConsumed = {
                        requestedUri = null
                        requestedTitle = null
                    },
                    onMediaChanged = { title, isVideo ->
                        activeMediaTitle = title
                        activeMediaIsVideo = isVideo
                    },
                    onExitPlayer = {
                        requestedUri = null
                        requestedTitle = null
                        selectedDestinationName = MediaDestination.entries
                            .firstOrNull { it.name == previousDestinationName && !it.isPlayer }
                            ?.name ?: MediaDestination.LIBRARY.name
                    },
                    onPreviousTrack = { queue, shuffle ->
                        val available = queue.mapNotNull { entry ->
                            mediaItems.firstOrNull { it.uri == entry.uri }
                        }
                        if (available.isNotEmpty()) {
                            val index = available.indexOfFirst { it.uri == activeMediaUri }
                            val target = if (shuffle && available.size > 1) {
                                available.indices.filter { it != index }.random()
                            } else if (index <= 0) available.lastIndex else index - 1
                            playMedia(available[target])
                        }
                    },
                    onNextTrack = { queue, shuffle ->
                        val available = queue.mapNotNull { entry ->
                            mediaItems.firstOrNull { it.uri == entry.uri }
                        }
                        if (available.isNotEmpty()) {
                            val index = available.indexOfFirst { it.uri == activeMediaUri }
                            val target = if (shuffle && available.size > 1) {
                                available.indices.filter { it != index }.random()
                            } else if (index < 0 || index == available.lastIndex) 0 else index + 1
                            playMedia(available[target])
                        }
                    },
                    musicQueue = mediaItems.filter { it.kind == MediaKind.AUDIO }.map { item ->
                        MusicQueueEntry(uri = item.uri, title = item.title, artist = item.artist)
                    },
                    onQueueTrackSelected = { entry ->
                        mediaItems.firstOrNull { it.uri == entry.uri }?.let(playMedia)
                    },
                )
            }
        } else {
            val filteredItems = mediaItems
                .asSequence()
                .filter { destination.kind == null || it.kind == destination.kind }
                .filter { item ->
                    !destination.isFavorites || context.getSharedPreferences("ap_music_player", Context.MODE_PRIVATE)
                        .getBoolean("favorite:${item.uri}", false)
                }
                .filter { item ->
                    val query = searchQuery.trim()
                    query.isEmpty() ||
                        item.title.contains(query, ignoreCase = true) ||
                        item.artist.orEmpty().contains(query, ignoreCase = true) ||
                        item.album.orEmpty().contains(query, ignoreCase = true)
                }
                .toList()
            val visibleItems = when {
                destination == MediaDestination.MUSIC && musicCategory == "Albums" ->
                    filteredItems.filter { it.kind == MediaKind.AUDIO }.distinctBy { it.album ?: "Unknown album" }
                        .map { it.copy(title = it.album ?: "Unknown album") }
                destination == MediaDestination.MUSIC && musicCategory == "Artists" ->
                    filteredItems.filter { it.kind == MediaKind.AUDIO }.distinctBy { it.artist ?: "Unknown artist" }
                        .map { it.copy(title = it.artist ?: "Unknown artist") }
                else -> filteredItems
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (destination == MediaDestination.MUSIC || destination == MediaDestination.VIDEOS) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (destination == MediaDestination.MUSIC) "Songs" else "All videos",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${visibleItems.size} items on this device",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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

                    if (destination != MediaDestination.COLLECTIONS) item {
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

                    if (destination != MediaDestination.LIBRARY && destination != MediaDestination.COLLECTIONS) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterChip(
                                    selected = destination == MediaDestination.MUSIC,
                                    onClick = { selectedDestinationName = MediaDestination.MUSIC.name },
                                    label = { Text("Music") },
                                )
                                FilterChip(
                                    selected = destination == MediaDestination.VIDEOS,
                                    onClick = { selectedDestinationName = MediaDestination.VIDEOS.name },
                                    label = { Text("Videos") },
                                )
                            }
                        }
                    }

                    if (destination == MediaDestination.MUSIC) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Songs", "Albums", "Artists", "Playlists").forEach { category ->
                                    FilterChip(
                                        selected = musicCategory == category,
                                        onClick = {
                                            musicCategory = category
                                            if (category == "Playlists") selectedDestinationName = MediaDestination.COLLECTIONS.name
                                        },
                                        label = { Text(category) },
                                    )
                                }
                            }
                        }
                    }
                    if (destination == MediaDestination.COLLECTIONS) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            if (selectedPlaylistId == null) "Playlists" else playlists.firstOrNull { it.id == selectedPlaylistId }?.name ?: "Playlist",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            if (selectedPlaylistId == null) "${playlists.size} playlists" else "Manage songs in this playlist",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (selectedPlaylistId == null) {
                                        Button(onClick = { showCreatePlaylistDialog = true }) { Text("Create playlist") }
                                    } else {
                                        TextButton(onClick = { selectedPlaylistId = null }) { Text("Back") }
                                    }
                                }
                                if (selectedPlaylistId == null) {
                                    if (playlists.isEmpty()) {
                                        EmptyLibraryCard(
                                            title = "No playlists yet",
                                            description = "Create a playlist, then add songs from your music library.",
                                            onRefresh = { showCreatePlaylistDialog = true },
                                        )
                                    } else {
                                        playlists.forEach { playlist ->
                                            Card(
                                                onClick = { selectedPlaylistId = playlist.id },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = MaterialTheme.shapes.large,
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                ) {
                                                    Surface(
                                                        modifier = Modifier.size(48.dp),
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.MusicNote, contentDescription = null) }
                                                    }
                                                    Column(Modifier.weight(1f)) {
                                                        Text(playlist.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                                        Text("${playlist.mediaUris.size} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    TextButton(onClick = { playlists = playlistRepository.delete(playlist.id) }) { Text("Delete") }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val playlist = playlists.firstOrNull { it.id == selectedPlaylistId }
                                    val trackUris = playlist?.mediaUris.orEmpty().toSet()
                                    Text("In this playlist", style = MaterialTheme.typography.titleMedium)
                                    val savedTracks = mediaItems.filter { it.uri.toString() in trackUris }
                                    if (savedTracks.isEmpty()) {
                                        Text("No songs added yet. Choose tracks below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        savedTracks.forEach { track ->
                                            MediaItemCard(
                                                item = track,
                                                onClick = { playMedia(track) },
                                                trailingAction = {
                                                    TextButton(onClick = {
                                                        playlists = playlistRepository.removeTrack(selectedPlaylistId!!, track.uri.toString())
                                                    }) { Text("Remove") }
                                                },
                                            )
                                        }
                                    }
                                    Text("Add songs", style = MaterialTheme.typography.titleMedium)
                                    mediaItems.filter { it.kind == MediaKind.AUDIO && it.uri.toString() !in trackUris }.take(40).forEach { track ->
                                        MediaItemCard(
                                            item = track,
                                            onClick = { playMedia(track) },
                                            trailingAction = {
                                                TextButton(onClick = {
                                                    playlists = playlistRepository.addTrack(selectedPlaylistId!!, track.uri.toString())
                                                }) { Text("Add") }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (destination == MediaDestination.LIBRARY || destination == MediaDestination.MUSIC ||
                        destination == MediaDestination.VIDEOS
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = if (destination == MediaDestination.VIDEOS) "Recent videos" else "Recently added",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = "See all",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    visibleItems.take(4).forEachIndexed { index, media ->
                                        Column(
                                            modifier = Modifier.width(126.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(112.dp)
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(
                                                                when (index % 4) {
                                                                    0 -> MaterialTheme.colorScheme.primaryContainer
                                                                    1 -> MaterialTheme.colorScheme.tertiaryContainer
                                                                    2 -> MaterialTheme.colorScheme.secondaryContainer
                                                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                                                },
                                                                MaterialTheme.colorScheme.surfaceContainer,
                                                            ),
                                                        ),
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                MediaArtwork(media, Modifier.fillMaxSize())
                                                FilledTonalIconButton(
                                                    onClick = { playMedia(media) },
                                                    modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp).size(34.dp),
                                                ) {
                                                    Icon(Icons.Outlined.PlayArrow, contentDescription = "Play ${media.title}")
                                                }
                                            }
                                            Text(
                                                media.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = media.artist.orEmpty().ifBlank {
                                                    if (media.kind == MediaKind.VIDEO) "Video" else "On this device"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (destination == MediaDestination.LIBRARY) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Explore",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    QuickPickCard(
                                        modifier = Modifier.weight(1f),
                                        title = "Music",
                                        subtitle = "${mediaItems.count { it.kind == MediaKind.AUDIO }} songs",
                                        icon = Icons.Outlined.MusicNote,
                                        onClick = { selectedDestinationName = MediaDestination.MUSIC.name },
                                    )
                                    QuickPickCard(
                                        modifier = Modifier.weight(1f),
                                        title = "Videos",
                                        subtitle = "${mediaItems.count { it.kind == MediaKind.VIDEO }} videos",
                                        icon = Icons.Outlined.Movie,
                                        onClick = { selectedDestinationName = MediaDestination.VIDEOS.name },
                                    )
                                }
                            }
                        }
                    }

                    if (destination != MediaDestination.LIBRARY && destination != MediaDestination.COLLECTIONS) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when (destination) {
                                            MediaDestination.MUSIC -> "All songs"
                                            MediaDestination.VIDEOS -> "All videos"
                                            else -> "Your library"
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
                                if (isScanning) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
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
                    } else if (!isScanning && visibleItems.isEmpty()) {
                        item {
                            EmptyLibraryCard(
                                title = if (searchQuery.isBlank()) "Your library is empty" else "No matches",
                                description = if (searchQuery.isBlank()) "Add music or videos to your device, then scan your library." else "Try another title, artist or album name.",
                                onRefresh = { scanVersion += 1 },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPickCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Rescan media",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
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
                text = "Allow audio and video access so AV Music Player can automatically build your local library when it opens. Your files stay on this device.",
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

private object MediaArtworkCache {
    private val cache = object : android.util.LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    @Synchronized
    fun get(key: String): Bitmap? = cache.get(key)

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}

private fun loadMediaArtwork(context: Context, item: LocalMediaItem): Bitmap? {
    val key = "${item.kind}:${item.uri}"
    MediaArtworkCache.get(key)?.let { return it }
    val bitmap = runCatching {
        MediaMetadataRetriever().let { retriever ->
            try {
                retriever.setDataSource(context, item.uri)
                if (item.kind == MediaKind.VIDEO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        retriever.getScaledFrameAtTime(
                            0L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            512,
                            512,
                        )
                    } else {
                        retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                } else {
                    retriever.embeddedPicture?.let { bytes ->
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        val sample = (maxOf(bounds.outWidth, bounds.outHeight) / 512).coerceAtLeast(1)
                        BitmapFactory.decodeByteArray(
                            bytes,
                            0,
                            bytes.size,
                            BitmapFactory.Options().apply { inSampleSize = sample },
                        )
                    }
                }
            } finally {
                retriever.release()
            }
        }
    }.getOrNull() ?: return null
    MediaArtworkCache.put(key, bitmap)
    return bitmap
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}
@Composable
private fun MediaArtwork(
    item: LocalMediaItem,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember(item.uri, item.kind) {
        mutableStateOf(MediaArtworkCache.get("${item.kind}:${item.uri}"))
    }
    LaunchedEffect(item.uri, item.kind) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) { loadMediaArtwork(context, item) }
        }
    }
    val art = bitmap
    if (art != null) {
        Image(
            bitmap = art.asImageBitmap(),
            contentDescription = "${item.title} artwork",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (item.kind == MediaKind.AUDIO) Icons.Outlined.MusicNote else Icons.Outlined.Movie,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun MediaItemCard(
    item: LocalMediaItem,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null,
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
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (item.kind == MediaKind.AUDIO) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                MediaArtwork(item, Modifier.fillMaxSize())
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
            if (trailingAction != null) {
                trailingAction()
            } else {
                FilledTonalIconButton(onClick = onClick) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = "Play ${item.title}")
                }
            }
        }
    }
}

private fun mediaPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun canReadAudioMedia(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        },
    ) == PackageManager.PERMISSION_GRANTED

private fun canReadVideoMedia(context: Context): Boolean =
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
