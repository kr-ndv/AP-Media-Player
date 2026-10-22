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

/** LibVLC events that affect the app-owned playback state. */
internal enum class NativePlaybackEvent {
    OPENING,
    PLAYING,
    PAUSED,
    STOPPED,
    END_REACHED,
    ENCOUNTERED_ERROR,
}

/**
 * Converts native event categories into immutable app snapshots.
 *
 * LibVLC's Stopped event is deliberately not mapped to a state. The adapter's explicit
 * stop/open commands publish the corresponding state instead, because a delayed native
 * Stopped event for the previous item can otherwise overwrite the state of a replacement.
 */
internal object PlaybackEventStateMapper {
    fun toSnapshot(event: NativePlaybackEvent): PlaybackSnapshot? = when (event) {
        NativePlaybackEvent.OPENING -> PlaybackSnapshot(PlaybackStatus.OPENING)
        NativePlaybackEvent.PLAYING -> PlaybackSnapshot(PlaybackStatus.PLAYING)
        NativePlaybackEvent.PAUSED -> PlaybackSnapshot(PlaybackStatus.PAUSED)
        NativePlaybackEvent.STOPPED -> null
        NativePlaybackEvent.END_REACHED -> PlaybackSnapshot(PlaybackStatus.ENDED)
        NativePlaybackEvent.ENCOUNTERED_ERROR -> PlaybackSnapshot(PlaybackStatus.ERROR)
    }
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
