package dev.krndv.openmediaplayer.core.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSnapshotTest {
    @Test
    fun isPlayingIsTrueOnlyForPlayingStatus() {
        assertTrue(PlaybackSnapshot(status = PlaybackStatus.PLAYING).isPlaying)
        assertFalse(PlaybackSnapshot(status = PlaybackStatus.IDLE).isPlaying)
        assertFalse(PlaybackSnapshot(status = PlaybackStatus.OPENING).isPlaying)
        assertFalse(PlaybackSnapshot(status = PlaybackStatus.PAUSED).isPlaying)
        assertFalse(PlaybackSnapshot(status = PlaybackStatus.STOPPED).isPlaying)
        assertFalse(PlaybackSnapshot(status = PlaybackStatus.ENDED).isPlaying)
        assertFalse(PlaybackSnapshot(status = PlaybackStatus.ERROR).isPlaying)
    }

    @Test
    fun errorSnapshotCanCarryActionableMessage() {
        val snapshot = PlaybackSnapshot(
            status = PlaybackStatus.ERROR,
            errorMessage = "LibVLC could not open or decode this media.",
        )

        assertFalse(snapshot.isPlaying)
        assertTrue(snapshot.errorMessage.orEmpty().contains("open or decode"))
    }
}
