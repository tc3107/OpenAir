package com.example.openair.data.repo

import kotlinx.serialization.Serializable

@Serializable
enum class ScreenType {
    Browse,
    Playlists,
    NearMe,
    Config,
    StationList,
    PlaylistStations
}

@Serializable
enum class PlaylistRefType {
    Favorites,
    Recents,
    Custom,
    User
}

@Serializable
data class NavigationState(
    val screen: ScreenType = ScreenType.Browse,
    val playlistRefType: PlaylistRefType? = null,
    val playlistId: String? = null,
    val playlistName: String? = null,
    val stationListTitle: String? = null,
    val stationFilters: SearchFilters? = null
)
