package dev.krndv.openmediaplayer.core.playback

import android.content.Context
import android.net.Uri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

/**
 * Initial LibVLC adapter for the feasibility prototype.
 *
 * This adapter deliberately does not own a video surface. Video output attachment,
 * lifecycle coordination, playback events and audio focus will be implemented in
 * the player feature after the native dependency has passed device-level checks.
 */
class LibVlcPlaybackEngine(context: Context) : PlaybackEngine {
    private val libVlc = LibVLC(context.applicationContext, arrayListOf("--no-drop-late-frames"))
    private val mediaPlayer = MediaPlayer(libVlc)
    private var released = false

    override val isPlaying: Boolean
        get() = !released && mediaPlayer.isPlaying

    @Synchronized
    override fun open(uri: Uri, autoplay: Boolean) {
        check(!released) { "PlaybackEngine has already been released" }

        mediaPlayer.stop()
        val media = Media(libVlc, uri)
        try {
            media.setHWDecoderEnabled(true, false)
            mediaPlayer.media = media
        } finally {
            media.release()
        }

        if (autoplay) {
            mediaPlayer.play()
        }
    }

    @Synchronized
    override fun play() {
        check(!released) { "PlaybackEngine has already been released" }
        mediaPlayer.play()
    }

    @Synchronized
    override fun pause() {
        if (!released) mediaPlayer.pause()
    }

    @Synchronized
    override fun stop() {
        if (!released) mediaPlayer.stop()
    }

    @Synchronized
    override fun release() {
        if (released) return
        released = true
        mediaPlayer.stop()
        mediaPlayer.release()
        libVlc.release()
    }
}
