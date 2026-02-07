package com.tudorc.openair.player

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.tudorc.openair.data.model.Station
import com.tudorc.openair.data.repo.AppStateRepository
import com.tudorc.openair.data.repo.PlaylistRepository
import com.tudorc.openair.data.repo.RadioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import java.util.concurrent.Executor
import kotlin.math.abs

class PlaybackViewModel(
    private val app: Application,
    private val repository: RadioRepository,
    private val playlistRepository: PlaylistRepository,
    private val appStateRepository: AppStateRepository
) : AndroidViewModel(app), PlaylistCommandHandler {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    private var controller: MediaController? = null
    private val triedUrls = mutableSetOf<String>()
    private var candidateUrls: List<String> = emptyList()
    private var candidateIndex = 0
    private var pendingPlayStation: Station? = null
    private var lastAttemptedUrl: String? = null
    private var stationResolveJob: Job? = null
    private var stationSwitchJob: Job? = null
    private var fadeOutJob: Job? = null
    private var playRequestId = 0L
    @Volatile private var playlistContext: PlaylistContext? = null
    @Volatile private var allowBackgroundMediaService = false

    private val executor = Executor { command -> command.run() }

    init {
        val token = SessionToken(app, ComponentName(app, RadioPlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            controller = future.get()
            controller?.addListener(playerListener)
            controller?.let { updateFromController(it) }
            pendingPlayStation?.let { station ->
                pendingPlayStation = null
                playStation(station)
            }
        }, executor)
        restoreLastStation()
        observeBackgroundMediaServiceSetting()
        PlaybackCommandBridge.bind(this)
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(state: Int) {
            val status = buildStatus(state, controller?.isPlaying == true)
            val buffering = state == Player.STATE_BUFFERING
            _state.update {
                it.copy(
                    status = status,
                    isBuffering = buffering,
                    error = if (state == Player.STATE_READY) null else it.error
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update {
                it.copy(
                    status = "Error: ${error.errorCodeName}",
                    isBuffering = false,
                    error = buildPlaybackError(error, lastAttemptedUrl)
                )
            }
            retryNextUrl()
        }
    }

    private fun updateFromController(controller: MediaController) {
        val status = buildStatus(controller.playbackState, controller.isPlaying)
        _state.update {
            it.copy(
                isPlaying = controller.isPlaying,
                isBuffering = controller.playbackState == Player.STATE_BUFFERING,
                status = status,
                error = if (controller.playbackState == Player.STATE_READY) null else it.error
            )
        }
    }

    private fun buildStatus(state: Int, isPlaying: Boolean): String {
        return when (state) {
            Player.STATE_BUFFERING -> "Buffering"
            Player.STATE_READY -> if (isPlaying) "Playing" else "Paused"
            Player.STATE_ENDED -> "Ended"
            else -> "Idle"
        }
    }

    fun playStation(station: Station) {
        playlistContext = null
        playStationInternal(station)
    }

    fun playStationFromPlaylist(station: Station, playlistStations: List<Station>, index: Int) {
        val stations = playlistStations.toList()
        val safeIndex = if (index in stations.indices) {
            index
        } else {
            stations.indexOfFirst { it.stationuuid == station.stationuuid }
        }
        if (safeIndex !in stations.indices) {
            playStation(station)
            return
        }
        playlistContext = PlaylistContext(stations = stations, index = safeIndex)
        playStationInternal(station)
    }

    override fun hasPlaylistContext(): Boolean = playlistContext != null

    override fun skipNext(): Boolean = skipWithinPlaylist(1)

    override fun skipPrevious(): Boolean = skipWithinPlaylist(-1)

    private fun skipWithinPlaylist(step: Int): Boolean {
        val context = playlistContext ?: return false
        val size = context.stations.size
        if (size == 0) return false
        val targetIndex = ((context.index + step) % size + size) % size
        val station = context.stations[targetIndex]
        playlistContext = context.copy(index = targetIndex)
        playStationInternal(station)
        return true
    }

    private fun playStationInternal(station: Station) {
        stationResolveJob?.cancel()
        val requestId = ++playRequestId
        val transition = beginImmediateStationSwitch()
        stationResolveJob = viewModelScope.launch {
            appStateRepository.saveLastStation(station)
            playlistRepository.addToRecents(station)
            _state.update {
                it.copy(
                    status = "Resolving stream...",
                    currentStation = station,
                    error = null
                )
            }
            val resolvedUrls = repository.resolvePlaybackUrls(station.stationuuid)
            if (isStaleRequest(requestId)) return@launch
            triedUrls.clear()
            candidateUrls = resolvedUrls
            candidateIndex = 0
            if (candidateUrls.isEmpty()) {
                _state.update {
                    it.copy(
                        status = "No playable URL",
                        error = PlaybackError(
                            title = "No playable stream",
                            message = "This station does not provide a usable stream URL."
                        )
                    )
                }
                return@launch
            }
            startPlayback(
                url = candidateUrls[candidateIndex],
                station = station,
                requestId = requestId,
                transition = transition
            )
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
        val player = controller
        val station = _state.value.currentStation
        if (player == null) {
            if (station != null) {
                pendingPlayStation = station
            }
            return
        }
        if (player.isPlaying) {
            player.pause()
            return
        }
        if (player.playbackState == Player.STATE_IDLE || player.currentMediaItem == null) {
            if (station != null) {
                playStation(station)
            }
            return
        }
        player.play()
    }

    fun stop() {
        stationResolveJob?.cancel()
        stationResolveJob = null
        fadeOutJob?.cancel()
        fadeOutJob = null
        stationSwitchJob?.cancel()
        stationSwitchJob = null
        controller?.stop()
        controller?.clearMediaItems()
        controller?.volume = MAX_VOLUME
        stopPlaybackService()
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun startPlayback(
        url: String,
        station: Station,
        requestId: Long = playRequestId,
        transition: StationSwitchTransition = StationSwitchTransition(
            startedAtMs = SystemClock.elapsedRealtime(),
            hadActiveAudio = false
        )
    ) {
        if (isStaleRequest(requestId)) return
        triedUrls.add(url)
        lastAttemptedUrl = url
        ensureServiceStarted()
        val artworkUri = station.favicon?.trim()?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        val item = MediaItem.Builder()
            .setMediaId(station.stationuuid)
            .setUri(url)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle("OpenAir Live")
                    .setArtist(station.name.ifBlank { "Unknown station" })
                    .setSubtitle(station.name.ifBlank { "Unknown station" })
                    .setDescription(station.country ?: "")
                    .setArtworkUri(artworkUri)
                    .build()
            )
            .build()
        val mediaController = controller ?: return
        stationSwitchJob?.cancel()
        stationSwitchJob = viewModelScope.launch {
            try {
                if (transition.hadActiveAudio) {
                    fadeOutJob?.cancel()
                    fadeOutJob = null
                } else {
                    mediaController.volume = MAX_VOLUME
                }
                if (isStaleRequest(requestId)) return@launch
                transitionToStation(mediaController, item, requestId, transition)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                mediaController.volume = MAX_VOLUME
            }
        }
    }

    private suspend fun transitionToStation(
        controller: MediaController,
        item: MediaItem,
        requestId: Long,
        transition: StationSwitchTransition
    ) {
        if (!transition.hadActiveAudio) {
            controller.volume = MAX_VOLUME
            controller.setMediaItem(item)
            controller.prepare()
            controller.play()
            return
        }

        controller.stop()
        controller.clearMediaItems()
        controller.volume = MIN_VOLUME
        controller.setMediaItem(item)
        controller.prepare()
        controller.play()

        val elapsed = SystemClock.elapsedRealtime() - transition.startedAtMs
        val remainingWindow = (TOTAL_TRANSITION_WINDOW_MS - elapsed).coerceAtLeast(0L)
        val readyWithinWindow = waitUntilReady(controller, remainingWindow, requestId)
        if (!readyWithinWindow) {
            waitUntilReady(controller, READY_WAIT_TIMEOUT_MS, requestId)
        }
        if (isStaleRequest(requestId)) return

        if (controller.currentMediaItem?.mediaId == item.mediaId &&
            controller.playbackState == Player.STATE_READY
        ) {
            val fadeInDuration = if (readyWithinWindow) {
                FAST_FADE_IN_DURATION_MS
            } else {
                NORMAL_FADE_IN_DURATION_MS
            }
            fadeVolume(controller, MAX_VOLUME, fadeInDuration)
        } else {
            controller.volume = MAX_VOLUME
        }
    }

    private suspend fun waitUntilReady(
        controller: MediaController,
        timeoutMs: Long,
        requestId: Long
    ): Boolean {
        if (isStaleRequest(requestId)) return false
        if (timeoutMs <= 0L) {
            return controller.playbackState == Player.STATE_READY
        }
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (isStaleRequest(requestId)) return false
            if (controller.playbackState == Player.STATE_READY) return true
            if (controller.playbackState == Player.STATE_IDLE || controller.playbackState == Player.STATE_ENDED) {
                return false
            }
            delay(POLL_INTERVAL_MS)
        }
        return controller.playbackState == Player.STATE_READY
    }

    private suspend fun fadeVolume(controller: MediaController, target: Float, durationMs: Long) {
        val clampedTarget = target.coerceIn(MIN_VOLUME, MAX_VOLUME)
        val start = controller.volume.coerceIn(MIN_VOLUME, MAX_VOLUME)
        if (durationMs <= 0L || abs(start - clampedTarget) < 0.01f) {
            controller.volume = clampedTarget
            return
        }
        val steps = (durationMs / VOLUME_STEP_MS).toInt().coerceAtLeast(1)
        val stepDelay = (durationMs / steps).coerceAtLeast(1L)
        for (step in 1..steps) {
            val fraction = step.toFloat() / steps
            controller.volume = start + ((clampedTarget - start) * fraction)
            delay(stepDelay)
        }
        controller.volume = clampedTarget
    }

    private fun beginImmediateStationSwitch(): StationSwitchTransition {
        val startedAtMs = SystemClock.elapsedRealtime()
        val mediaController = controller ?: return StationSwitchTransition(
            startedAtMs = startedAtMs,
            hadActiveAudio = false
        )
        val hasActiveAudio = mediaController.isPlaying ||
            (mediaController.playWhenReady && mediaController.playbackState == Player.STATE_BUFFERING)

        fadeOutJob?.cancel()
        if (!hasActiveAudio) {
            mediaController.volume = MAX_VOLUME
            fadeOutJob = null
            return StationSwitchTransition(
                startedAtMs = startedAtMs,
                hadActiveAudio = false
            )
        }

        fadeOutJob = viewModelScope.launch {
            fadeVolume(mediaController, MIN_VOLUME, FADE_OUT_DURATION_MS)
            mediaController.pause()
            mediaController.stop()
            mediaController.clearMediaItems()
            mediaController.volume = MIN_VOLUME
        }
        return StationSwitchTransition(
            startedAtMs = startedAtMs,
            hadActiveAudio = true
        )
    }

    private fun isStaleRequest(requestId: Long): Boolean = requestId != playRequestId

    private fun retryNextUrl() {
        val station = _state.value.currentStation ?: return
        val requestId = playRequestId
        viewModelScope.launch {
            if (isStaleRequest(requestId)) return@launch
            if (candidateIndex + 1 < candidateUrls.size) {
                candidateIndex += 1
                startPlayback(
                    url = candidateUrls[candidateIndex],
                    station = station,
                    requestId = requestId
                )
                return@launch
            }
            val refreshed = repository.resolvePlaybackUrls(station.stationuuid)
            if (isStaleRequest(requestId)) return@launch
            val next = refreshed.firstOrNull { it !in triedUrls }
            if (next != null) {
                candidateUrls = refreshed
                candidateIndex = refreshed.indexOf(next)
                startPlayback(
                    url = next,
                    station = station,
                    requestId = requestId
                )
                return@launch
            }
            _state.update {
                it.copy(
                    status = "Playback failed",
                    error = PlaybackError(
                        title = "Station unavailable",
                        message = "All available stream URLs failed. The station may be offline or blocked."
                    )
                )
            }
        }
    }

    private fun ensureServiceStarted() {
        if (!allowBackgroundMediaService) return
        val intent = Intent(app, RadioPlaybackService::class.java)
        app.startForegroundService(intent)
    }

    private fun stopPlaybackService() {
        val intent = Intent(app, RadioPlaybackService::class.java)
        app.stopService(intent)
    }

    private fun observeBackgroundMediaServiceSetting() {
        viewModelScope.launch {
            appStateRepository.observeAllowBackgroundMediaService().collect { enabled ->
                allowBackgroundMediaService = enabled
                if (enabled) {
                    if (controller?.currentMediaItem != null) {
                        ensureServiceStarted()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        PlaybackCommandBridge.bind(null)
        stationResolveJob?.cancel()
        stationResolveJob = null
        fadeOutJob?.cancel()
        fadeOutJob = null
        stationSwitchJob?.cancel()
        stationSwitchJob = null
        controller?.removeListener(playerListener)
        controller?.release()
        super.onCleared()
    }
}

private data class PlaylistContext(
    val stations: List<Station>,
    val index: Int
)

private data class StationSwitchTransition(
    val startedAtMs: Long,
    val hadActiveAudio: Boolean
)

private fun buildPlaybackError(error: PlaybackException, url: String?): PlaybackError {
    val host = url?.let { runCatching { Uri.parse(it).host }.getOrNull() }?.takeIf { it.isNotBlank() }
    val hostLabel = host?.let { " ($it)" } ?: ""
    val cause = error.cause
    return when (cause) {
        is UnknownHostException -> PlaybackError(
            title = "Can't reach station",
            message = "We couldn't resolve the station host$hostLabel. Check your connection or try again."
        )
        is SocketTimeoutException -> PlaybackError(
            title = "Connection timed out",
            message = "The station didn't respond in time$hostLabel. Try again later."
        )
        is ConnectException -> PlaybackError(
            title = "Connection failed",
            message = "We couldn't connect to the station$hostLabel. It may be offline or blocked."
        )
        is SSLException -> PlaybackError(
            title = "Secure connection failed",
            message = "The station's HTTPS connection couldn't be established$hostLabel."
        )
        else -> when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> PlaybackError(
                title = "No network connection",
                message = "Check your internet connection and try again."
            )
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> PlaybackError(
                title = "Network timeout",
                message = "The station did not respond in time$hostLabel."
            )
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                val status = (cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode
                val detail = status?.let { "HTTP $it" } ?: "HTTP error"
                PlaybackError(
                    title = "Stream rejected",
                    message = "The station server returned $detail$hostLabel."
                )
            }
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> PlaybackError(
                title = "Stream not found",
                message = "The station stream URL is no longer available."
            )
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> PlaybackError(
                title = "Access denied",
                message = "The stream couldn't be accessed due to permission or policy restrictions."
            )
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> PlaybackError(
                title = "Cleartext blocked",
                message = "This station uses HTTP and cleartext traffic isn't permitted on this device."
            )
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> PlaybackError(
                title = "Unsupported stream",
                message = "This station uses a stream format the player can't read."
            )
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> PlaybackError(
                title = "Unsupported audio",
                message = "Your device doesn't support this audio format."
            )
            PlaybackException.ERROR_CODE_DECODING_FAILED -> PlaybackError(
                title = "Audio decode failed",
                message = "The stream couldn't be decoded. Try another station."
            )
            PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR,
            PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED,
            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
            PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED,
            PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED -> PlaybackError(
                title = "DRM error",
                message = "This stream requires DRM that isn't available on this device."
            )
            else -> PlaybackError(
                title = "Playback error",
                message = "Something went wrong while playing this station."
            )
        }
    }
}

data class PlaybackError(
    val title: String,
    val message: String
)

data class PlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val status: String = "Idle",
    val currentStation: Station? = null,
    val error: PlaybackError? = null
)

private const val TOTAL_TRANSITION_WINDOW_MS = 1_000L
private const val FADE_OUT_DURATION_MS = 700L
private const val FAST_FADE_IN_DURATION_MS = 170L
private const val NORMAL_FADE_IN_DURATION_MS = 260L
private const val READY_WAIT_TIMEOUT_MS = 30_000L
private const val POLL_INTERVAL_MS = 25L
private const val VOLUME_STEP_MS = 30L
private const val MIN_VOLUME = 0f
private const val MAX_VOLUME = 1f
