package com.example.openair.data.model

import kotlinx.serialization.Serializable
import java.util.Locale

fun normalizePlaylistName(name: String): String = name.trim().lowercase(Locale.ROOT)

object SystemPlaylistNames {
    const val FAVORITES = "Favorites"
    const val RECENTS = "Recents"
    const val CUSTOM = "Custom"

    val all: Set<String> = setOf(FAVORITES, RECENTS, CUSTOM)
    val normalized: Set<String> = all.map(::normalizePlaylistName).toSet()
}

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long = createdAt
)

@Serializable
data class PlaylistStore(
    val playlists: List<Playlist> = emptyList(),
    val playlistStations: Map<String, List<Station>> = emptyMap(),
    val favorites: List<Station> = emptyList(),
    val recents: List<Station> = emptyList(),
    val custom: List<Station> = emptyList()
)
