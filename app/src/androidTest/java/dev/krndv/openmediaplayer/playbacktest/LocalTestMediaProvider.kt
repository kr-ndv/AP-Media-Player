package dev.krndv.openmediaplayer.playbacktest

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.PI
import kotlin.math.sin

/**
 * Test-only provider serving a generated PCM WAV. This verifies real ContentResolver descriptor
 * handling without bundling copyrighted or personal audio into the test APK.
 */
class LocalTestMediaProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "audio/wav"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Test provider is read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Test provider is read-only")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("Test provider is read-only")

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (uri.lastPathSegment != WAV_NAME || !mode.startsWith("r")) {
            throw FileNotFoundException("Unknown test media item")
        }
        return ParcelFileDescriptor.open(createWaveFile(), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun createWaveFile(): File {
        val ctx = context ?: throw FileNotFoundException("Test provider has no context")
        val target = File(ctx.cacheDir, WAV_NAME)
        if (target.exists() && target.length() == WAV_SIZE_BYTES) return target

        val sampleRate = SAMPLE_RATE
        val sampleCount = sampleRate * DURATION_SECONDS
        val dataSize = sampleCount * 2 // 16-bit mono PCM
        val pcm = ByteArray(dataSize)
        for (sampleIndex in 0 until sampleCount) {
            val sample = (sin(2.0 * PI * TONE_HZ * sampleIndex / sampleRate) * AMPLITUDE).toInt()
            val byteIndex = sampleIndex * 2
            pcm[byteIndex] = (sample and 0xff).toByte()
            pcm[byteIndex + 1] = ((sample shr 8) and 0xff).toByte()
        }

        FileOutputStream(target).use { output ->
            output.writeAscii("RIFF")
            output.writeIntLittleEndian(36 + dataSize)
            output.writeAscii("WAVE")
            output.writeAscii("fmt ")
            output.writeIntLittleEndian(16)
            output.writeShortLittleEndian(1) // PCM
            output.writeShortLittleEndian(1) // mono
            output.writeIntLittleEndian(sampleRate)
            output.writeIntLittleEndian(sampleRate * 2)
            output.writeShortLittleEndian(2) // block align
            output.writeShortLittleEndian(16)
            output.writeAscii("data")
            output.writeIntLittleEndian(dataSize)
            output.write(pcm)
            output.flush()
        }
        return target
    }

    private fun OutputStream.writeAscii(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun OutputStream.writeShortLittleEndian(value: Int) {
        write(value and 0xff)
        write((value shr 8) and 0xff)
    }

    private fun OutputStream.writeIntLittleEndian(value: Int) {
        write(value and 0xff)
        write((value shr 8) and 0xff)
        write((value shr 16) and 0xff)
        write((value shr 24) and 0xff)
    }

    private companion object {
        const val WAV_NAME = "local-playback-test-tone.wav"
        const val SAMPLE_RATE = 44_100
        const val DURATION_SECONDS = 8
        const val TONE_HZ = 440
        const val AMPLITUDE = 12_000
        const val WAV_SIZE_BYTES = 44L + SAMPLE_RATE.toLong() * DURATION_SECONDS * 2L
    }
}
