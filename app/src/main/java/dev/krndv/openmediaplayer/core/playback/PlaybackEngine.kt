package dev.krndv.openmediaplayer.core.playback

import android.net.Uri

/**
 * App-owned playback boundary. UI and repositories should not depend on LibVLC types.
 */
interface PlaybackEngine {
    val isPlaying: Boolean

    fun open(uri: Uri, autoplay: Boolean = true)
    fun play()
    fun pause()
    fun stop()
    fun release()
}
