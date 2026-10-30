package dev.krndv.openmediaplayer.feature.library

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LocalPlaylist(
    val id: String,
    val name: String,
    val mediaUris: List<String>,
)

/** Stores user-created playlists locally without changing the MediaStore library source. */
class PlaylistRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    fun load(): List<LocalPlaylist> {
        val raw = preferences.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                val tracks = item.optJSONArray("mediaUris") ?: JSONArray()
                LocalPlaylist(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    mediaUris = List(tracks.length()) { trackIndex -> tracks.getString(trackIndex) },
                )
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun create(name: String): LocalPlaylist? {
        val cleanName = name.trim().take(60)
        if (cleanName.isBlank()) return null
        val existing = load()
        if (existing.any { it.name.equals(cleanName, ignoreCase = true) }) return null
        val playlist = LocalPlaylist(UUID.randomUUID().toString(), cleanName, emptyList())
        save(existing + playlist)
        return playlist
    }

    @Synchronized
    fun addTrack(playlistId: String, uri: String): List<LocalPlaylist> {
        val updated = load().map { playlist ->
            if (playlist.id == playlistId && uri !in playlist.mediaUris) {
                playlist.copy(mediaUris = playlist.mediaUris + uri)
            } else playlist
        }
        save(updated)
        return updated
    }

    @Synchronized
    fun removeTrack(playlistId: String, uri: String): List<LocalPlaylist> {
        val updated = load().map { playlist ->
            if (playlist.id == playlistId) playlist.copy(mediaUris = playlist.mediaUris - uri) else playlist
        }
        save(updated)
        return updated
    }

    @Synchronized
    fun delete(playlistId: String): List<LocalPlaylist> {
        val updated = load().filterNot { it.id == playlistId }
        save(updated)
        return updated
    }

    private fun save(playlists: List<LocalPlaylist>) {
        val array = JSONArray()
        playlists.forEach { playlist ->
            val tracks = JSONArray()
            playlist.mediaUris.forEach(tracks::put)
            array.put(JSONObject().put("id", playlist.id).put("name", playlist.name).put("mediaUris", tracks))
        }
        preferences.edit().putString(KEY_PLAYLISTS, array.toString()).apply()
    }

    private companion object {
        const val PREFERENCES = "av_music_player_playlists"
        const val KEY_PLAYLISTS = "playlists"
    }
}
