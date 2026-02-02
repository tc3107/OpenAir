package com.example.openair.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.openair.data.model.Playlist
import com.example.openair.data.model.PlaylistStore
import com.example.openair.data.model.Station
import com.example.openair.data.model.SystemPlaylistNames
import com.example.openair.data.model.normalizePlaylistName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.playlistDataStore by preferencesDataStore("openair_playlists")

class PlaylistRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val storeKey = stringPreferencesKey("playlist_store_json")

    val storeFlow: Flow<PlaylistStore> = context.playlistDataStore.data.map { prefs ->
        val payload = prefs[storeKey]
        if (payload.isNullOrBlank()) {
            PlaylistStore()
        } else {
            runCatching { json.decodeFromString(PlaylistStore.serializer(), payload) }
                .getOrElse { PlaylistStore() }
        }
    }

    suspend fun toggleFavorite(station: Station) {
        updateStore { store ->
            val existing = store.favorites
            val hasStation = existing.any { it.stationuuid == station.stationuuid }
            val updated = if (hasStation) {
                existing.filterNot { it.stationuuid == station.stationuuid }
            } else {
                listOf(station) + existing.filterNot { it.stationuuid == station.stationuuid }
            }
            store.copy(favorites = updated)
        }
    }

    suspend fun toggleStationInPlaylist(playlistId: String, station: Station) {
        updateStore { store ->
            val current = store.playlistStations[playlistId].orEmpty()
            val hasStation = current.any { it.stationuuid == station.stationuuid }
            val updatedList = if (hasStation) {
                current.filterNot { it.stationuuid == station.stationuuid }
            } else {
                listOf(station) + current.filterNot { it.stationuuid == station.stationuuid }
            }
            store.withPlaylistStations(playlistId, updatedList)
        }
    }

    suspend fun addToRecents(station: Station) {
        updateStore { store ->
            val updated = listOf(station) + store.recents.filterNot { it.stationuuid == station.stationuuid }
            store.copy(recents = updated.take(MAX_RECENTS))
        }
    }

    suspend fun addCustomStation(station: Station) {
        updateStore { store ->
            val url = station.url_resolved ?: station.url_https ?: station.url ?: ""
            if (station.name.isBlank() || url.isBlank()) return@updateStore store
            val updated = listOf(station) + store.custom.filterNot { existing ->
                val existingUrl = existing.url_resolved ?: existing.url_https ?: existing.url ?: ""
                existingUrl == url
            }
            store.copy(custom = updated)
        }
    }

    suspend fun createPlaylist(name: String): Playlist? {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return null
        var created: Playlist? = null
        updateStore { store ->
            if (store.isNameTaken(trimmed)) {
                return@updateStore store
            }
            val playlist = Playlist(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                createdAt = System.currentTimeMillis()
            )
            created = playlist
            store.copy(playlists = store.playlists + playlist)
        }
        return created
    }

    suspend fun renamePlaylist(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        updateStore { store ->
            if (store.isNameTaken(trimmed, ignoreId = id)) {
                return@updateStore store
            }
            val updated = store.playlists.map { playlist ->
                if (playlist.id == id) {
                    playlist.copy(name = trimmed, updatedAt = System.currentTimeMillis())
                } else {
                    playlist
                }
            }
            store.copy(playlists = updated)
        }
    }

    suspend fun deletePlaylist(id: String) {
        updateStore { store ->
            val updatedPlaylists = store.playlists.filterNot { it.id == id }
            val updatedStations = store.playlistStations.toMutableMap().apply { remove(id) }
            store.copy(playlists = updatedPlaylists, playlistStations = updatedStations)
        }
    }

    suspend fun movePlaylist(fromIndex: Int, toIndex: Int) {
        updateStore { store ->
            if (fromIndex !in store.playlists.indices || toIndex !in store.playlists.indices) {
                return@updateStore store
            }
            val mutable = store.playlists.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            store.copy(playlists = mutable)
        }
    }

    private suspend fun updateStore(transform: (PlaylistStore) -> PlaylistStore) {
        val current = storeFlow.first()
        val updated = transform(current)
        context.playlistDataStore.edit { prefs ->
            prefs[storeKey] = json.encodeToString(PlaylistStore.serializer(), updated)
        }
    }

    private fun PlaylistStore.withPlaylistStations(id: String, stations: List<Station>): PlaylistStore {
        val updated = playlistStations.toMutableMap()
        if (stations.isEmpty()) {
            updated.remove(id)
        } else {
            updated[id] = stations
        }
        return copy(playlistStations = updated)
    }

    private fun PlaylistStore.isNameTaken(name: String, ignoreId: String? = null): Boolean {
        val normalized = normalizePlaylistName(name)
        if (normalized in SystemPlaylistNames.normalized) return true
        return playlists.any { playlist ->
            playlist.id != ignoreId && normalizePlaylistName(playlist.name) == normalized
        }
    }

    companion object {
        private const val MAX_RECENTS = 50
    }
}
