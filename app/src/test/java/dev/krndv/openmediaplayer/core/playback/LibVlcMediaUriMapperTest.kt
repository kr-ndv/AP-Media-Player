package dev.krndv.openmediaplayer.core.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class LibVlcMediaUriMapperTest {
    @Test
    fun contentUriMapsToDescriptorLocation() {
        assertEquals(
            "fd://42",
            LibVlcMediaUriMapper.toMediaLocation(
                source = "content://media/external/video/media/123",
                fileDescriptorId = 42,
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun contentUriRequiresOpenedDescriptor() {
        LibVlcMediaUriMapper.toMediaLocation(
            source = "content://example.provider/document/1",
            fileDescriptorId = null,
        )
    }

    @Test
    fun fileUriRemainsUnchanged() {
        val source = "file:///storage/emulated/0/Movies/test.mp4"

        assertEquals(source, LibVlcMediaUriMapper.toMediaLocation(source, fileDescriptorId = null))
    }

    @Test
    fun nonContentUriRemainsUnchanged() {
        val source = "https://example.invalid/sample.mp4"

        assertEquals(source, LibVlcMediaUriMapper.toMediaLocation(source, fileDescriptorId = null))
    }
}
