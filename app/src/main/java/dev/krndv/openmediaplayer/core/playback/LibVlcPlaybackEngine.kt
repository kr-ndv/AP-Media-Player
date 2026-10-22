package dev.krndv.openmediaplayer.core.playback

import android.content.Context
import android.net.Uri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * LibVLC adapter for the first playback prototype.
 *
 * The engine owns native playback resources. The player screen supplies a video
 * layout while visible; callers must release the engine when its owner is destroyed.
 */
class LibVlcPlaybackEngine(context: Context) : PlaybackEngine {
    private val libVlc = LibVLC(
        context.applicationContext,
        arrayListOf("--no-drop-late-frames"),
    )
    private val mediaPlayer = MediaPlayer(libVlc)
    private var released = false
    private var attachedVideoLayout: VLCVideoLayout? = null

    override val isPlaying: Boolean
        get() = !released && mediaPlayer.isPlaying

    @Synchronized
    fun attachVideoLayout(layout: VLCVideoLayout) {
        check(!released) { "PlaybackEngine has already been released" }
        if (attachedVideoLayout === layout) return
        if (attachedVideoLayout != null) mediaPlayer.detachViews()
        mediaPlayer.attachViews(layout, null, false, false)
        attachedVideoLayout = layout
    }

    @Synchronized
    fun detachVideoLayout(layout: VLCVideoLayout) {
        if (released || attachedVideoLayout !== layout) return
        mediaPlayer.detachViews()
        attachedVideoLayout = null
    }

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
        if (autoplay) mediaPlayer.play()
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
        if (attachedVideoLayout != null) mediaPlayer.detachViews()
        attachedVideoLayout = null
        mediaPlayer.release()
        libVlc.release()
    }
}
