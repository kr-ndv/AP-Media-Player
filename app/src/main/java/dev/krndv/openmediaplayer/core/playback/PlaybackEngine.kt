package dev.krndv.openmediaplayer.core.playback

import android.net.Uri

enum class PlaybackStatus {
    IDLE,
    OPENING,
    PLAYING,
    PAUSED,
    STOPPED,
    ENDED,
    ERROR,
}

data class PlaybackSnapshot(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val errorMessage: String? = null,
) {
    val isPlaying: Boolean
        get() = status == PlaybackStatus.PLAYING
}

fun interface PlaybackStateListener {
    fun onPlaybackStateChanged(snapshot: PlaybackSnapshot)
}

/**
 * App-owned playback boundary. UI and repositories should not depend on LibVLC types.
 */
interface PlaybackEngine {
    val playbackState: PlaybackSnapshot

    val isPlaying: Boolean
        get() = playbackState.isPlaying

    fun addPlaybackStateListener(listener: PlaybackStateListener)
    fun removePlaybackStateListener(listener: PlaybackStateListener)

    fun open(uri: Uri, autoplay: Boolean = true)
    fun play()
    fun pause()
    fun stop()
    fun release()
}
