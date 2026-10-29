package dev.krndv.openmediaplayer.playbacktest

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.krndv.openmediaplayer.core.playback.LibVlcPlaybackEngine
import dev.krndv.openmediaplayer.core.playback.PlaybackSnapshot
import dev.krndv.openmediaplayer.core.playback.PlaybackStateListener
import dev.krndv.openmediaplayer.core.playback.PlaybackStatus
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalContentUriPlaybackTest {
    @Test
    fun opensGeneratedLocalAudioFromContentUri() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = Uri.parse("content://dev.krndv.openmediaplayer.testmedia/local-playback-test-tone.wav")
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
        assertNotNull("The test provider must expose a readable local media descriptor", descriptor)
        descriptor?.close()

        val engine = LibVlcPlaybackEngine(context)
        val sawPlaying = AtomicBoolean(false)
        val failureMessage = AtomicReference<String?>(null)
        val reachedResult = CountDownLatch(1)
        val observed = java.util.concurrent.CopyOnWriteArrayList<PlaybackSnapshot>()
        val listener = PlaybackStateListener { snapshot ->
            observed += snapshot
            when (snapshot.status) {
                PlaybackStatus.PLAYING -> {
                    sawPlaying.set(true)
                    reachedResult.countDown()
                }
                PlaybackStatus.ERROR -> {
                    failureMessage.set(snapshot.errorMessage ?: "LibVLC reported EncounteredError")
                    reachedResult.countDown()
                }
                else -> Unit
            }
        }

        engine.addPlaybackStateListener(listener)
        try {
            engine.open(uri, autoplay = true)
            assertTrue(
                "Timed out waiting for local audio playback; observed=${observed.map { it.status }}",
                reachedResult.await(15, TimeUnit.SECONDS),
            )
            assertNull(
                "LibVLC reported a playback error: ${failureMessage.get()}",
                failureMessage.get(),
            )
            assertTrue("LibVLC never reported Playing", sawPlaying.get())
        } finally {
            engine.removePlaybackStateListener(listener)
            runCatching { engine.stop() }
            engine.release()
        }
    }
}
