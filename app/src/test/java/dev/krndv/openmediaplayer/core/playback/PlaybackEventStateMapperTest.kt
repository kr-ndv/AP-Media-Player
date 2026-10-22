package dev.krndv.openmediaplayer.core.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackEventStateMapperTest {
    @Test
    fun openingEventMapsToOpeningSnapshot() {
        assertEquals(
            PlaybackSnapshot(PlaybackStatus.OPENING),
            PlaybackEventStateMapper.toSnapshot(NativePlaybackEvent.OPENING),
        )
    }

    @Test
    fun playingEventMapsToPlayingSnapshot() {
        val snapshot = PlaybackEventStateMapper.toSnapshot(NativePlaybackEvent.PLAYING)

        assertEquals(PlaybackSnapshot(PlaybackStatus.PLAYING), snapshot)
        assertTrue(snapshot?.isPlaying == true)
    }

    @Test
    fun pausedEventMapsToPausedSnapshot() {
        assertEquals(
            PlaybackSnapshot(PlaybackStatus.PAUSED),
            PlaybackEventStateMapper.toSnapshot(NativePlaybackEvent.PAUSED),
        )
    }

    @Test
    fun stoppedEventDoesNotOverrideExplicitCommandState() {
        assertNull(PlaybackEventStateMapper.toSnapshot(NativePlaybackEvent.STOPPED))
    }

    @Test
    fun endReachedMapsToEndedSnapshot() {
        assertEquals(
            PlaybackSnapshot(PlaybackStatus.ENDED),
            PlaybackEventStateMapper.toSnapshot(NativePlaybackEvent.END_REACHED),
        )
    }

    @Test
    fun nativeErrorMapsToErrorSnapshot() {
        val snapshot = PlaybackEventStateMapper.toSnapshot(NativePlaybackEvent.ENCOUNTERED_ERROR)

        assertEquals(PlaybackStatus.ERROR, snapshot?.status)
        assertFalse(snapshot?.isPlaying == true)
    }
}
