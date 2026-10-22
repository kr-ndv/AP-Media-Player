package dev.krndv.openmediaplayer.core.playback

import android.content.ContentResolver
import android.net.Uri

/**
 * LibVLC consumes local file descriptors through an fd:// MRL rather than using Android's
 * ContentResolver itself. The engine must keep the corresponding descriptor open.
 */
internal object LibVlcMediaUriMapper {
    fun toMediaUri(uri: Uri, fileDescriptorId: Int?): Uri {
        if (!uri.scheme.equals(ContentResolver.SCHEME_CONTENT, ignoreCase = true)) {
            return uri
        }

        val descriptorId = requireNotNull(fileDescriptorId) {
            "A content URI must be opened through ContentResolver before passing it to LibVLC."
        }
        return Uri.parse("fd://$descriptorId")
    }
}
