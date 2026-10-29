package dev.krndv.openmediaplayer.core.playback

/**
 * Maps source locations to the MRL passed to LibVLC.
 *
 * Android content-provider URIs must first be opened with ContentResolver and represented by
 * a retained file descriptor. Keeping this mapper in plain Kotlin makes the policy testable
 * by the local JVM test runner without requiring a framework runtime.
 */
internal object LibVlcMediaUriMapper {
    fun toMediaLocation(source: String, fileDescriptorId: Int?): String {
        val scheme = source.substringBefore(':', missingDelimiterValue = "")
        if (!scheme.equals("content", ignoreCase = true)) return source

        val descriptorId = requireNotNull(fileDescriptorId) {
            "A content URI must be opened through ContentResolver before passing it to LibVLC."
        }
        return "fd://$descriptorId"
    }
}
