package dev.krndv.openmediaplayer.core.playback

/**
 * Converts Android content-provider URIs to a LibVLC file-descriptor media location.
 * Other URI schemes are passed through unchanged.
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
