package dev.krndv.openmediaplayer.core.playback

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet

/**
 * LibVLC adapter for the first playback prototype.
 *
 * The engine owns native playback resources. The player screen supplies a video
 * layout while visible; callers must release the engine when its owner is destroyed.
 */
class LibVlcPlaybackEngine(context: Context) : PlaybackEngine {
    private val appContext = context.applicationContext
    private val libVlc = LibVLC(
        appContext,
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
    private var activeSourceDescriptor: ParcelFileDescriptor? = null

    override val playbackState: PlaybackSnapshot
        get() = currentState

    val currentTimeMs: Long
        get() = if (released) 0L else mediaPlayer.time.coerceAtLeast(0L)

    val durationMs: Long
        get() = if (released) 0L else mediaPlayer.length.coerceAtLeast(0L)

    @Synchronized
    fun seekTo(positionMs: Long) {
        if (released || durationMs <= 0L) return
        mediaPlayer.time = positionMs.coerceIn(0L, durationMs)
    }

    @Synchronized
    fun setPlaybackRate(rate: Float) {
        check(!released) { "PlaybackEngine has already been released" }
        require(rate in 0.25f..4.0f) { "Playback rate must be between 0.25x and 4x" }
        mediaPlayer.setRate(rate)
    }

    data class TrackOption(val id: Int, val name: String)

    @Synchronized
    fun audioTracks(): List<TrackOption> =
        if (released) emptyList() else mediaPlayer.audioTracks.orEmpty().map { TrackOption(it.id, it.name ?: "Audio track") }

    @Synchronized
    fun subtitleTracks(): List<TrackOption> =
        if (released) emptyList() else mediaPlayer.spuTracks.orEmpty().map { TrackOption(it.id, it.name ?: "Subtitle") }

    @Synchronized
    fun selectedAudioTrack(): Int = if (released) -1 else mediaPlayer.audioTrack

    @Synchronized
    fun selectedSubtitleTrack(): Int = if (released) -1 else mediaPlayer.spuTrack

    @Synchronized
    fun selectAudioTrack(id: Int): Boolean = !released && mediaPlayer.setAudioTrack(id)

    @Synchronized
    fun selectSubtitleTrack(id: Int): Boolean = !released && mediaPlayer.setSpuTrack(id)

    @Synchronized
    fun addExternalSubtitle(uri: Uri): Boolean =
        !released && mediaPlayer.addSlave(IMedia.Slave.Type.Subtitle, uri, true)

    @Synchronized
    fun setSubtitleDelayMs(delayMs: Long): Boolean =
        !released && mediaPlayer.setSpuDelay(delayMs * 1_000L)

    @Synchronized
    fun setVideoAspectMode(mode: String) {
        if (released) return
        when (mode) {
            "fit", "fill" -> { mediaPlayer.aspectRatio = null; mediaPlayer.scale = 0f }
            "stretch" -> { mediaPlayer.aspectRatio = "16:9"; mediaPlayer.scale = 0f }
            "zoom" -> { mediaPlayer.aspectRatio = null; mediaPlayer.scale = 1.25f }
        }
    }

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

        var candidateDescriptor: ParcelFileDescriptor? = null
        var mediaInstalled = false
        try {
            // LibVLC cannot reliably consume Android content:// URIs directly. Open the URI
            // through Android's ContentResolver and pass LibVLC a retained fd:// location.
            mediaPlayer.stop()
            candidateDescriptor = openContentDescriptorIfNeeded(uri)
            // Use LibVLC's FileDescriptor constructor for Android content URIs. Passing
            // an fd:// location string can lose the original media metadata/extension and
            // depends on the VLC fd access module resolving the descriptor number correctly.
            // Keep the ParcelFileDescriptor alive until this media is replaced or released.
            Log.d(
                TAG,
                "Resolved media scheme=" + (uri.scheme ?: "unknown") +
                    " descriptor=" + (candidateDescriptor != null),
            )

            val media = candidateDescriptor?.let { descriptor ->
                Media(libVlc, descriptor.fileDescriptor)
            } ?: Media(libVlc, uri)
            try {
                media.setHWDecoderEnabled(true, false)
                mediaPlayer.media = media

                // Keep the descriptor open while native playback reads fd://N.
                val previousDescriptor = activeSourceDescriptor
                activeSourceDescriptor = candidateDescriptor
                candidateDescriptor = null
                mediaInstalled = true
                closeDescriptor(previousDescriptor, "replaced")
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
            closeDescriptor(candidateDescriptor, "failed-candidate")
            if (!mediaInstalled) {
                closeDescriptor(activeSourceDescriptor, "failed-open-after-stop")
                activeSourceDescriptor = null
            }
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

    private fun openContentDescriptorIfNeeded(uri: Uri): ParcelFileDescriptor? {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
        return appContext.contentResolver.openFileDescriptor(uri, "r")
            ?: throw FileNotFoundException("Android did not provide a descriptor for this media item.")
    }

    private fun closeDescriptor(descriptor: ParcelFileDescriptor?, reason: String) {
        if (descriptor == null) return
        try {
            descriptor.close()
        } catch (exception: IOException) {
            Log.w(TAG, "Could not close media descriptor: reason=" + reason, exception)
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
        closeDescriptor(activeSourceDescriptor, "release")
        activeSourceDescriptor = null
        libVlc.release()
    }

    private companion object {
        const val TAG = "APMediaPlayback"
    }
}
