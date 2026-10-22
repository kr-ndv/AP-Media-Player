package dev.krndv.openmediaplayer.feature.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

enum class MediaKind {
    AUDIO,
    VIDEO,
}

data class LocalMediaItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String?,
    val album: String?,
    val mimeType: String?,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    val kind: MediaKind,
)

/**
 * Reads media indexed by Android's MediaStore. It never crawls arbitrary folders or uploads
 * metadata; only collections for which the caller has permission are queried.
 */
class MediaRepository(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun loadMedia(includeAudio: Boolean, includeVideo: Boolean): List<LocalMediaItem> {
        if (!includeAudio && !includeVideo) return emptyList()

        return buildList {
            if (includeAudio) addAll(queryAudio())
            if (includeVideo) addAll(queryVideo())
        }.sortedByDescending(LocalMediaItem::dateAddedSeconds)
    }

    private fun queryAudio(): List<LocalMediaItem> = queryCollection(
        collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        columns = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
        ),
    ) { cursor, id ->
        LocalMediaItem(
            id = id,
            uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
            title = cursor.getStringOrFallback(
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
            ),
            artist = cursor.getNullableString(MediaStore.Audio.Media.ARTIST)
                ?.takeUnless { it.equals("<unknown>", ignoreCase = true) },
            album = cursor.getNullableString(MediaStore.Audio.Media.ALBUM)
                ?.takeUnless { it.equals("<unknown>", ignoreCase = true) },
            mimeType = cursor.getNullableString(MediaStore.Audio.Media.MIME_TYPE),
            durationMs = cursor.getLongOrZero(MediaStore.Audio.Media.DURATION),
            sizeBytes = cursor.getLongOrZero(MediaStore.Audio.Media.SIZE),
            dateAddedSeconds = cursor.getLongOrZero(MediaStore.Audio.Media.DATE_ADDED),
            kind = MediaKind.AUDIO,
        )
    }

    private fun queryVideo(): List<LocalMediaItem> = queryCollection(
        collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        columns = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
        ),
    ) { cursor, id ->
        LocalMediaItem(
            id = id,
            uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
            title = cursor.getStringOrFallback(
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DISPLAY_NAME,
            ),
            artist = null,
            album = null,
            mimeType = cursor.getNullableString(MediaStore.Video.Media.MIME_TYPE),
            durationMs = cursor.getLongOrZero(MediaStore.Video.Media.DURATION),
            sizeBytes = cursor.getLongOrZero(MediaStore.Video.Media.SIZE),
            dateAddedSeconds = cursor.getLongOrZero(MediaStore.Video.Media.DATE_ADDED),
            kind = MediaKind.VIDEO,
        )
    }

    private inline fun queryCollection(
        collection: Uri,
        columns: Array<String>,
        crossinline mapRow: android.database.Cursor.(Long) -> LocalMediaItem,
    ): List<LocalMediaItem> = runCatching {
        val results = mutableListOf<LocalMediaItem>()
        resolver.query(
            collection,
            columns,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                results += cursor.mapRow(id)
            }
        }
        results
    }.getOrDefault(emptyList())

    private fun android.database.Cursor.getNullableString(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index)?.trim()?.takeIf(String::isNotEmpty) else null
    }

    private fun android.database.Cursor.getStringOrFallback(primary: String, fallback: String): String {
        val first = getNullableString(primary)
        val second = getNullableString(fallback)
        return first ?: second ?: "Untitled media"
    }

    private fun android.database.Cursor.getLongOrZero(column: String): Long {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getLong(index) else 0L
    }
}
