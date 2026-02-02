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
import kotlinx.serialization.Serializable
import java.util.UUID
import java.security.MessageDigest
import android.os.Build
import android.content.pm.PackageManager

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
            store.copy(recents = updated.take(PLAYLIST_MAX_RECENTS))
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
        writeStore(updated)
    }

    private suspend fun writeStore(store: PlaylistStore) {
        context.playlistDataStore.edit { prefs ->
            prefs[storeKey] = json.encodeToString(PlaylistStore.serializer(), store)
        }
    }

    suspend fun exportPlaylists(): PlaylistExportResult {
        val store = storeFlow.first()
        val payload = PlaylistExportPayload.fromStore(store)
        val canonicalPayload = payload.canonicalized()
        val payloadJson = json.encodeToString(PlaylistExportPayload.serializer(), canonicalPayload)
        val checksum = sha256(payloadJson)
        val export = PlaylistExportFile(
            schemaVersion = PLAYLIST_EXPORT_SCHEMA_VERSION,
            appVersion = resolveAppVersion(),
            exportedAt = System.currentTimeMillis(),
            payload = payload,
            checksum = checksum
        )
        val exportJson = json.encodeToString(PlaylistExportFile.serializer(), export)
        return PlaylistExportResult(
            json = exportJson,
            summary = PlaylistExportSummary.fromStore(store)
        )
    }

    suspend fun importPlaylists(payload: String, mode: PlaylistImportMode): PlaylistImportResult {
        val trimmed = payload.trim()
        if (trimmed.isBlank()) {
            return PlaylistImportResult(false, "Import failed: file is empty.")
        }

        val parsedExport = runCatching {
            json.decodeFromString(PlaylistExportFile.serializer(), trimmed)
        }.getOrNull()

        val exportPayload = when {
            parsedExport != null -> {
                if (parsedExport.schemaVersion > PLAYLIST_EXPORT_SCHEMA_VERSION) {
                    return PlaylistImportResult(
                        false,
                        "Import failed: unsupported export version ${parsedExport.schemaVersion}."
                    )
                }
                parsedExport.checksum?.let { expected ->
                    val canonicalPayload = parsedExport.payload.canonicalized()
                    val payloadJson = json.encodeToString(PlaylistExportPayload.serializer(), canonicalPayload)
                    val actual = sha256(payloadJson)
                    if (!actual.equals(expected, ignoreCase = true)) {
                        return PlaylistImportResult(false, "Import failed: file checksum mismatch.")
                    }
                }
                parsedExport.payload
            }
            else -> {
                val legacyStore = runCatching {
                    json.decodeFromString(PlaylistStore.serializer(), trimmed)
                }.getOrNull() ?: return PlaylistImportResult(
                    false,
                    "Import failed: unrecognized playlist file format."
                )
                PlaylistExportPayload.fromStore(legacyStore)
            }
        }

        validateExportPayload(exportPayload)?.let { error ->
            return PlaylistImportResult(false, "Import failed: $error")
        }

        val incoming = exportPayload.toStore().normalizedForImport()
        val updated = when (mode) {
            PlaylistImportMode.Replace -> incoming
            PlaylistImportMode.Merge -> mergeStores(storeFlow.first(), incoming)
        }.withEnsuredCustomStations()

        writeStore(updated)
        return PlaylistImportResult(
            success = true,
            message = when (mode) {
                PlaylistImportMode.Replace -> "Playlists replaced successfully."
                PlaylistImportMode.Merge -> "Playlists merged successfully."
            },
            summary = PlaylistExportSummary.fromStore(updated)
        )
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

    private fun resolveAppVersion(): String {
        return runCatching {
            val pm = context.packageManager
            val pkg = context.packageName
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
            info.versionName
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "0.0.0"
    }

    companion object {
        private const val MAX_RECENTS = PLAYLIST_MAX_RECENTS
    }
}

enum class PlaylistImportMode {
    Replace,
    Merge
}

data class PlaylistExportResult(
    val json: String,
    val summary: PlaylistExportSummary
)

data class PlaylistImportResult(
    val success: Boolean,
    val message: String,
    val summary: PlaylistExportSummary? = null
)

data class PlaylistExportSummary(
    val playlistCount: Int,
    val playlistStationCount: Int,
    val favoritesCount: Int,
    val recentsCount: Int,
    val customCount: Int
) {
    companion object {
        fun fromStore(store: PlaylistStore): PlaylistExportSummary {
            return PlaylistExportSummary(
                playlistCount = store.playlists.size,
                playlistStationCount = store.playlistStations.values.sumOf { it.size },
                favoritesCount = store.favorites.size,
                recentsCount = store.recents.size,
                customCount = store.custom.size
            )
        }
    }
}

@Serializable
data class PlaylistExportFile(
    val schemaVersion: Int = PLAYLIST_EXPORT_SCHEMA_VERSION,
    val appVersion: String = "",
    val exportedAt: Long = 0L,
    val payload: PlaylistExportPayload = PlaylistExportPayload(),
    val checksum: String? = null
)

@Serializable
data class PlaylistExportPayload(
    val playlists: List<Playlist> = emptyList(),
    val playlistStations: Map<String, List<Station>> = emptyMap(),
    val favorites: List<Station> = emptyList(),
    val recents: List<Station> = emptyList(),
    val custom: List<Station> = emptyList()
) {
    fun toStore(): PlaylistStore {
        return PlaylistStore(
            playlists = playlists,
            playlistStations = playlistStations,
            favorites = favorites,
            recents = recents,
            custom = custom
        )
    }

    fun canonicalized(): PlaylistExportPayload {
        val sorted = playlistStations.toSortedMap()
        return copy(playlistStations = LinkedHashMap(sorted))
    }

    companion object {
        fun fromStore(store: PlaylistStore): PlaylistExportPayload {
            return PlaylistExportPayload(
                playlists = store.playlists,
                playlistStations = store.playlistStations,
                favorites = store.favorites,
                recents = store.recents,
                custom = store.custom
            )
        }
    }
}

private const val PLAYLIST_EXPORT_SCHEMA_VERSION = 1

private fun PlaylistStore.normalizedForImport(): PlaylistStore {
    val playlistIds = playlists.map { it.id }.toSet()
    val cleanedStations = playlistStations
        .filterKeys { it in playlistIds }
        .mapValues { (_, stations) -> dedupeStations(stations) }
    return copy(
        favorites = dedupeStations(favorites),
        recents = dedupeStations(recents).take(PLAYLIST_MAX_RECENTS),
        custom = dedupeStations(custom, filterInvalidCustom = true),
        playlistStations = cleanedStations
    )
}

private fun PlaylistStore.withEnsuredCustomStations(): PlaylistStore {
    val customStations = buildList {
        addAll(custom)
        favorites.filterTo(this, ::isCustomStation)
        recents.filterTo(this, ::isCustomStation)
        playlistStations.values.flatten().filterTo(this, ::isCustomStation)
    }
    val mergedCustom = dedupeStations(customStations, filterInvalidCustom = true)
    return copy(custom = mergedCustom)
}

private fun mergeStores(current: PlaylistStore, incoming: PlaylistStore): PlaylistStore {
    val existingPlaylists = current.playlists.toMutableList()
    val existingByName = existingPlaylists.associateBy { normalizePlaylistName(it.name) }
    val usedIds = existingPlaylists.map { it.id }.toMutableSet()
    val idMapping = mutableMapOf<String, String>()
    var mergedPlaylists = existingPlaylists

    incoming.playlists.forEach { playlist ->
        val normalized = normalizePlaylistName(playlist.name)
        val existing = existingByName[normalized]
        if (existing != null) {
            idMapping[playlist.id] = existing.id
        } else {
            val newId = if (playlist.id !in usedIds) playlist.id else UUID.randomUUID().toString()
            usedIds.add(newId)
            mergedPlaylists.add(playlist.copy(id = newId))
            idMapping[playlist.id] = newId
        }
    }

    val mergedStations = current.playlistStations.toMutableMap()
    incoming.playlistStations.forEach { (incomingId, stations) ->
        val targetId = idMapping[incomingId] ?: incomingId
        val existing = mergedStations[targetId].orEmpty()
        mergedStations[targetId] = mergeStations(existing, stations)
    }

    val mergedFavorites = mergeStations(current.favorites, incoming.favorites)
    val mergedRecents = mergeStations(current.recents, incoming.recents).take(PLAYLIST_MAX_RECENTS)
    val mergedCustom = mergeStations(current.custom, incoming.custom, filterInvalidCustom = true)

    return PlaylistStore(
        playlists = mergedPlaylists,
        playlistStations = mergedStations,
        favorites = mergedFavorites,
        recents = mergedRecents,
        custom = mergedCustom
    )
}

private fun validateExportPayload(payload: PlaylistExportPayload): String? {
    if (payload.playlists.isEmpty() &&
        payload.favorites.isEmpty() &&
        payload.recents.isEmpty() &&
        payload.custom.isEmpty() &&
        payload.playlistStations.isEmpty()
    ) {
        return "no playlists or stations found"
    }

    val normalizedNames = mutableSetOf<String>()
    val ids = mutableSetOf<String>()
    payload.playlists.forEach { playlist ->
        if (playlist.id.isBlank()) return "playlist id is missing"
        if (!ids.add(playlist.id)) return "duplicate playlist id: ${playlist.id}"
        if (playlist.name.isBlank()) return "playlist name is missing"
        val normalized = normalizePlaylistName(playlist.name)
        if (normalized in SystemPlaylistNames.normalized) {
            return "playlist name is reserved: ${playlist.name}"
        }
        if (!normalizedNames.add(normalized)) {
            return "duplicate playlist name: ${playlist.name}"
        }
    }

    val playlistIds = payload.playlists.map { it.id }.toSet()
    payload.playlistStations.keys.forEach { key ->
        if (key !in playlistIds) {
            return "playlist stations reference unknown playlist id: $key"
        }
    }

    fun validateStations(stations: List<Station>, label: String): String? {
        stations.forEach { station ->
            val url = stationUrl(station)
            val isCustom = isCustomStation(station)
            if (isCustom && url.isBlank()) {
                return "$label contains a custom station with no URL"
            }
            if (!isCustom && station.stationuuid.isBlank() && url.isBlank()) {
                return "$label contains a station missing id and url"
            }
        }
        return null
    }

    validateStations(payload.favorites, "Favorites")?.let { return it }
    validateStations(payload.recents, "Recents")?.let { return it }
    validateStations(payload.custom, "Custom")?.let { return it }
    payload.playlistStations.forEach { (playlistId, stations) ->
        validateStations(stations, "Playlist $playlistId")?.let { return it }
    }

    return null
}

private fun dedupeStations(
    stations: List<Station>,
    filterInvalidCustom: Boolean = false
): List<Station> {
    val result = LinkedHashMap<String, Station>()
    stations.forEach { station ->
        if (filterInvalidCustom && isCustomStation(station) && stationUrl(station).isBlank()) {
            return@forEach
        }
        val key = stationKey(station)
        if (!result.containsKey(key)) {
            result[key] = station
        }
    }
    return result.values.toList()
}

private fun mergeStations(
    existing: List<Station>,
    incoming: List<Station>,
    filterInvalidCustom: Boolean = false
): List<Station> {
    val result = LinkedHashMap<String, Station>()
    existing.forEach { station ->
        if (filterInvalidCustom && isCustomStation(station) && stationUrl(station).isBlank()) return@forEach
        result[stationKey(station)] = station
    }
    incoming.forEach { station ->
        if (filterInvalidCustom && isCustomStation(station) && stationUrl(station).isBlank()) return@forEach
        val key = stationKey(station)
        if (!result.containsKey(key)) {
            result[key] = station
        }
    }
    return result.values.toList()
}

private fun stationKey(station: Station): String {
    val url = stationUrl(station)
    return if (station.stationuuid.startsWith("custom_") && url.isNotBlank()) {
        "custom:$url"
    } else if (station.stationuuid.isBlank() && url.isNotBlank()) {
        "custom:$url"
    } else {
        "uuid:${station.stationuuid}"
    }
}

private fun stationUrl(station: Station): String {
    return (station.url_resolved ?: station.url_https ?: station.url ?: "").trim()
}

private fun isCustomStation(station: Station): Boolean {
    val url = stationUrl(station)
    return station.stationuuid.startsWith("custom_") || (station.stationuuid.isBlank() && url.isNotBlank())
}

private fun sha256(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

private const val PLAYLIST_MAX_RECENTS = 50
