package dev.krndv.openmediaplayer.core.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
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

    @Volatile
    private var currentUri: Uri? = null

    private var attachedVideoLayout: VLCVideoLayout? = null

    override val playbackState: PlaybackSnapshot
        get() = currentState

    private val playerEventListener = MediaPlayer.EventListener { event ->
        val uriScheme = currentUri?.scheme ?: "unknown"
        when (event.type) {
            MediaPlayer.Event.Opening -> {
                Log.d(TAG, "LibVLC event=Opening scheme=" + uriScheme)
                publishState(PlaybackSnapshot(PlaybackStatus.OPENING))
            }

            MediaPlayer.Event.Playing -> {
                Log.i(TAG, "LibVLC event=Playing scheme=" + uriScheme)
                publishState(PlaybackSnapshot(PlaybackStatus.PLAYING))
            }

            MediaPlayer.Event.Paused -> {
                Log.d(TAG, "LibVLC event=Paused scheme=" + uriScheme)
                publishState(PlaybackSnapshot(PlaybackStatus.PAUSED))
            }

            MediaPlayer.Event.Stopped -> {
                Log.d(TAG, "LibVLC event=Stopped scheme=" + uriScheme)
                publishState(PlaybackSnapshot(PlaybackStatus.STOPPED))
            }

            MediaPlayer.Event.EndReached -> {
                Log.d(TAG, "LibVLC event=EndReached scheme=" + uriScheme)
                publishState(PlaybackSnapshot(PlaybackStatus.ENDED))
            }

            MediaPlayer.Event.EncounteredError -> {
                // Avoid logging full paths or signed URLs, which may contain private data.
                Log.e(TAG, "LibVLC event=EncounteredError scheme=" + uriScheme)
                publishState(
                    PlaybackSnapshot(
                        status = PlaybackStatus.ERROR,
                        errorMessage = "LibVLC could not open or decode this media. Check device logs for playback diagnostics.",
                    ),
                )
            }
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
        Log.d(TAG, "Video output attached")
    }

    @Synchronized
    fun detachVideoLayout(layout: VLCVideoLayout) {
        if (released || attachedVideoLayout !== layout) return
        mediaPlayer.detachViews()
        attachedVideoLayout = null
        Log.d(TAG, "Video output detached")
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
        currentUri = uri
        Log.i(TAG, "Opening media scheme=" + (uri.scheme ?: "unknown") + " autoplay=" + autoplay)
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
            Log.e(TAG, "Failed to start media scheme=" + (uri.scheme ?: "unknown"), exception)
            publishState(
                PlaybackSnapshot(
                    status = PlaybackStatus.ERROR,
                    errorMessage = exception.localizedMessage
                        ?: "Could not start playback (" + exception.javaClass.simpleName + ").",
                ),
            )
            throw exception
        }
    }

    @Synchronized
    override fun play() {
        check(!released) { "PlaybackEngine has already been released" }
        try {
            Log.d(TAG, "Play requested; state=" + currentState.status)
            mediaPlayer.play()
        } catch (exception: Exception) {
            Log.e(TAG, "Play request failed", exception)
            publishState(
                PlaybackSnapshot(
                    status = PlaybackStatus.ERROR,
                    errorMessage = exception.localizedMessage
                        ?: "Could not resume playback (" + exception.javaClass.simpleName + ").",
                ),
            )
            throw exception
        }
    }

    @Synchronized
    override fun pause() {
        if (!released) {
            Log.d(TAG, "Pause requested")
            mediaPlayer.pause()
        }
    }

    @Synchronized
    override fun stop() {
        if (released) return
        Log.d(TAG, "Stop requested")
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
        Log.d(TAG, "Releasing LibVLC playback resources")
        mediaPlayer.setEventListener(null)
        mediaPlayer.stop()
        if (attachedVideoLayout != null) mediaPlayer.detachViews()
        attachedVideoLayout = null
        currentUri = null
        stateListeners.clear()
        mediaPlayer.release()
        libVlc.release()
    }

    private companion object {
        const val TAG = "APMediaPlayback"
    }
}
