package dev.krndv.openmediaplayer.core.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LibVlcMediaUriMapperTest {
    @Test
    fun contentUriMapsToFileDescriptorLocation() {
        assertEquals("fd://42", LibVlcMediaUriMapper.toMediaLocation("content://media/external/audio/1", 42))
    }

    @Test
    fun networkUriRemainsUnchanged() {
        assertEquals("https://example.com/audio.mp3", LibVlcMediaUriMapper.toMediaLocation("https://example.com/audio.mp3", null))
    }

    @Test
    fun fileUriRemainsUnchanged() {
        assertEquals("file:///storage/emulated/0/Music/song.mp3", LibVlcMediaUriMapper.toMediaLocation("file:///storage/emulated/0/Music/song.mp3", null))
    }

    @Test
    fun contentUriWithoutDescriptorIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LibVlcMediaUriMapper.toMediaLocation("content://media/external/video/1", null)
        }
    }
}
