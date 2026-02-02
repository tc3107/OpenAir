package com.example.openair.player

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.openair.data.model.Station
import com.example.openair.data.repo.AppStateRepository
import com.example.openair.data.repo.PlaylistRepository
import com.example.openair.data.repo.RadioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class PlaybackViewModel(
    private val app: Application,
    private val repository: RadioRepository,
    private val playlistRepository: PlaylistRepository,
    private val appStateRepository: AppStateRepository
) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    private var controller: MediaController? = null
    private val triedUrls = mutableSetOf<String>()
    private var candidateUrls: List<String> = emptyList()
    private var candidateIndex = 0

    private val executor = Executor { command -> command.run() }

    init {
        val token = SessionToken(app, ComponentName(app, RadioPlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            controller = future.get()
            controller?.addListener(playerListener)
        }, executor)
        restoreLastStation()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(state: Int) {
            val status = when (state) {
                Player.STATE_BUFFERING -> "Buffering"
                Player.STATE_READY -> if (controller?.isPlaying == true) "Playing" else "Paused"
                Player.STATE_ENDED -> "Ended"
                else -> "Idle"
            }
            val buffering = state == Player.STATE_BUFFERING
            _state.update { it.copy(status = status, isBuffering = buffering) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(status = "Error: ${error.errorCodeName}", isBuffering = false) }
            retryNextUrl()
        }
    }

    fun playStation(station: Station) {
        viewModelScope.launch {
            appStateRepository.saveLastStation(station)
            playlistRepository.addToRecents(station)
            _state.update { it.copy(status = "Resolving stream...", currentStation = station) }
            triedUrls.clear()
            candidateUrls = if (station.stationuuid.startsWith("custom_")) {
                listOfNotNull(station.url_resolved, station.url_https, station.url)
            } else {
                repository.resolvePlaybackUrls(station.stationuuid)
            }
            candidateIndex = 0
            if (candidateUrls.isEmpty()) {
                _state.update { it.copy(status = "No playable URL") }
                return@launch
            }
            startPlayback(candidateUrls[candidateIndex], station)
        }
    }

    private fun restoreLastStation() {
        viewModelScope.launch {
            val saved = appStateRepository.readLastStation() ?: return@launch
            if (saved.stationuuid.isBlank()) return@launch
            val refreshedResult = runCatching { repository.getStationByUuid(saved.stationuuid) }
            val station = when {
                refreshedResult.isSuccess && refreshedResult.getOrNull() != null ->
                    refreshedResult.getOrNull()
                refreshedResult.isSuccess -> null
                else -> saved
            }
            if (station == null) {
                appStateRepository.saveLastStation(null)
                return@launch
            }
            _state.update { it.copy(currentStation = station, status = "Idle") }
        }
    }

    fun togglePlayback() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun stop() {
        controller?.stop()
    }

    private fun startPlayback(url: String, station: Station) {
        triedUrls.add(url)
        ensureServiceStarted()
        val item = MediaItem.Builder()
            .setMediaId(station.stationuuid)
            .setUri(url)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.country ?: "")
                    .build()
            )
            .build()
        controller?.setMediaItem(item)
        controller?.prepare()
        controller?.play()
    }

    private fun retryNextUrl() {
        val station = _state.value.currentStation ?: return
        viewModelScope.launch {
            if (candidateIndex + 1 < candidateUrls.size) {
                candidateIndex += 1
                startPlayback(candidateUrls[candidateIndex], station)
                return@launch
            }
            val refreshed = repository.resolvePlaybackUrls(station.stationuuid)
            val next = refreshed.firstOrNull { it !in triedUrls }
            if (next != null) {
                candidateUrls = refreshed
                candidateIndex = refreshed.indexOf(next)
                startPlayback(next, station)
                return@launch
            }
            _state.update { it.copy(status = "Playback failed") }
        }
    }

    private fun ensureServiceStarted() {
        val intent = Intent(app, RadioPlaybackService::class.java)
        app.startForegroundService(intent)
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controller?.release()
        super.onCleared()
    }
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val status: String = "Idle",
    val currentStation: Station? = null
)
