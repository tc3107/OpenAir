package com.example.openair.ui

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openair.data.model.Country
import com.example.openair.data.model.Language
import com.example.openair.data.model.NearMeCacheEntry
import com.example.openair.data.model.PlaylistStore
import com.example.openair.data.model.Station
import com.example.openair.data.model.Tag
import com.example.openair.data.repo.PlaylistRepository
import com.example.openair.data.repo.PlaylistExportResult
import com.example.openair.data.repo.PlaylistImportMode
import com.example.openair.data.repo.PlaylistImportResult
import com.example.openair.data.repo.RadioRepository
import com.example.openair.data.repo.SearchFilters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

enum class BrowseMode {
    Countries,
    Tags,
    Languages
}

class BrowseViewModel(private val repository: RadioRepository) : ViewModel() {
    private val _countries = MutableStateFlow<List<Country>>(emptyList())
    val countries: StateFlow<List<Country>> = _countries

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags

    private val _languages = MutableStateFlow<List<Language>>(emptyList())
    val languages: StateFlow<List<Language>> = _languages

    private val _allStations = MutableStateFlow<List<Station>>(emptyList())
    private val _filteredStations = MutableStateFlow<List<Station>>(emptyList())
    val filteredStations: StateFlow<List<Station>> = _filteredStations

    private val _filterProgress = MutableStateFlow<Float?>(null)
    val filterProgress: StateFlow<Float?> = _filterProgress

    private val _isFiltering = MutableStateFlow(false)
    val isFiltering: StateFlow<Boolean> = _isFiltering

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var loadJob: Job? = null
    private var filterJob: Job? = null
    private var lastMode: BrowseMode = BrowseMode.Countries
    private var deferredMode: BrowseMode? = null
    private val loadedModes = mutableSetOf<BrowseMode>()
    private val stationLoadMutex = Mutex()

    fun loadAll() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val countries = repository.getCountries()
                val tags = repository.getTags()
                val languages = repository.getLanguages()
                _countries.value = sortCountries(countries)
                _tags.value = sortTags(tags)
                _languages.value = sortLanguages(languages)
                loadedModes.addAll(BrowseMode.values().toSet())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message
                Log.e(LOG_TAG, "Browse load failed", e)
            } finally {
                _isLoading.value = false
                val currentJob = coroutineContext[Job]
                if (currentJob != null && loadJob == currentJob) {
                    loadJob = null
                }
            }
        }
    }

    fun loadMode(mode: BrowseMode) {
        if (loadJob?.isActive == true && mode == lastMode) return
        if (loadJob?.isActive == true && mode != lastMode) {
            deferredMode = lastMode
        }
        startLoad(mode)
    }

    fun deferCurrentLoad(): Boolean {
        val active = loadJob?.isActive == true
        if (active) {
            deferredMode = lastMode
            loadJob?.cancel()
            loadJob = null
            _isLoading.value = false
        }
        return active
    }

    fun resumeDeferredLoad() {
        resumeDeferredIfIdle()
    }

    private fun startLoad(mode: BrowseMode) {
        lastMode = mode
        loadJob?.cancel()
        loadJob = null
        if (loadedModes.contains(mode)) {
            resumeDeferredIfIdle()
            return
        }
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                when (mode) {
                    BrowseMode.Countries -> {
                        repository.readCountriesCache()?.let { cached ->
                            if (cached.isNotEmpty()) {
                                _countries.value = sortCountries(cached)
                            }
                        }
                        val fresh = repository.getCountries()
                        val sorted = sortCountries(fresh)
                        if (sorted != _countries.value) {
                            _countries.value = sorted
                        }
                    }
                    BrowseMode.Tags -> {
                        repository.readTagsCache()?.let { cached ->
                            if (cached.isNotEmpty()) {
                                _tags.value = sortTags(cached)
                            }
                        }
                        val fresh = repository.getTags()
                        val sorted = sortTags(fresh)
                        if (sorted != _tags.value) {
                            _tags.value = sorted
                        }
                    }
                    BrowseMode.Languages -> {
                        repository.readLanguagesCache()?.let { cached ->
                            if (cached.isNotEmpty()) {
                                _languages.value = sortLanguages(cached)
                            }
                        }
                        val fresh = repository.getLanguages()
                        val sorted = sortLanguages(fresh)
                        if (sorted != _languages.value) {
                            _languages.value = sorted
                        }
                    }
                }
                loadedModes.add(mode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message
                Log.e(LOG_TAG, "Browse load failed", e)
            } finally {
                _isLoading.value = false
                val currentJob = coroutineContext[Job]
                if (currentJob != null && loadJob == currentJob) {
                    loadJob = null
                }
                resumeDeferredIfIdle()
            }
        }
    }

    private fun resumeDeferredIfIdle() {
        val mode = deferredMode ?: return
        if (loadJob?.isActive == true) return
        deferredMode = null
        startLoad(mode)
    }

    private suspend fun sortCountries(items: List<Country>): List<Country> =
        withContext(Dispatchers.Default) {
            if (items.isEmpty()) return@withContext emptyList()
            items.sortedWith(
                compareByDescending<Country> { it.stationcount }
                    .thenBy { it.name.lowercase() }
            )
        }

    private suspend fun sortTags(items: List<Tag>): List<Tag> =
        withContext(Dispatchers.Default) {
            if (items.isEmpty()) return@withContext emptyList()
            items.sortedWith(
                compareByDescending<Tag> { it.stationcount }
                    .thenBy { it.name.lowercase() }
            )
        }

    private suspend fun sortLanguages(items: List<Language>): List<Language> =
        withContext(Dispatchers.Default) {
            if (items.isEmpty()) return@withContext emptyList()
            items.sortedWith(
                compareByDescending<Language> { it.stationcount }
                    .thenBy { it.name.lowercase() }
            )
        }

    fun clearFilteredStations() {
        filterJob?.cancel()
        _filteredStations.value = emptyList()
        _filterProgress.value = null
        _isFiltering.value = false
    }

    fun filterStations(query: String, filters: BrowseFilterInputs?, sort: BrowseSortOption) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            _isFiltering.value = true
            _filterProgress.value = 0f
            val stations = ensureAllStationsLoaded()
            if (stations.isEmpty()) {
                _filteredStations.value = emptyList()
                _filterProgress.value = null
                _isFiltering.value = false
                return@launch
            }
            val queryValue = query.trim()
            val hasQuery = queryValue.isNotBlank()
            val countryFilter = filters?.country?.trim().orEmpty()
            val languageFilter = filters?.language?.trim().orEmpty()
            val tagFilter = filters?.tag?.trim().orEmpty()
            val minVotes = filters?.minVotes

            val total = stations.size
            val step = (total / 100).coerceAtLeast(200)
            val results = ArrayList<Station>(total / 5)
            try {
                for (index in stations.indices) {
                    val station = stations[index]
                    var matches = true
                    if (hasQuery && !station.name.contains(queryValue, ignoreCase = true)) {
                        matches = false
                    }
                    if (matches && countryFilter.isNotBlank() &&
                        station.country?.contains(countryFilter, ignoreCase = true) != true
                    ) {
                        matches = false
                    }
                    if (matches && languageFilter.isNotBlank() &&
                        station.language?.contains(languageFilter, ignoreCase = true) != true
                    ) {
                        matches = false
                    }
                    if (matches && tagFilter.isNotBlank() &&
                        station.tags?.contains(tagFilter, ignoreCase = true) != true
                    ) {
                        matches = false
                    }
                    if (matches && minVotes != null && (station.votes ?: 0) < minVotes) {
                        matches = false
                    }
                    if (matches) {
                        results.add(station)
                    }
                    if (index % step == 0 || index == total - 1) {
                        if (!isActive) return@launch
                        yield()
                        _filterProgress.value = if (total > 0) (index + 1).toFloat() / total else 1f
                    }
                }
                when (sort) {
                    BrowseSortOption.Votes ->
                        results.sortWith(compareByDescending<Station> { it.votes ?: 0 }.thenBy { it.name.lowercase() })
                    BrowseSortOption.Clicks ->
                        results.sortWith(compareByDescending<Station> { it.clickcount ?: 0 }.thenBy { it.name.lowercase() })
                    BrowseSortOption.Distance -> {}
                }
                if (isActive) {
                    _filteredStations.value = results
                }
            } finally {
                _filterProgress.value = null
                _isFiltering.value = false
            }
        }
    }

    private suspend fun ensureAllStationsLoaded(): List<Station> {
        val cached = _allStations.value
        if (cached.isNotEmpty()) return cached
        return stationLoadMutex.withLock {
            val secondCheck = _allStations.value
            if (secondCheck.isNotEmpty()) return@withLock secondCheck
            val loaded = repository.getAllStations()
            _allStations.value = loaded
            loaded
        }
    }

}

class StationListViewModel(private val repository: RadioRepository) : ViewModel() {
    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isRetrying = MutableStateFlow(false)
    val isRetrying: StateFlow<Boolean> = _isRetrying

    private val _matchCount = MutableStateFlow<Int?>(null)
    val matchCount: StateFlow<Int?> = _matchCount

    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded: StateFlow<Boolean> = _hasLoaded

    private var offset = 0
    private val limit = 50
    private var currentFilters: SearchFilters? = null
    private var loadJob: Job? = null
    private var countJob: Job? = null
    private var searchGeneration = 0

    init {
        viewModelScope.launch {
            val cached = repository.readLastSearch()
            if (!cached.isNullOrEmpty()) {
                _stations.value = cached
                _canLoadMore.value = cached.size >= limit
            }
        }
    }

    fun search(filters: SearchFilters) {
        loadJob?.cancel()
        loadJob = null
        countJob?.cancel()
        countJob = null
        _isLoading.value = false
        _isRetrying.value = false
        _error.value = null
        _matchCount.value = null
        searchGeneration += 1
        val generation = searchGeneration
        currentFilters = filters
        offset = 0
        _stations.value = emptyList()
        _canLoadMore.value = true
        _hasLoaded.value = false
        countJob = viewModelScope.launch {
            try {
                val count = repository.countStations(filters)
                if (generation == searchGeneration) {
                    _matchCount.value = count
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Station count failed", e)
            }
        }
        loadNextPage(reset = true, generation = generation)
    }

    fun loadNextPage(reset: Boolean = false, generation: Int = searchGeneration) {
        val filters = currentFilters ?: return
        if (_isLoading.value || !_canLoadMore.value) return
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _isRetrying.value = false
            val requestOffset = offset
            val requestLimit = limit
            val cached = repository.readSearchCache(filters, requestOffset, requestLimit).orEmpty()
            if (generation != searchGeneration) return@launch
            var cachedSize = 0
            if (cached.isNotEmpty()) {
                cachedSize = cached.size
                if (reset) {
                    _stations.value = cached
                } else {
                    _stations.value = _stations.value + cached
                }
                _canLoadMore.value = cached.size >= requestLimit
            }
            try {
                if (generation != searchGeneration) return@launch
                var delayMillis = 400L
                while (isActive) {
                    try {
                        val result = repository.searchStations(filters, requestOffset, requestLimit)
                        if (generation != searchGeneration) return@launch
                        offset = requestOffset + result.size
                        if (cachedSize > 0) {
                            val trimmed = _stations.value.dropLast(cachedSize)
                            _stations.value = trimmed + result
                        } else if (reset) {
                            _stations.value = result
                        } else {
                            _stations.value = _stations.value + result
                        }
                        _canLoadMore.value = result.size >= requestLimit
                        _error.value = null
                        _isRetrying.value = false
                        _hasLoaded.value = true
                        break
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _error.value = e.message
                        _isRetrying.value = true
                        Log.e(LOG_TAG, "Station search failed", e)
                        delay(delayMillis)
                        delayMillis = (delayMillis * 2).coerceAtMost(5000L)
                    }
                }
            } finally {
                if (generation == searchGeneration) {
                    _isLoading.value = false
                    if (!isActive) {
                        _isRetrying.value = false
                    }
                }
                val currentJob = coroutineContext[Job]
                if (currentJob != null && loadJob == currentJob) {
                    loadJob = null
                }
            }
        }
    }

    fun vote(station: Station) {
        viewModelScope.launch {
            runCatching { repository.vote(station.stationuuid) }
                .onFailure { Log.e(LOG_TAG, "Vote failed for ${station.stationuuid}", it) }
        }
    }
}

data class NearMeStation(
    val station: Station,
    val distanceMeters: Int
)

enum class NearMePhase {
    Idle,
    Fetching,
    Computing
}

data class NearMeStatus(
    val phase: NearMePhase = NearMePhase.Idle,
    val message: String? = null,
    val progress: Float? = null,
    val progressLabel: String? = null
)

class NearMeViewModel(private val repository: RadioRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<NearMeStation>>(emptyList())
    val items: StateFlow<List<NearMeStation>> = _items
    private val _allItems = MutableStateFlow<List<NearMeStation>>(emptyList())
    val allItems: StateFlow<List<NearMeStation>> = _allItems

    private val _canLoadMore = MutableStateFlow(false)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore

    private val _isPaging = MutableStateFlow(false)
    val isPaging: StateFlow<Boolean> = _isPaging

    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching

    private val _isComputing = MutableStateFlow(false)
    val isComputing: StateFlow<Boolean> = _isComputing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _status = MutableStateFlow(NearMeStatus())
    val status: StateFlow<NearMeStatus> = _status

    private var allStations: List<NearMeStation> = emptyList()
    private val pageSize = 10

    init {
        viewModelScope.launch {
            val cached = repository.readNearMeCache()
            if (!cached.isNullOrEmpty()) {
                val mapped = cached.map { NearMeStation(it.station, it.distanceMeters) }
                allStations = mapped
                _allItems.value = mapped
                val firstPage = mapped.take(pageSize)
                _items.value = firstPage
                _canLoadMore.value = mapped.size > firstPage.size
            }
        }
    }

    fun clearItems() {
        _items.value = emptyList()
        allStations = emptyList()
        _allItems.value = emptyList()
        _canLoadMore.value = false
        _isPaging.value = false
        _error.value = null
    }

    fun loadNextPage() {
        if (_isFetching.value || _isComputing.value || _isPaging.value) return
        val current = _items.value
        val next = allStations.drop(current.size).take(pageSize)
        if (next.isEmpty()) {
            _canLoadMore.value = false
            return
        }
        _isPaging.value = true
        _items.value = current + next
        _canLoadMore.value = _items.value.size < allStations.size
        _isPaging.value = false
    }

    fun recompute(location: Location) {
        if (_isFetching.value || _isComputing.value) return
        viewModelScope.launch {
            _items.value = emptyList()
            allStations = emptyList()
            _canLoadMore.value = false
            _isPaging.value = false
            _isFetching.value = true
            _error.value = null
            _status.value = NearMeStatus(
                phase = NearMePhase.Fetching,
                message = "Fetching stations with location data…"
            )
            try {
                val stations = withContext(Dispatchers.IO) { repository.getStationsWithGeo() }
                _isFetching.value = false
                _isComputing.value = true
                val total = stations.size
                _status.value = NearMeStatus(
                    phase = NearMePhase.Computing,
                    message = "Computing distances…",
                    progress = if (total > 0) 0f else 1f,
                    progressLabel = "0 / $total"
                )
                val computed = withContext(Dispatchers.Default) {
                    val results = ArrayList<NearMeStation>(total)
                    val step = (total / 100).coerceAtLeast(1)
                    stations.forEachIndexed { index, station ->
                        val lat = station.geo_lat
                        val lon = station.geo_long
                        if (lat != null && lon != null) {
                            val distanceResults = FloatArray(1)
                            Location.distanceBetween(
                                location.latitude,
                                location.longitude,
                                lat,
                                lon,
                                distanceResults
                            )
                            val distanceMeters = distanceResults[0].roundToInt()
                            results.add(NearMeStation(station, distanceMeters))
                        }
                        if (index % step == 0 || index == total - 1) {
                            val progress = if (total > 0) (index + 1).toFloat() / total else 1f
                            _status.value = NearMeStatus(
                                phase = NearMePhase.Computing,
                                message = "Computing distances…",
                                progress = progress,
                                progressLabel = "${index + 1} / $total"
                            )
                        }
                    }
                    results.sortedWith(
                        compareBy<NearMeStation> { it.distanceMeters }
                            .thenByDescending { it.station.votes ?: 0 }
                    )
                }
                runCatching {
                    val payload = computed.map { NearMeCacheEntry(it.station, it.distanceMeters) }
                    repository.writeNearMeCache(payload)
                }.onFailure {
                    Log.w(LOG_TAG, "Near me cache write failed", it)
                }
                allStations = computed
                _allItems.value = computed
                val firstPage = computed.take(pageSize)
                _items.value = firstPage
                _canLoadMore.value = computed.size > firstPage.size
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message
                Log.e(LOG_TAG, "Near me refresh failed", e)
            } finally {
                _isFetching.value = false
                _isComputing.value = false
                _status.value = NearMeStatus()
            }
        }
    }
}

class PlaylistsViewModel(private val repository: PlaylistRepository) : ViewModel() {
    val store: StateFlow<PlaylistStore> = repository.storeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaylistStore())

    fun toggleFavorite(station: Station) {
        viewModelScope.launch {
            repository.toggleFavorite(station)
        }
    }

    fun toggleInPlaylist(playlistId: String, station: Station) {
        viewModelScope.launch {
            repository.toggleStationInPlaylist(playlistId, station)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun renamePlaylist(id: String, name: String) {
        viewModelScope.launch {
            repository.renamePlaylist(id, name)
        }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun movePlaylist(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            repository.movePlaylist(fromIndex, toIndex)
        }
    }


    fun removeFromRecents(station: Station) {
        viewModelScope.launch {
            repository.removeFromRecents(station)
        }
    }

    fun removeFromPlaylist(playlistId: String, station: Station) {
        viewModelScope.launch {
            repository.removeFromPlaylist(playlistId, station)
        }
    }

    fun moveStationInFavorites(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            repository.moveStationInFavorites(fromIndex, toIndex)
        }
    }

    fun moveStationInRecents(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            repository.moveStationInRecents(fromIndex, toIndex)
        }
    }


    fun moveStationInPlaylist(playlistId: String, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            repository.moveStationInPlaylist(playlistId, fromIndex, toIndex)
        }
    }

    suspend fun exportPlaylists(): PlaylistExportResult {
        return repository.exportPlaylists()
    }

    suspend fun importPlaylists(payload: String, mode: PlaylistImportMode): PlaylistImportResult {
        return repository.importPlaylists(payload, mode)
    }
}

private const val LOG_TAG = "OpenAirRadio"
