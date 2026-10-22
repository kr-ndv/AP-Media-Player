package dev.krndv.openmediaplayer.core.playback

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test

class LibVlcMediaUriMapperTest {
    @Test
    fun contentUriMapsToDescriptorLocation() {
        val uri = Uri.parse("content://media/external/video/media/123")

        assertEquals(
            Uri.parse("fd://42"),
            LibVlcMediaUriMapper.toMediaUri(uri, fileDescriptorId = 42),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun contentUriRequiresOpenedDescriptor() {
        LibVlcMediaUriMapper.toMediaUri(
            Uri.parse("content://example.provider/document/1"),
            fileDescriptorId = null,
        )
    }

    @Test
    fun fileUriRemainsUnchanged() {
        val uri = Uri.parse("file:///storage/emulated/0/Movies/test.mp4")

        assertEquals(uri, LibVlcMediaUriMapper.toMediaUri(uri, fileDescriptorId = null))
    }

    @Test
    fun nonContentUriRemainsUnchanged() {
        val uri = Uri.parse("https://example.invalid/sample.mp4")

        assertEquals(uri, LibVlcMediaUriMapper.toMediaUri(uri, fileDescriptorId = null))
    }
}
