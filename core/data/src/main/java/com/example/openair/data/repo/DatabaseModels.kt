package com.example.openair.data.repo

data class DatabaseSummary(
    val stationCount: Int,
    val countryCount: Int,
    val tagCount: Int,
    val languageCount: Int,
    val lastUpdated: Long,
    val lastDurationMillis: Long,
    val lastError: String?
)

enum class DatabaseStatus {
    Ready,
    Empty,
    Error,
    Building
}

data class DatabaseRebuildProgress(
    val phase: DatabaseRebuildPhase,
    val message: String,
    val stationCount: Int = 0,
    val page: Int = 0
)

enum class DatabaseRebuildPhase {
    Starting,
    FetchingMetadata,
    DownloadingStations,
    Finalizing
}

sealed class DatabaseRebuildState {
    data object Idle : DatabaseRebuildState()
    data class Running(val progress: DatabaseRebuildProgress) : DatabaseRebuildState()
    data class Success(val summary: DatabaseSummary) : DatabaseRebuildState()
    data class Error(val message: String) : DatabaseRebuildState()
}
