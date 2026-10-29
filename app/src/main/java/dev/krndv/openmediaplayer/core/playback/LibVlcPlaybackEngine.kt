package dev.krndv.openmediaplayer.core.playback

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet

/**
 * LibVLC adapter for local media playback.
 *
 * Android content-provider URIs are opened through ContentResolver and passed to LibVLC
 * as fd:// locations. The descriptor stays open for as long as the active media may need it.
 * The player screen supplies a video layout while visible; callers must release this engine.
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
    private var playbackAttempt = 0L

    @Volatile
    private var lastSourceScheme: String? = null

    private var activeSourceDescriptor: ParcelFileDescriptor? = null
    private var attachedVideoLayout: VLCVideoLayout? = null

    override val playbackState: PlaybackSnapshot
        get() = currentState

    private val playerEventListener = MediaPlayer.EventListener { event ->
        handleNativeEvent(event.type)
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

    @Synchronized
    override fun addPlaybackStateListener(listener: PlaybackStateListener) {
        check(!released) { "PlaybackEngine has already been released" }
        stateListeners.add(listener)
        val initialSnapshot = currentState
        mainHandler.post {
            if (!released && stateListeners.contains(listener)) {
                listener.onPlaybackStateChanged(initialSnapshot)
            }
        }
    }

    override fun removePlaybackStateListener(listener: PlaybackStateListener) {
        stateListeners.remove(listener)
    }

    @Synchronized
    override fun open(uri: Uri, autoplay: Boolean) {
        check(!released) { "PlaybackEngine has already been released" }

        val attempt = ++playbackAttempt
        lastSourceScheme = uri.scheme
        Log.i(
            TAG,
            "Open requested: attempt=$attempt scheme=${uri.scheme ?: "unknown"} " +
                "autoplay=$autoplay previousState=${currentState.status}",
        )

        var candidateDescriptor: ParcelFileDescriptor? = null
        var mediaInstalled = false
        try {
            if (autoplay) {
                publishState(PlaybackSnapshot(PlaybackStatus.OPENING), "open#$attempt")
            }

            // Stop the old item before resolving a new descriptor. Keep its descriptor open
            // until MediaPlayer has successfully accepted the replacement media.
            mediaPlayer.stop()

            candidateDescriptor = openContentDescriptorIfNeeded(uri)
            val mediaLocation = LibVlcMediaUriMapper.toMediaLocation(
                uri.toString(),
                candidateDescriptor?.fd,
            )
            val libVlcUri = Uri.parse(mediaLocation)
            Log.d(
                TAG,
                "Resolved source: attempt=$attempt originalScheme=${uri.scheme ?: "unknown"} " +
                    "libVlcScheme=${libVlcUri.scheme ?: "unknown"} " +
                    "usesDescriptor=${candidateDescriptor != null}",
            )

            val media = Media(libVlc, libVlcUri)
            try {
                media.setHWDecoderEnabled(true, false)
                mediaPlayer.media = media

                // Transfer ownership only after MediaPlayer accepted the media. Keeping the
                // ParcelFileDescriptor open is required while LibVLC reads fd://N.
                val previousDescriptor = activeSourceDescriptor
                activeSourceDescriptor = candidateDescriptor
                candidateDescriptor = null
                mediaInstalled = true
                closeDescriptor(previousDescriptor, "replaced")
            } finally {
                media.release()
            }

            if (autoplay) {
                mediaPlayer.play()
            } else {
                publishState(PlaybackSnapshot(), "prepared#$attempt")
            }
        } catch (exception: Exception) {
            closeDescriptor(candidateDescriptor, "failed-candidate")
            if (!mediaInstalled) {
                closeDescriptor(activeSourceDescriptor, "failed-open-after-stop")
                activeSourceDescriptor = null
            }
            Log.e(
                TAG,
                "Open failed: attempt=$attempt scheme=${uri.scheme ?: "unknown"} " +
                    "errorType=${exception.javaClass.simpleName}",
            )
            publishState(
                PlaybackSnapshot(
                    status = PlaybackStatus.ERROR,
                    errorMessage = exception.localizedMessage,
                ),
                "open-failed#$attempt",
            )
            throw exception
        }
    }

    @Synchronized
    override fun play() {
        check(!released) { "PlaybackEngine has already been released" }
        val attempt = playbackAttempt
        Log.i(TAG, "Play requested: attempt=$attempt state=${currentState.status}")

        try {
            if (currentState.status !in setOf(PlaybackStatus.PAUSED, PlaybackStatus.PLAYING)) {
                publishState(PlaybackSnapshot(PlaybackStatus.OPENING), "play#$attempt")
            }
            mediaPlayer.play()
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Play failed: attempt=$attempt scheme=${lastSourceScheme ?: "unknown"} " +
                    "errorType=${exception.javaClass.simpleName}",
            )
            publishState(
                PlaybackSnapshot(
                    status = PlaybackStatus.ERROR,
                    errorMessage = exception.localizedMessage,
                ),
                "play-failed#$attempt",
            )
            throw exception
        }
    }

    @Synchronized
    override fun pause() {
        if (released) return
        Log.i(TAG, "Pause requested: attempt=$playbackAttempt")
        mediaPlayer.pause()
    }

    @Synchronized
    override fun stop() {
        if (released) return
        Log.i(TAG, "Stop requested: attempt=$playbackAttempt state=${currentState.status}")
        mediaPlayer.stop()
        publishState(PlaybackSnapshot(PlaybackStatus.STOPPED), "stop#$playbackAttempt")
    }

    private fun openContentDescriptorIfNeeded(uri: Uri): ParcelFileDescriptor? {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
        return appContext.contentResolver.openFileDescriptor(uri, "r")
            ?: throw FileNotFoundException("Android did not provide a descriptor for this media item.")
    }

    private fun handleNativeEvent(eventType: Int) {
        if (released) return

        val event = when (eventType) {
            MediaPlayer.Event.Opening -> NativePlaybackEvent.OPENING
            MediaPlayer.Event.Playing -> NativePlaybackEvent.PLAYING
            MediaPlayer.Event.Paused -> NativePlaybackEvent.PAUSED
            MediaPlayer.Event.Stopped -> NativePlaybackEvent.STOPPED
            MediaPlayer.Event.EndReached -> NativePlaybackEvent.END_REACHED
            MediaPlayer.Event.EncounteredError -> NativePlaybackEvent.ENCOUNTERED_ERROR
            else -> null
        } ?: return

        Log.d(
            TAG,
            "LibVLC event=$event currentAttempt=$playbackAttempt " +
                "state=${currentState.status} sourceScheme=${lastSourceScheme ?: "unknown"}",
        )

        if (event == NativePlaybackEvent.ENCOUNTERED_ERROR) {
            Log.e(
                TAG,
                "LibVLC encountered an error: attempt=$playbackAttempt " +
                    "sourceScheme=${lastSourceScheme ?: "unknown"}",
            )
        }

        val snapshot = PlaybackEventStateMapper.toSnapshot(event)
        if (snapshot == null) {
            Log.d(
                TAG,
                "Ignoring native Stopped event; explicit stop/open commands own this state " +
                    "to prevent an old item's stop event overwriting a replacement item.",
            )
            return
        }

        publishState(snapshot, "native-$event")
    }

    /**
     * Delivers the exact snapshot that caused the update.
     *
     * Do not read currentState inside the posted callback: that can collapse Opening,
     * Playing and Paused into the same latest value when native events arrive quickly.
     */
    @Synchronized
    private fun publishState(snapshot: PlaybackSnapshot, source: String) {
        if (released) return
        currentState = snapshot
        Log.d(TAG, "State -> ${snapshot.status} source=$source attempt=$playbackAttempt")
        val publishedSnapshot = snapshot
        mainHandler.post {
            if (!released) {
                stateListeners.forEach { listener ->
                    if (stateListeners.contains(listener)) {
                        listener.onPlaybackStateChanged(publishedSnapshot)
                    }
                }
            }
        }
    }

    @Synchronized
    override fun release() {
        if (released) return
        released = true
        Log.i(TAG, "Release playback engine: attempt=$playbackAttempt")
        mediaPlayer.setEventListener(null)
        mediaPlayer.stop()
        if (attachedVideoLayout != null) mediaPlayer.detachViews()
        attachedVideoLayout = null
        stateListeners.clear()
        mediaPlayer.release()
        closeDescriptor(activeSourceDescriptor, "release")
        activeSourceDescriptor = null
        libVlc.release()
    }

    private fun closeDescriptor(descriptor: ParcelFileDescriptor?, reason: String) {
        if (descriptor == null) return
        try {
            descriptor.close()
        } catch (exception: IOException) {
            Log.w(
                TAG,
                "Could not close content descriptor: reason=$reason " +
                    "errorType=${exception.javaClass.simpleName}",
            )
        }
    }

    private companion object {
        const val TAG = "OpenMediaPlayerPlayback"
    }
}
