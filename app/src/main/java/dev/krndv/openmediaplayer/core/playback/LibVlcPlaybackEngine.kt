package dev.krndv.openmediaplayer.core.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.concurrent.CopyOnWriteArraySet

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
    private val mainHandler = Handler(Looper.getMainLooper())
    private val stateListeners = CopyOnWriteArraySet<PlaybackStateListener>()

    @Volatile
    private var currentState = PlaybackSnapshot()

    @Volatile
    private var released = false

    private var attachedVideoLayout: VLCVideoLayout? = null

    override val playbackState: PlaybackSnapshot
        get() = currentState

    private val playerEventListener = MediaPlayer.EventListener { event ->
        when (event.type) {
            MediaPlayer.Event.Opening ->
                publishState(PlaybackSnapshot(PlaybackStatus.OPENING))

            MediaPlayer.Event.Playing ->
                publishState(PlaybackSnapshot(PlaybackStatus.PLAYING))

            MediaPlayer.Event.Paused ->
                publishState(PlaybackSnapshot(PlaybackStatus.PAUSED))

            MediaPlayer.Event.Stopped ->
                publishState(PlaybackSnapshot(PlaybackStatus.STOPPED))

            MediaPlayer.Event.EndReached ->
                publishState(PlaybackSnapshot(PlaybackStatus.ENDED))

            MediaPlayer.Event.EncounteredError ->
                publishState(PlaybackSnapshot(PlaybackStatus.ERROR))
        }
    }

    init {
        mediaPlayer.setEventListener(playerEventListener)
    }

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

    override fun addPlaybackStateListener(listener: PlaybackStateListener) {
        check(!released) { "PlaybackEngine has already been released" }
        stateListeners.add(listener)
        mainHandler.post {
            if (!released && stateListeners.contains(listener)) {
                listener.onPlaybackStateChanged(currentState)
            }
        }
    }

    override fun removePlaybackStateListener(listener: PlaybackStateListener) {
        stateListeners.remove(listener)
    }

    @Synchronized
    override fun open(uri: Uri, autoplay: Boolean) {
        check(!released) { "PlaybackEngine has already been released" }
        try {
            mediaPlayer.stop()
            val media = Media(libVlc, uri)
            try {
                media.setHWDecoderEnabled(true, false)
                mediaPlayer.media = media
            } finally {
                media.release()
            }

            if (autoplay) {
                publishState(PlaybackSnapshot(PlaybackStatus.OPENING))
                mediaPlayer.play()
            } else {
                publishState(PlaybackSnapshot())
            }
        } catch (exception: Exception) {
            publishState(
                PlaybackSnapshot(
                    status = PlaybackStatus.ERROR,
                    errorMessage = exception.localizedMessage,
                ),
            )
            throw exception
        }
    }

    @Synchronized
    override fun play() {
        check(!released) { "PlaybackEngine has already been released" }
        try {
            mediaPlayer.play()
        } catch (exception: Exception) {
            publishState(
                PlaybackSnapshot(
                    status = PlaybackStatus.ERROR,
                    errorMessage = exception.localizedMessage,
                ),
            )
            throw exception
        }
    }

    @Synchronized
    override fun pause() {
        if (!released) mediaPlayer.pause()
    }

    @Synchronized
    override fun stop() {
        if (released) return
        mediaPlayer.stop()
        publishState(PlaybackSnapshot(PlaybackStatus.STOPPED))
    }

    private fun publishState(snapshot: PlaybackSnapshot) {
        if (released) return
        currentState = snapshot
        mainHandler.post {
            if (!released) {
                val latest = currentState
                stateListeners.forEach { listener ->
                    if (stateListeners.contains(listener)) {
                        listener.onPlaybackStateChanged(latest)
                    }
                }
            }
        }
    }

    @Synchronized
    override fun release() {
        if (released) return
        released = true
        mediaPlayer.setEventListener(null)
        mediaPlayer.stop()
        if (attachedVideoLayout != null) mediaPlayer.detachViews()
        attachedVideoLayout = null
        stateListeners.clear()
        mediaPlayer.release()
        libVlc.release()
    }
}
