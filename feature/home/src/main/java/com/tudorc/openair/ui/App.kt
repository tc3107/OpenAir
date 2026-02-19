package com.tudorc.openair.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.imageLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import com.tudorc.openair.data.model.Country
import com.tudorc.openair.data.model.Language
import com.tudorc.openair.data.model.Playlist
import com.tudorc.openair.data.model.PlaylistStore
import com.tudorc.openair.data.model.Station
import com.tudorc.openair.data.model.SystemPlaylistNames
import com.tudorc.openair.data.model.Tag
import com.tudorc.openair.data.model.normalizePlaylistName
import com.tudorc.openair.data.repo.AppStateRepository
import com.tudorc.openair.data.repo.DatabaseRebuildPhase
import com.tudorc.openair.data.repo.DatabaseStatus
import com.tudorc.openair.data.repo.NavigationState
import com.tudorc.openair.data.repo.PlaylistImportMode
import com.tudorc.openair.data.repo.PlaylistRefType
import com.tudorc.openair.data.repo.PlaylistRepository
import com.tudorc.openair.data.repo.RadioRepository
import com.tudorc.openair.data.repo.ScreenType
import com.tudorc.openair.data.repo.SearchFilters
import com.tudorc.openair.player.PlaybackError
import com.tudorc.openair.player.PlaybackViewModel

sealed class Screen {
    data object Browse : Screen()
    data object Playlists : Screen()
    data object NearMe : Screen()
    data object Config : Screen()
    data class StationList(val title: String, val filters: SearchFilters) : Screen()
    data class PlaylistStations(val title: String, val ref: PlaylistRef) : Screen()
}

sealed class PlaylistRef(val label: String) {
    data object Favorites : PlaylistRef(SystemPlaylistNames.FAVORITES)
    data object Recents : PlaylistRef(SystemPlaylistNames.RECENTS)
    data class User(val id: String, val name: String) : PlaylistRef(name)
}

enum class BrowseSortOption(val label: String) {
    Votes("Votes"),
    Clicks("Clicks"),
    Distance("Distance")
}

data class BrowseFilterInputs(
    val country: String,
    val language: String,
    val tag: String,
    val minVotes: Int?
)

private val browseFilterInputsStateSaver: Saver<MutableState<BrowseFilterInputs?>, Any> = Saver(
    save = { state ->
        val input = state.value
        if (input == null) {
            emptyMap<String, Any>()
        } else {
            mapOf(
                "country" to input.country,
                "language" to input.language,
                "tag" to input.tag,
                "minVotes" to (input.minVotes ?: -1)
            )
        }
    },
    restore = { restored ->
        val map = restored as? Map<*, *> ?: emptyMap<Any, Any>()
        val input = if (map.isEmpty()) {
            null
        } else {
            val minVotes = (map["minVotes"] as? Int) ?: -1
            BrowseFilterInputs(
                country = map["country"] as? String ?: "",
                language = map["language"] as? String ?: "",
                tag = map["tag"] as? String ?: "",
                minVotes = minVotes.takeIf { it >= 0 }
            )
        }
        mutableStateOf(input)
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenAirApp() {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val repository = remember { RadioRepository(app) }
    val playlistRepository = remember { PlaylistRepository(app) }
    val appStateRepository = remember { AppStateRepository(app) }
    val factory = remember { AppViewModelFactory(app, repository, playlistRepository, appStateRepository) }

    val browseViewModel: BrowseViewModel = viewModel(factory = factory)
    val stationListViewModel: StationListViewModel = viewModel(factory = factory)
    val nearMeViewModel: NearMeViewModel = viewModel(factory = factory)
    val playbackViewModel: PlaybackViewModel = viewModel(factory = factory)
    val playlistsViewModel: PlaylistsViewModel = viewModel(factory = factory)
    val configViewModel: ConfigViewModel = viewModel(factory = factory)

    var screen by remember { mutableStateOf<Screen>(Screen.Browse) }
    var navRestored by remember { mutableStateOf(false) }
    var navLoaded by remember { mutableStateOf(false) }
    var pendingNavigation by remember { mutableStateOf<NavigationState?>(null) }
    var browseQuery by rememberSaveable { mutableStateOf("") }
    var playlistsQuery by rememberSaveable { mutableStateOf("") }
    var stationListQuery by rememberSaveable { mutableStateOf("") }
    var playlistStationsQuery by rememberSaveable { mutableStateOf("") }
    val playlistStore by playlistsViewModel.store.collectAsState()
    val configState by configViewModel.uiState.collectAsState()
    val statusLoaded by configViewModel.isStatusLoaded.collectAsState()
    val stationListLoading by stationListViewModel.isLoading.collectAsState()
    val stationListRetrying by stationListViewModel.isRetrying.collectAsState()
    val browseFilterProgress by browseViewModel.filterProgress.collectAsState()
    val browseFiltering by browseViewModel.isFiltering.collectAsState()
    val nearMeFetching by nearMeViewModel.isFetching.collectAsState()
    val nearMeComputing by nearMeViewModel.isComputing.collectAsState()
    val playbackState by playbackViewModel.state.collectAsState()
    val isDatabaseReady = statusLoaded && configState.status == DatabaseStatus.Ready
    var browseResetNonce by rememberSaveable { mutableStateOf(0) }
    var playlistsResetNonce by rememberSaveable { mutableStateOf(0) }
    val currentTab = when (screen) {
        Screen.Browse, is Screen.StationList, Screen.NearMe -> Screen.Browse
        Screen.Playlists, is Screen.PlaylistStations -> Screen.Playlists
        Screen.Config -> Screen.Config
    }
    val currentQuery = when (screen) {
        Screen.Browse -> browseQuery
        Screen.Playlists -> playlistsQuery
        Screen.NearMe -> ""
        Screen.Config -> ""
        is Screen.StationList -> stationListQuery
        is Screen.PlaylistStations -> playlistStationsQuery
    }

    val haptics = rememberHaptics()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val dismissKeyboardOnTap = Modifier.pointerInput(imeVisible) {
        if (!imeVisible) return@pointerInput
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val isDown = event.changes.any { it.pressed && !it.previousPressed }
                if (isDown) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            }
        }
    }

    BackHandler(enabled = imeVisible) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    BackHandler(enabled = !imeVisible && screen is Screen.StationList) {
        screen = Screen.Browse
    }

    BackHandler(enabled = !imeVisible && screen is Screen.PlaylistStations) {
        screen = Screen.Playlists
    }

    BackHandler(enabled = !imeVisible && screen is Screen.Browse && browseQuery.isNotBlank()) {
        browseQuery = ""
    }

    BackHandler(enabled = !imeVisible && screen is Screen.Playlists && playlistsQuery.isNotBlank()) {
        playlistsQuery = ""
    }

    LaunchedEffect(Unit) {
        pendingNavigation = appStateRepository.readNavigationState()
        navLoaded = true
    }

    LaunchedEffect(pendingNavigation, playlistStore, navLoaded, navRestored, configState.status, statusLoaded) {
        if (!navLoaded || navRestored || !statusLoaded) return@LaunchedEffect
        if (configState.status != DatabaseStatus.Ready) {
            screen = Screen.Config
            return@LaunchedEffect
        }
        screen = pendingNavigation?.toScreen(playlistStore) ?: Screen.Browse
        navRestored = true
    }

    LaunchedEffect(screen, navRestored) {
        if (navRestored) {
            appStateRepository.saveNavigationState(screen.toNavigationState())
        }
    }

    LaunchedEffect(configState.status, statusLoaded) {
        if (statusLoaded && configState.status != DatabaseStatus.Ready && screen !is Screen.Config) {
            screen = Screen.Config
        }
    }

    val topBarLoading = when (screen) {
        Screen.Browse -> browseFiltering || nearMeFetching || nearMeComputing
        Screen.NearMe -> nearMeFetching || nearMeComputing
        is Screen.StationList -> stationListLoading || stationListRetrying
        Screen.Playlists, is Screen.PlaylistStations, Screen.Config -> false
    } || playbackState.isBuffering
    val topBarError = screen is Screen.StationList && stationListRetrying
    val topBarProgress = if (screen is Screen.Browse) browseFilterProgress else null

    Scaffold(
        topBar = {
            if (screen !is Screen.Config) {
                SearchTopBar(
                    query = currentQuery,
                    showLoading = topBarLoading,
                    isError = topBarError,
                    progress = topBarProgress,
                    onQueryChange = { value ->
                        when (screen) {
                            Screen.Browse -> browseQuery = value
                            Screen.Playlists -> playlistsQuery = value
                            Screen.NearMe -> {}
                            is Screen.StationList -> stationListQuery = value
                            is Screen.PlaylistStations -> playlistStationsQuery = value
                            Screen.Config -> {}
                        }
                    }
                )
            }
        },
        bottomBar = {
            val isRebuildInProgress = configState.isRebuilding || configState.status == DatabaseStatus.Building
            if (!isRebuildInProgress && !imeVisible) {
                Column(modifier = dismissKeyboardOnTap) {
                    PlayerBar(playbackViewModel, playlistsViewModel)
                    NavigationBar {
                        if (isDatabaseReady) {
                            NavigationBarItem(
                                selected = currentTab == Screen.Browse,
                                onClick = {
                                    haptics.soft()
                                    if (currentTab == Screen.Browse) {
                                        browseResetNonce += 1
                                    } else {
                                        browseQuery = ""
                                        screen = Screen.Browse
                                    }
                                },
                                label = { Text("Browse") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Explore,
                                        contentDescription = null
                                    )
                                }
                            )
                            NavigationBarItem(
                                selected = currentTab == Screen.Playlists,
                                onClick = {
                                    haptics.soft()
                                    if (currentTab == Screen.Playlists) {
                                        playlistsQuery = ""
                                        if (screen is Screen.PlaylistStations) {
                                            playlistStationsQuery = ""
                                            screen = Screen.Playlists
                                        } else {
                                            playlistsResetNonce += 1
                                        }
                                    } else {
                                        playlistsQuery = ""
                                        screen = Screen.Playlists
                                    }
                                },
                                label = { Text("Playlists") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                        NavigationBarItem(
                            selected = currentTab == Screen.Config,
                            onClick = {
                                haptics.soft()
                                screen = Screen.Config
                            },
                            label = { Text("Config") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.HelpOutline,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        val contentModifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .then(dismissKeyboardOnTap)
        when (val current = screen) {
            Screen.Browse, Screen.NearMe -> BrowseScreen(
                modifier = contentModifier,
                browseViewModel = browseViewModel,
                stationListViewModel = stationListViewModel,
                nearMeViewModel = nearMeViewModel,
                playbackViewModel = playbackViewModel,
                playlistsViewModel = playlistsViewModel,
                searchQuery = browseQuery,
                onClearSearch = { browseQuery = "" },
                totalStations = configState.summary?.stationCount ?: 0,
                resetSignal = browseResetNonce
            )

            is Screen.StationList -> StationListScreen(
                modifier = contentModifier,
                filters = current.filters,
                viewModel = stationListViewModel,
                playbackViewModel = playbackViewModel,
                playlistsViewModel = playlistsViewModel,
                searchQuery = stationListQuery,
                onBack = { screen = Screen.Browse },
                title = current.title
            )

            Screen.Playlists -> PlaylistsScreen(
                modifier = contentModifier,
                viewModel = playlistsViewModel,
                playbackViewModel = playbackViewModel,
                searchQuery = playlistsQuery,
                resetSignal = playlistsResetNonce,
                onPlaylistSelected = { ref, title ->
                    playlistStationsQuery = ""
                    screen = Screen.PlaylistStations(title, ref)
                }
            )

            Screen.Config -> ConfigScreen(
                modifier = contentModifier,
                uiState = configState,
                playlistsViewModel = playlistsViewModel,
                onRebuildDatabase = { configViewModel.rebuildDatabase() },
                onAllowBackgroundMediaServiceChange = { enabled ->
                    configViewModel.setAllowBackgroundMediaService(enabled)
                }
            )

            is Screen.PlaylistStations -> PlaylistStationsScreen(
                modifier = contentModifier,
                title = current.title,
                ref = current.ref,
                viewModel = playlistsViewModel,
                playbackViewModel = playbackViewModel,
                searchQuery = playlistStationsQuery,
                onBack = { screen = Screen.Playlists }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    modifier: Modifier,
    browseViewModel: BrowseViewModel,
    stationListViewModel: StationListViewModel,
    nearMeViewModel: NearMeViewModel,
    playbackViewModel: PlaybackViewModel,
    playlistsViewModel: PlaylistsViewModel,
    searchQuery: String,
    onClearSearch: () -> Unit,
    totalStations: Int,
    resetSignal: Int
) {
    val countries by browseViewModel.countries.collectAsState()
    val tags by browseViewModel.tags.collectAsState()
    val languages by browseViewModel.languages.collectAsState()
    val filteredStations by browseViewModel.filteredStations.collectAsState()
    val isFiltering by browseViewModel.isFiltering.collectAsState()
    val distanceItems by nearMeViewModel.allItems.collectAsState()
    val distanceStatus by nearMeViewModel.status.collectAsState()
    val isDistanceFetching by nearMeViewModel.isFetching.collectAsState()
    val isDistanceComputing by nearMeViewModel.isComputing.collectAsState()
    val playlistStore by playlistsViewModel.store.collectAsState()
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var countryInput by rememberSaveable { mutableStateOf("") }
    var languageInput by rememberSaveable { mutableStateOf("") }
    var tagInput by rememberSaveable { mutableStateOf("") }
    var minVotesInput by rememberSaveable { mutableStateOf("") }
    var appliedFilters by rememberSaveable(saver = browseFilterInputsStateSaver) {
        mutableStateOf<BrowseFilterInputs?>(null)
    }

    var sortOption by rememberSaveable { mutableStateOf(BrowseSortOption.Votes) }
    var lastNonDistanceSort by rememberSaveable { mutableStateOf(BrowseSortOption.Votes) }
    var sortExpanded by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var sortFieldHeightPx by remember { mutableStateOf(0) }
    val sortFieldHeight = with(density) { sortFieldHeightPx.toDp() }
    val sortFieldMinHeight = if (sortFieldHeight > 0.dp) sortFieldHeight else 56.dp

    val query = searchQuery.trim()
    val hasSearch = query.isNotBlank()
    val minVotesValue = minVotesInput.trim().toIntOrNull()
    val minVotesError = minVotesInput.trim().isNotBlank() && minVotesValue == null
    val hasFilterInput = countryInput.trim().isNotBlank() ||
        languageInput.trim().isNotBlank() ||
        tagInput.trim().isNotBlank() ||
        minVotesInput.trim().isNotBlank()
    val showResults = hasSearch || appliedFilters != null || sortOption == BrowseSortOption.Distance

    LaunchedEffect(Unit) {
        browseViewModel.loadAll()
    }

    fun resetToFilters() {
        appliedFilters = null
        if (searchQuery.isNotBlank()) {
            onClearSearch()
        }
        if (sortOption == BrowseSortOption.Distance) {
            sortOption = lastNonDistanceSort
        }
        locationError = null
        browseViewModel.clearFilteredStations()
    }

    LaunchedEffect(resetSignal) {
        if (resetSignal == 0) return@LaunchedEffect
        resetToFilters()
    }

    BackHandler(enabled = showResults) {
        resetToFilters()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted || hasLocationPermission(context)) {
            scope.launch {
                locationError = null
                val location = getCurrentLocation(context)
                if (location == null) {
                    locationError = "Unable to get location."
                    sortOption = lastNonDistanceSort
                    return@launch
                }
                sortOption = BrowseSortOption.Distance
                nearMeViewModel.recompute(location)
            }
        } else {
            locationError = "Location permission denied."
            sortOption = lastNonDistanceSort
        }
    }

    fun selectSort(option: BrowseSortOption) {
        if (option == BrowseSortOption.Distance) {
            lastNonDistanceSort = sortOption.takeIf { it != BrowseSortOption.Distance } ?: lastNonDistanceSort
            if (hasLocationPermission(context)) {
                scope.launch {
                    locationError = null
                    val location = getCurrentLocation(context)
                    if (location == null) {
                        locationError = "Unable to get location."
                        sortOption = lastNonDistanceSort
                        return@launch
                    }
                    sortOption = BrowseSortOption.Distance
                    nearMeViewModel.recompute(location)
                }
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            return
        }
        sortOption = option
        lastNonDistanceSort = option
        locationError = null
    }

    val applied = appliedFilters
    LaunchedEffect(showResults, applied, query, sortOption) {
        if (!showResults || sortOption == BrowseSortOption.Distance) {
            browseViewModel.clearFilteredStations()
            return@LaunchedEffect
        }
        delay(150)
        browseViewModel.filterStations(query, applied, sortOption)
    }

    val distanceFiltered by produceState(
        initialValue = emptyList(),
        distanceItems,
        applied,
        query,
        sortOption
    ) {
        value = withContext(Dispatchers.Default) {
            if (sortOption != BrowseSortOption.Distance) {
                emptyList()
            } else {
                distanceItems.filter { item ->
                    val station = item.station
                    val matchesQuery = query.isBlank() ||
                        station.name.contains(query, ignoreCase = true)
                    val matchesCountry = applied?.country?.let { country ->
                        country.isBlank() || station.country?.contains(country, ignoreCase = true) == true
                    } ?: true
                    val matchesLanguage = applied?.language?.let { language ->
                        language.isBlank() || station.language?.contains(language, ignoreCase = true) == true
                    } ?: true
                    val matchesTag = applied?.tag?.let { tag ->
                        tag.isBlank() || station.tags?.contains(tag, ignoreCase = true) == true
                    } ?: true
                    val matchesVotes = applied?.minVotes?.let { minVotes ->
                        (station.votes ?: 0) >= minVotes
                    } ?: true
                    matchesQuery && matchesCountry && matchesLanguage && matchesTag && matchesVotes
                }
            }
        }
    }

    val displayedCount = when {
        sortOption == BrowseSortOption.Distance -> distanceFiltered.size
        showResults -> filteredStations.size
        else -> 0
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            TextField(
                value = sortOption.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Sort by") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        sortFieldHeightPx = coordinates.size.height
                    },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sortFieldMinHeight)
                    .clickable { sortExpanded = true }
            )
            DropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = { sortExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                BrowseSortOption.values().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            sortExpanded = false
                            haptics.soft()
                            selectSort(option)
                        }
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val summary = if (showResults) {
                "$displayedCount/$totalStations stations displayed"
            } else {
                "$totalStations stations available"
            }
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (sortOption == BrowseSortOption.Distance && (isDistanceFetching || isDistanceComputing)) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            distanceStatus.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        locationError?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (!showResults) {
            val countryOptions = remember(countries) { countries.map { it.name }.distinct() }
            val tagOptions = remember(tags) { tags.map { it.name }.distinct() }
            val languageOptions = remember(languages) { languages.map { toTitleCase(it.name) }.distinct() }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    FilterDropdownField(
                        label = "Country",
                        value = countryInput,
                        options = countryOptions,
                        onValueChange = { countryInput = it },
                        onOptionSelected = { countryInput = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    FilterDropdownField(
                        label = "Language",
                        value = languageInput,
                        options = languageOptions,
                        onValueChange = { languageInput = it },
                        onOptionSelected = { languageInput = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    FilterDropdownField(
                        label = "Tag",
                        value = tagInput,
                        options = tagOptions,
                        onValueChange = { tagInput = it },
                        onOptionSelected = { tagInput = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    TextField(
                        value = minVotesInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            minVotesInput = filtered
                        },
                        placeholder = { Text("Min. Votes") },
                        singleLine = true,
                        isError = minVotesError,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                }
                if (minVotesError) {
                    item {
                        Text(
                            text = "Min. Votes must be a number.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item {
                    Button(
                        onClick = {
                            haptics.strong()
                            appliedFilters = BrowseFilterInputs(
                                country = countryInput.trim(),
                                language = languageInput.trim(),
                                tag = tagInput.trim(),
                                minVotes = minVotesValue
                            )
                        },
                        enabled = hasFilterInput && !minVotesError,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Filter Stations")
                    }
                }
            }
            return@Column
        }

        if (sortOption == BrowseSortOption.Distance) {
            val listState = rememberLazyListState()
            var addDialogStation by remember { mutableStateOf<Station?>(null) }
            if (distanceFiltered.isEmpty() && !isDistanceFetching && !isDistanceComputing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No stations match your filters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                    itemsIndexed(distanceFiltered) { index, item ->
                        StationRow(
                            station = item.station,
                            onPlay = { playbackViewModel.playStation(item.station) },
                            iconLoadDelayMs = imageLoadDelayForIndex(index),
                            onAddToPlaylist = { addDialogStation = item.station }
                        )
                    }
                }
            }
            addDialogStation?.let { station ->
                AddToPlaylistDialog(
                    station = station,
                    store = playlistStore,
                    onDismiss = { addDialogStation = null },
                    onToggleFavorite = { playlistsViewModel.toggleFavorite(station) },
                    onCreatePlaylist = { name -> playlistsViewModel.createPlaylist(name) },
                    onTogglePlaylist = { playlistId ->
                        playlistsViewModel.toggleInPlaylist(playlistId, station)
                    }
                )
            }
        } else {
            val listState = rememberLazyListState()
            var addDialogStation by remember { mutableStateOf<Station?>(null) }

            val showFiltering = isFiltering && filteredStations.isEmpty()
            if (showFiltering) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(52.dp),
                            strokeWidth = 3.dp
                        )
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredStations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No stations match your filters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                    itemsIndexed(filteredStations) { index, station ->
                        StationRow(
                            station = station,
                            onPlay = { playbackViewModel.playStation(station) },
                            iconLoadDelayMs = imageLoadDelayForIndex(index),
                            onAddToPlaylist = { addDialogStation = station }
                        )
                    }
                }
            }

            addDialogStation?.let { station ->
                AddToPlaylistDialog(
                    station = station,
                    store = playlistStore,
                    onDismiss = { addDialogStation = null },
                    onToggleFavorite = { playlistsViewModel.toggleFavorite(station) },
                    onCreatePlaylist = { name -> playlistsViewModel.createPlaylist(name) },
                    onTogglePlaylist = { playlistId ->
                        playlistsViewModel.toggleInPlaylist(playlistId, station)
                    }
                )
            }
        }
    }
}

@Composable
fun <T> FacetList(
    items: List<T>,
    modifier: Modifier = Modifier,
    pageSize: Int = 10,
    onSelect: (T) -> Unit
) {
    val listState = rememberLazyListState()
    var visibleCount by remember(items) { mutableStateOf(pageSize) }
    val haptics = rememberHaptics()

    var reachedStart by remember(items) { mutableStateOf(false) }
    var reachedEnd by remember(items) { mutableStateOf(false) }

    LaunchedEffect(items, listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val firstIndex = visibleItems.firstOrNull()?.index ?: 0
                val lastIndex = visibleItems.lastOrNull()?.index ?: -1
                val startNow = firstIndex == 0 && visibleItems.isNotEmpty()
                val endNow = lastIndex >= visibleCount - 1 && visibleItems.isNotEmpty()

                if (startNow && !reachedStart) {
                    haptics.strong()
                }
                if (endNow && !reachedEnd) {
                    haptics.strong()
                }
                reachedStart = startNow
                reachedEnd = endNow

                if (items.isNotEmpty() && lastIndex >= visibleCount - 1 && visibleCount < items.size) {
                    visibleCount = (visibleCount + pageSize).coerceAtMost(items.size)
                }
            }
    }

    val displayItems = items.take(visibleCount)
    LazyColumn(modifier = modifier, state = listState) {
        items(displayItems) { item ->
            val label = when (item) {
                is Country -> formatFacetLabel(item.name, item.stationcount)
                is Tag -> formatFacetLabel(item.name, item.stationcount)
                is Language -> formatFacetLabel(item.name, item.stationcount)
                else -> item.toString()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.soft()
                        onSelect(item)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider()
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}

@SuppressLint("MissingPermission")
private suspend fun getCurrentLocation(context: Context): Location? {
    return runCatching {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val providers = if (hasFine) {
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
        } else {
            listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
        }
        val lastKnown = providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
        if (lastKnown != null) return@runCatching lastKnown
        val provider = providers.firstOrNull { candidate ->
            runCatching { locationManager.isProviderEnabled(candidate) }.getOrDefault(false)
        } ?: return@runCatching null
        withTimeoutOrNull(15000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    try {
                        locationManager.getCurrentLocation(
                            provider,
                            signal,
                            ContextCompat.getMainExecutor(context)
                        ) { location ->
                            if (cont.isActive) cont.resume(location)
                        }
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resume(null)
                    }
                } else {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (cont.isActive) {
                                cont.resume(location)
                            }
                            locationManager.removeUpdates(this)
                        }

                        override fun onProviderEnabled(provider: String) {}

                        override fun onProviderDisabled(provider: String) {}

                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(
                            provider: String?,
                            status: Int,
                            extras: Bundle?
                        ) {
                        }
                    }
                    try {
                        locationManager.requestLocationUpdates(
                            provider,
                            0L,
                            0f,
                            listener,
                            Looper.getMainLooper()
                        )
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resume(null)
                    }
                    cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
                }
            }
        }
    }.getOrNull()
}

@Composable
fun NearMeScreen(
    modifier: Modifier,
    viewModel: NearMeViewModel,
    playbackViewModel: PlaybackViewModel,
    playlistsViewModel: PlaylistsViewModel
) {
    val items by viewModel.items.collectAsState()
    val isFetching by viewModel.isFetching.collectAsState()
    val isComputing by viewModel.isComputing.collectAsState()
    val canLoadMore by viewModel.canLoadMore.collectAsState()
    val isPaging by viewModel.isPaging.collectAsState()
    val status by viewModel.status.collectAsState()
    val error by viewModel.error.collectAsState()
    val playlistStore by playlistsViewModel.store.collectAsState()
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var isLocating by remember { mutableStateOf(false) }
    var locatingMessage by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var addDialogStation by remember { mutableStateOf<Station?>(null) }

    fun startLocationFlow() {
        scope.launch {
            isLocating = true
            locatingMessage = "Getting your location…"
            val location = getCurrentLocation(context)
            isLocating = false
            locatingMessage = null
            if (location == null) {
                localError = "Unable to get location."
                return@launch
            }
            viewModel.recompute(location)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted || hasLocationPermission(context)) {
            startLocationFlow()
        } else {
            isLocating = false
            locatingMessage = null
            localError = "Location permission denied."
        }
    }

    val isBusy = isFetching || isComputing || isLocating
    val listState = rememberLazyListState()
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (isBusy) return@Button
                    haptics.soft()
                    localError = null
                    viewModel.clearItems()
                    if (!hasLocationPermission(context)) {
                        isLocating = true
                        locatingMessage = "Requesting location permission…"
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        startLocationFlow()
                    }
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isBusy) 0.6f else 1f)
            ) {
                Text("Recompute proximity data")
            }
        }

        val errorMessage = localError ?: error
        val showStatusBox = locatingMessage != null ||
            status.phase != NearMePhase.Idle ||
            errorMessage != null

        if (showStatusBox) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Near Me",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    val message = errorMessage
                        ?: locatingMessage
                        ?: status.message
                        ?: "Working…"
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (errorMessage != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (errorMessage == null) {
                        if (status.phase == NearMePhase.Computing && status.progress != null) {
                            status.progress?.let { progress ->
                                LinearProgressIndicator(progress = { progress })
                            }
                            status.progressLabel?.let { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (locatingMessage != null || status.phase == NearMePhase.Fetching) {
                            LinearProgressIndicator()
                        }
                    }
                }
            }
        } else if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No nearby stations yet")
            }
        } else {
            LaunchedEffect(items.size, canLoadMore, listState) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                    .collect { visibleItems ->
                        val lastIndex = visibleItems.lastOrNull()?.index ?: -1
                        if (items.isNotEmpty() && lastIndex >= items.lastIndex - 3 && canLoadMore) {
                            viewModel.loadNextPage()
                        }
                    }
            }

            LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                itemsIndexed(items) { index, item ->
                    StationRow(
                        station = item.station,
                        onPlay = { playbackViewModel.playStation(item.station) },
                        iconLoadDelayMs = imageLoadDelayForIndex(index),
                        onAddToPlaylist = { addDialogStation = item.station }
                    )
                }
                item {
                    if (isPaging) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    addDialogStation?.let { station ->
        AddToPlaylistDialog(
            station = station,
            store = playlistStore,
            onDismiss = { addDialogStation = null },
            onToggleFavorite = { playlistsViewModel.toggleFavorite(station) },
            onCreatePlaylist = { name ->
                playlistsViewModel.createPlaylist(name)
            },
            onTogglePlaylist = { playlistId ->
                playlistsViewModel.toggleInPlaylist(playlistId, station)
            }
        )
    }
}

@Composable
fun StationListScreen(
    modifier: Modifier,
    filters: SearchFilters,
    viewModel: StationListViewModel,
    playbackViewModel: PlaybackViewModel,
    playlistsViewModel: PlaylistsViewModel,
    searchQuery: String,
    onBack: () -> Unit,
    title: String
) {
    val stations by viewModel.stations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val canLoadMore by viewModel.canLoadMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val query = searchQuery.trim()
    val haptics = rememberHaptics()
    val playlistStore by playlistsViewModel.store.collectAsState()
    var addDialogStation by remember { mutableStateOf<Station?>(null) }

    LaunchedEffect(filters) {
        viewModel.search(filters)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                haptics.soft()
                onBack()
            }) {
                Text("Back")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    val listState = rememberLazyListState()
    val filtered by produceState(initialValue = stations, stations, query) {
        value = withContext(Dispatchers.Default) {
            if (query.isBlank()) {
                stations
            } else {
                stations.filter { station ->
                    station.name.contains(query, ignoreCase = true)
                }
            }
        }
    }

    var reachedStart by remember(filtered) { mutableStateOf(false) }
    var reachedEnd by remember(filtered) { mutableStateOf(false) }

    LaunchedEffect(filtered.size, isLoading, canLoadMore, listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val firstIndex = visibleItems.firstOrNull()?.index ?: 0
                val lastIndex = visibleItems.lastOrNull()?.index ?: -1
                val startNow = firstIndex == 0 && visibleItems.isNotEmpty()
                val endNow = lastIndex >= filtered.lastIndex && visibleItems.isNotEmpty()

                if (startNow && !reachedStart) {
                    haptics.strong()
                }
                if (endNow && !reachedEnd) {
                    haptics.strong()
                }
                reachedStart = startNow
                reachedEnd = endNow

                if (filtered.isNotEmpty() && lastIndex >= filtered.lastIndex - 3 && canLoadMore && !isLoading) {
                    viewModel.loadNextPage()
                }
            }
    }

    val showInitialLoading = isLoading && stations.isEmpty()
    if (showInitialLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 3.dp
                )
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(modifier = Modifier.weight(1f), state = listState) {
            itemsIndexed(filtered) { index, station ->
                StationRow(
                    station = station,
                    onPlay = { playbackViewModel.playStation(station) },
                    iconLoadDelayMs = imageLoadDelayForIndex(index),
                    onAddToPlaylist = {
                        addDialogStation = station
                    }
                )
            }
            item {
                if (isLoading && stations.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

        addDialogStation?.let { station ->
        AddToPlaylistDialog(
            station = station,
            store = playlistStore,
            onDismiss = { addDialogStation = null },
            onToggleFavorite = {
                playlistsViewModel.toggleFavorite(station)
            },
            onCreatePlaylist = { name ->
                playlistsViewModel.createPlaylist(name)
            },
            onTogglePlaylist = { playlistId ->
                playlistsViewModel.toggleInPlaylist(playlistId, station)
            }
        )
    }
}
}

@Composable
fun StationRow(
    station: Station,
    onPlay: () -> Unit,
    iconLoadDelayMs: Long = 0L,
    onAddToPlaylist: () -> Unit,
    showPlaylistMenu: Boolean = false,
    onReorder: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val haptics = rememberHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var marqueeEnabled by remember { mutableStateOf(false) }
    val countryText = station.country?.takeIf { it.isNotBlank() && it != "0" }
    val countryLine = countryText ?: "Unknown country"
    val tagsText = station.tags
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() && it != "0" }
        ?.takeIf { it.isNotEmpty() }
        ?.joinToString(", ")
    val tagsLine = if (tagsText.isNullOrBlank()) "Tags: —" else "Tags: $tagsText"
    val techParts = buildList {
        station.codec?.takeIf { it.isNotBlank() && it != "0" }?.let { add(it) }
        val bitrateValue = station.bitrate ?: 0
        if (bitrateValue > 0) add("$bitrateValue kbps")
        if (station.is_https == true) add("HTTPS")
    }
    val votesValue = station.votes ?: 0
    val votesLabel = if (votesValue > 0) "$votesValue votes" else null
    val detailParts = techParts.toMutableList().apply {
        if (votesLabel != null) add(votesLabel)
    }
    val techLine = if (detailParts.isEmpty()) "—" else detailParts.joinToString("  •  ")
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(500)
            marqueeEnabled = true
        } else {
            marqueeEnabled = false
        }
    }
    val statsModifier = if (marqueeEnabled) Modifier.basicMarquee() else Modifier
    val statsOverflow = if (marqueeEnabled) TextOverflow.Clip else TextOverflow.Ellipsis
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptics.soft()
                onPlay()
            },
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StationIcon(
                url = station.favicon,
                stationUuid = station.stationuuid,
                size = 44.dp,
                loadDelayMs = iconLoadDelayMs
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = station.name.ifBlank { "Unnamed station" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = countryLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = statsOverflow,
                    modifier = statsModifier
                )
                Text(
                    text = tagsLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = statsOverflow,
                    modifier = statsModifier
                )
                Text(
                    text = techLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = statsOverflow,
                    modifier = statsModifier
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            if (showPlaylistMenu) {
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = {
                        haptics.soft()
                        menuExpanded = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Playlists") },
                            onClick = {
                                menuExpanded = false
                                onAddToPlaylist()
                            }
                        )
                    DropdownMenuItem(
                        text = { Text("Reorder") },
                        onClick = {
                            menuExpanded = false
                            onReorder?.invoke()
                        },
                        enabled = onReorder != null
                    )
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        onClick = {
                            menuExpanded = false
                            onRemove?.invoke()
                        },
                        enabled = onRemove != null
                    )
                    }
                }
            } else {
                IconButton(onClick = {
                    haptics.soft()
                    onAddToPlaylist()
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun stationKey(station: Station): String {
    val url = (station.url_resolved ?: station.url_https ?: station.url ?: "").trim()
    return if (station.stationuuid.isNotBlank()) {
        "uuid:${station.stationuuid}"
    } else if (url.isNotBlank()) {
        "url:$url"
    } else {
        "uuid:"
    }
}

private fun formatFacetLabel(name: String, count: Int): String {
    return if (count > 0) "$name ($count)" else name
}

private fun Screen.toNavigationState(): NavigationState {
    return when (this) {
        Screen.Browse -> NavigationState(screen = ScreenType.Browse)
        Screen.Playlists -> NavigationState(screen = ScreenType.Playlists)
        Screen.NearMe -> NavigationState(screen = ScreenType.Browse)
        Screen.Config -> NavigationState(screen = ScreenType.Browse)
        is Screen.StationList -> NavigationState(
            screen = ScreenType.StationList,
            stationListTitle = title,
            stationFilters = filters
        )
        is Screen.PlaylistStations -> {
            val (type, id, name) = when (ref) {
                PlaylistRef.Favorites -> Triple(PlaylistRefType.Favorites, null, ref.label)
                PlaylistRef.Recents -> Triple(PlaylistRefType.Recents, null, ref.label)
                is PlaylistRef.User -> Triple(PlaylistRefType.User, ref.id, ref.name)
            }
            NavigationState(
                screen = ScreenType.PlaylistStations,
                playlistRefType = type,
                playlistId = id,
                playlistName = name
            )
        }
    }
}

private fun NavigationState.toScreen(store: PlaylistStore): Screen {
    return when (screen) {
        ScreenType.Browse -> Screen.Browse
        ScreenType.Playlists -> Screen.Playlists
        ScreenType.NearMe -> Screen.Browse
        ScreenType.Config -> Screen.Browse
        ScreenType.StationList -> {
            val title = stationListTitle?.takeIf { it.isNotBlank() }
            val filters = stationFilters
            if (title != null && filters != null) {
                Screen.StationList(title, filters)
            } else {
                Screen.Browse
            }
        }
        ScreenType.PlaylistStations -> {
            when (playlistRefType) {
                PlaylistRefType.Favorites ->
                    Screen.PlaylistStations(SystemPlaylistNames.FAVORITES, PlaylistRef.Favorites)
                PlaylistRefType.Recents ->
                    Screen.PlaylistStations(SystemPlaylistNames.RECENTS, PlaylistRef.Recents)
                PlaylistRefType.User -> {
                    val playlist = playlistId?.let { id ->
                        store.playlists.firstOrNull { it.id == id }
                    }
                    if (playlist != null) {
                        Screen.PlaylistStations(playlist.name, PlaylistRef.User(playlist.id, playlist.name))
                    } else {
                        Screen.Playlists
                    }
                }
                null -> Screen.Playlists
            }
        }
    }
}

private fun playlistNameError(
    name: String,
    reservedNames: Set<String>,
    existingNames: Set<String>
): String? {
    val normalized = normalizePlaylistName(name)
    return when {
        normalized in reservedNames -> "That name is reserved."
        normalized in existingNames -> "That name already exists."
        else -> null
    }
}

@Composable
fun PlaylistsScreen(
    modifier: Modifier,
    viewModel: PlaylistsViewModel,
    playbackViewModel: PlaybackViewModel,
    searchQuery: String,
    resetSignal: Int,
    onPlaylistSelected: (PlaylistRef, String) -> Unit
) {
    val store by viewModel.store.collectAsState()
    val query = searchQuery.trim()
    val haptics = rememberHaptics()
    val reservedNames = remember { SystemPlaylistNames.normalized }
    val existingNames = remember(store.playlists) {
        store.playlists.map { normalizePlaylistName(it.name) }.toSet()
    }

    val systemPlaylists = listOf(
        SystemPlaylistItem(PlaylistRef.Favorites, store.favorites, Icons.Filled.Favorite),
        SystemPlaylistItem(PlaylistRef.Recents, store.recents, Icons.Outlined.History)
    )

    val userPlaylists = store.playlists.map { playlist ->
        UserPlaylistItem(playlist, store.playlistStations[playlist.id].orEmpty())
    }.filter { item ->
        query.isBlank() || item.playlist.name.contains(query, ignoreCase = true)
    }

    val stationResults = remember(store.playlists, store.playlistStations, query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            val matches = LinkedHashMap<String, Station>()
            store.playlists.forEach { playlist ->
                val stations = store.playlistStations[playlist.id].orEmpty()
                stations.forEach { station ->
                    if (station.name.contains(query, ignoreCase = true)) {
                        matches.putIfAbsent(station.stationuuid, station)
                    }
                }
            }
            matches.values.toList()
        }
    }

    var showNewDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
    var reorderTarget by remember { mutableStateOf<Playlist?>(null) }
    var addDialogStation by remember { mutableStateOf<Station?>(null) }

    LaunchedEffect(resetSignal) {
        if (resetSignal == 0) return@LaunchedEffect
        showNewDialog = false
        renameTarget = null
        deleteTarget = null
        reorderTarget = null
    }

    LazyColumn(modifier = modifier) {
        if (query.isBlank()) {
            items(systemPlaylists) { item ->
                SystemPlaylistRow(
                    name = item.ref.label,
                    stations = item.stations,
                    icon = item.icon,
                    onClick = {
                        haptics.soft()
                        onPlaylistSelected(item.ref, item.ref.label)
                    }
                )
            }

            if (systemPlaylists.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }

        itemsIndexed(userPlaylists) { index, item ->
            PlaylistRow(
                name = item.playlist.name,
                stations = item.stations,
                showMenu = true,
                onClick = {
                    haptics.soft()
                    onPlaylistSelected(PlaylistRef.User(item.playlist.id, item.playlist.name), item.playlist.name)
                },
                onRename = { renameTarget = item.playlist },
                onDelete = { deleteTarget = item.playlist },
                onReorder = { reorderTarget = item.playlist },
                iconLoadDelayMs = imageLoadDelayForIndex(index)
            )
        }

        if (stationResults.isNotEmpty() && userPlaylists.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }

        if (stationResults.isNotEmpty()) {
            itemsIndexed(stationResults) { index, station ->
                StationRow(
                    station = station,
                    onPlay = { playbackViewModel.playStation(station) },
                    iconLoadDelayMs = imageLoadDelayForIndex(index + userPlaylists.size),
                    onAddToPlaylist = { addDialogStation = station }
                )
            }
        }

        if (query.isBlank()) {
            item {
                NewPlaylistRow(
                    onClick = {
                        haptics.soft()
                        showNewDialog = true
                    }
                )
            }
        }
    }

    if (showNewDialog) {
        PlaylistNameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            initialValue = "",
            onDismiss = { showNewDialog = false },
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    viewModel.createPlaylist(name)
                }
                showNewDialog = false
            },
            validateName = { input ->
                playlistNameError(input, reservedNames, existingNames)
            }
        )
    }

    renameTarget?.let { playlist ->
        val renameExistingNames = remember(store.playlists, playlist.id) {
            store.playlists
                .filter { it.id != playlist.id }
                .map { normalizePlaylistName(it.name) }
                .toSet()
        }
        PlaylistNameDialog(
            title = "Rename playlist",
            confirmLabel = "Save",
            initialValue = playlist.name,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    viewModel.renamePlaylist(playlist.id, name)
                }
                renameTarget = null
            },
            validateName = { input ->
                playlistNameError(input, reservedNames, renameExistingNames)
            }
        )
    }

    deleteTarget?.let { playlist ->
        ConfirmDeleteDialog(
            name = playlist.name,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.deletePlaylist(playlist.id)
                deleteTarget = null
            }
        )
    }

    reorderTarget?.let { playlist ->
        val index = store.playlists.indexOfFirst { it.id == playlist.id }
        val canMoveUp = index > 0
        val canMoveDown = index >= 0 && index < store.playlists.lastIndex
        ReorderDialog(
            name = playlist.name,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            onDismiss = { reorderTarget = null },
            onMoveUp = {
                if (canMoveUp) {
                    viewModel.movePlaylist(index, index - 1)
                }
            },
            onMoveDown = {
                if (canMoveDown) {
                    viewModel.movePlaylist(index, index + 1)
                }
            }
        )
    }

    addDialogStation?.let { station ->
        AddToPlaylistDialog(
            station = station,
            store = store,
            onDismiss = { addDialogStation = null },
            onToggleFavorite = { viewModel.toggleFavorite(station) },
            onCreatePlaylist = { name ->
                viewModel.createPlaylist(name)
            },
            onTogglePlaylist = { playlistId ->
                viewModel.toggleInPlaylist(playlistId, station)
            }
        )
    }
}

@Composable
fun PlaylistStationsScreen(
    modifier: Modifier,
    title: String,
    ref: PlaylistRef,
    viewModel: PlaylistsViewModel,
    playbackViewModel: PlaybackViewModel,
    searchQuery: String,
    onBack: () -> Unit
) {
    val store by viewModel.store.collectAsState()
    val haptics = rememberHaptics()
    val query = searchQuery.trim()
    val baseStations = when (ref) {
        PlaylistRef.Favorites -> store.favorites
        PlaylistRef.Recents -> store.recents
        is PlaylistRef.User -> store.playlistStations[ref.id].orEmpty()
    }
    val ordered = baseStations
    val filtered by produceState(initialValue = ordered, ordered, query) {
        value = withContext(Dispatchers.Default) {
            if (query.isBlank()) {
                ordered
            } else {
                ordered.filter { station ->
                    station.name.contains(query, ignoreCase = true)
                }
            }
        }
    }
    var addDialogStation by remember { mutableStateOf<Station?>(null) }
    var reorderTarget by remember { mutableStateOf<Station?>(null) }
    val reorderEnabled = query.isBlank()
    val baseIndexByKey = remember(baseStations) {
        baseStations.mapIndexed { index, station -> stationKey(station) to index }.toMap()
    }
    val moveStation: (Int, Int) -> Unit = { fromIndex, toIndex ->
        when (ref) {
            PlaylistRef.Favorites -> viewModel.moveStationInFavorites(fromIndex, toIndex)
            PlaylistRef.Recents -> viewModel.moveStationInRecents(fromIndex, toIndex)
            is PlaylistRef.User -> viewModel.moveStationInPlaylist(ref.id, fromIndex, toIndex)
        }
    }

    val listState = rememberLazyListState()
    var reachedStart by remember(filtered) { mutableStateOf(false) }
    var reachedEnd by remember(filtered) { mutableStateOf(false) }

    LaunchedEffect(filtered, listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val firstIndex = visibleItems.firstOrNull()?.index ?: 0
                val lastIndex = visibleItems.lastOrNull()?.index ?: -1
                val startNow = firstIndex == 0 && visibleItems.isNotEmpty()
                val endNow = lastIndex >= filtered.lastIndex && visibleItems.isNotEmpty()

                if (startNow && !reachedStart) {
                    haptics.strong()
                }
                if (endNow && !reachedEnd) {
                    haptics.strong()
                }
                reachedStart = startNow
                reachedEnd = endNow
            }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                haptics.soft()
                onBack()
            }) {
                Text("Back")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (filtered.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No stations yet")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                itemsIndexed(filtered) { index, station ->
                    StationRow(
                        station = station,
                        onPlay = { playbackViewModel.playStationFromPlaylist(station, filtered, index) },
                        iconLoadDelayMs = imageLoadDelayForIndex(index),
                        onAddToPlaylist = { addDialogStation = station },
                        showPlaylistMenu = true,
                        onReorder = if (reorderEnabled) {
                            { reorderTarget = station }
                        } else {
                            null
                        },
                        onRemove = {
                            when (ref) {
                                PlaylistRef.Favorites -> viewModel.toggleFavorite(station)
                                PlaylistRef.Recents -> viewModel.removeFromRecents(station)
                                is PlaylistRef.User -> viewModel.removeFromPlaylist(ref.id, station)
                            }
                        }
                    )
                }
            }
        }
    }

    addDialogStation?.let { station ->
        AddToPlaylistDialog(
            station = station,
            store = store,
            onDismiss = { addDialogStation = null },
            onToggleFavorite = { viewModel.toggleFavorite(station) },
            onCreatePlaylist = { name ->
                viewModel.createPlaylist(name)
            },
            onTogglePlaylist = { playlistId ->
                viewModel.toggleInPlaylist(playlistId, station)
            }
        )
    }

    val reorderIndex = reorderTarget?.let { baseIndexByKey[stationKey(it)] }
    if (reorderTarget != null && reorderIndex != null) {
        val station = reorderTarget!!
        val canMoveUp = reorderIndex > 0
        val canMoveDown = reorderIndex < baseStations.lastIndex
        ReorderStationDialog(
            name = station.name.ifBlank { "Station" },
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            onDismiss = { reorderTarget = null },
            onMoveUp = {
                if (canMoveUp) {
                    moveStation(reorderIndex, reorderIndex - 1)
                }
            },
            onMoveDown = {
                if (canMoveDown) {
                    moveStation(reorderIndex, reorderIndex + 1)
                }
            }
        )
    }

}

private data class SystemPlaylistItem(
    val ref: PlaylistRef,
    val stations: List<Station>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private data class UserPlaylistItem(
    val playlist: Playlist,
    val stations: List<Station>
)

@Composable
private fun PlaylistRow(
    name: String,
    stations: List<Station>,
    showMenu: Boolean,
    onClick: () -> Unit,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onReorder: (() -> Unit)? = null,
    iconLoadDelayMs: Long = 0L
) {
    val haptics = rememberHaptics()
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistIcon(
                stations = stations,
                size = 44.dp,
                loadDelayMs = iconLoadDelayMs
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (stations.isEmpty()) "Empty" else "${stations.size} stations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showMenu) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptics.soft()
                            menuExpanded = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuExpanded = false
                            onRename?.invoke()
                        }
                    )
                    if (onReorder != null) {
                        DropdownMenuItem(
                            text = { Text("Reorder") },
                            onClick = {
                                menuExpanded = false
                                onReorder()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDelete?.invoke()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemPlaylistRow(
    name: String,
    stations: List<Station>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (stations.isEmpty()) "Empty" else "${stations.size} stations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NewPlaylistRow(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "New playlist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Create a new list",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlaylistIcon(
    stations: List<Station>,
    size: Dp,
    loadDelayMs: Long
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Radio,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    validateName: (String) -> String?
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    val haptics = rememberHaptics()
    val trimmed = value.trim()
    val error = if (trimmed.isBlank()) null else validateName(trimmed)
    val canConfirm = trimmed.isNotBlank() && error == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    haptics.soft()
                    onConfirm(value)
                },
                enabled = canConfirm
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptics.soft()
                onDismiss()
            }) { Text("Cancel") }
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    name: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val haptics = rememberHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete playlist") },
        text = { Text("Delete \"$name\"? This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = {
                haptics.soft()
                onConfirm()
            }) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptics.soft()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ReorderDialog(
    name: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onDismiss: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val haptics = rememberHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reorder \"$name\"") },
        text = { Text("Move this playlist in the list.") },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        haptics.soft()
                        onMoveUp()
                    },
                    enabled = canMoveUp
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Move up"
                    )
                }
                IconButton(
                    onClick = {
                        haptics.soft()
                        onMoveDown()
                    },
                    enabled = canMoveDown
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Move down"
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptics.soft()
                onDismiss()
            }) { Text("Done") }
        }
    )
}

@Composable
private fun ReorderStationDialog(
    name: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onDismiss: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val haptics = rememberHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reorder \"$name\"") },
        text = { Text("Move this station in the playlist.") },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        haptics.soft()
                        onMoveUp()
                    },
                    enabled = canMoveUp
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Move up"
                    )
                }
                IconButton(
                    onClick = {
                        haptics.soft()
                        onMoveDown()
                    },
                    enabled = canMoveDown
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Move down"
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptics.soft()
                onDismiss()
            }) { Text("Done") }
        }
    )
}

@Composable
private fun AddToPlaylistDialog(
    station: Station,
    store: PlaylistStore,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onTogglePlaylist: (String) -> Unit
) {
    val haptics = rememberHaptics()
    val isFavorite = store.favorites.any { it.stationuuid == station.stationuuid }
    var showNewDialog by remember { mutableStateOf(false) }
    val reservedNames = remember { SystemPlaylistNames.normalized }
    val existingNames = remember(store.playlists) {
        store.playlists.map { normalizePlaylistName(it.name) }.toSet()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                PlaylistDialogRow(
                    name = SystemPlaylistNames.FAVORITES,
                    isMember = isFavorite,
                    onClick = {
                        haptics.soft()
                        onToggleFavorite()
                    }
                )
                if (store.playlists.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    store.playlists.forEach { playlist ->
                        val inPlaylist = store.playlistStations[playlist.id]
                            .orEmpty()
                            .any { it.stationuuid == station.stationuuid }
                        PlaylistDialogRow(
                            name = playlist.name,
                            isMember = inPlaylist,
                            onClick = {
                                haptics.soft()
                                onTogglePlaylist(playlist.id)
                            }
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No playlists yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                PlaylistDialogRow(
                    name = "New Playlist",
                    isMember = false,
                    onClick = {
                        haptics.soft()
                        showNewDialog = true
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )

    if (showNewDialog) {
        PlaylistNameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            initialValue = "",
            onDismiss = { showNewDialog = false },
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    onCreatePlaylist(name)
                }
                showNewDialog = false
            },
            validateName = { input ->
                playlistNameError(input, reservedNames, existingNames)
            }
        )
    }
}

@Composable
private fun PlaylistDialogRow(
    name: String,
    isMember: Boolean,
    onClick: () -> Unit
) {
    val color = if (isMember) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier.weight(1f)
        )
        if (isMember) {
            Text(
                text = "Added",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SearchTopBar(
    query: String,
    showLoading: Boolean,
    isError: Boolean,
    progress: Float?,
    onQueryChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = searchFocused) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarPadding)
        ) {
            val showProgress = showLoading || isError || progress != null
            if (showProgress) {
                val indicatorColor = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.BottomStart),
                        color = indicatorColor
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.BottomStart),
                        color = indicatorColor
                    )
                }
            }
        }
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search station names") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .onFocusChanged { focus -> searchFocused = focus.isFocused },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun FilterDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }
    var textFieldHeightPx by remember { mutableStateOf(0) }
    val textFieldHeight = with(density) { textFieldHeightPx.toDp() }
    var textFieldWidthPx by remember { mutableStateOf(0) }
    val textFieldWidth = with(density) { textFieldWidthPx.toDp() }
    val dropdownWidthModifier = if (textFieldWidth > 0.dp) {
        Modifier.width(textFieldWidth)
    } else {
        Modifier.fillMaxWidth()
    }
    val query = value.trim()
    val canShowMenu = query.isNotBlank()
    val normalizedOptions = remember(options) {
        options.map { option -> option to option.lowercase() }
    }
    val normalizedOptionSet = remember(normalizedOptions) {
        normalizedOptions.map { it.second }.toSet()
    }
    val queryLower = remember(query) { query.lowercase() }
    val matches = remember(normalizedOptions, queryLower, canShowMenu) {
        if (!canShowMenu) {
            emptyList()
        } else {
            normalizedOptions.filter { it.second.contains(queryLower) }
        }
    }
    val filtered = remember(matches) { matches.take(FILTER_DROPDOWN_MAX_ITEMS).map { it.first } }
    val hasValue = value.isNotBlank()
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused, value, normalizedOptionSet) {
        if (!isFocused) {
            val trimmed = value.trim()
            if (trimmed.isNotBlank() && trimmed.lowercase() !in normalizedOptionSet) {
                onValueChange("")
            }
        }
    }

    Box(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = it.isNotBlank()
            },
            placeholder = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .onGloballyPositioned { coordinates ->
                    textFieldHeightPx = coordinates.size.height
                    textFieldWidthPx = coordinates.size.width
                }
                .onFocusChanged { focus ->
                    isFocused = focus.isFocused
                    if (!focus.isFocused) {
                        expanded = false
                    } else if (value.isNotBlank()) {
                        expanded = true
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
        if (hasValue) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = textFieldHeight)
                    .pointerInput(value) {
                        detectTapGestures(
                            onTap = {
                                onValueChange("")
                                expanded = false
                                focusManager.clearFocus()
                            }
                        )
                    }
            )
        }
        if (expanded && canShowMenu) {
            val dropdownSpacingPx = with(density) { 4.dp.roundToPx() }
            val positionProvider = remember(dropdownSpacingPx) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset {
                        val preferredY = anchorBounds.top - popupContentSize.height - dropdownSpacingPx
                        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
                        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)

                        return IntOffset(
                            x = anchorBounds.left.coerceIn(0, maxX),
                            y = preferredY.coerceIn(0, maxY)
                        )
                    }
                }
            }
            Popup(
                popupPositionProvider = positionProvider,
                properties = PopupProperties(focusable = false, clippingEnabled = false)
            ) {
                Surface(
                    modifier = dropdownWidthModifier,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shadowElevation = 14.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    )
                ) {
                    if (filtered.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No matches") },
                            onClick = { expanded = false },
                            enabled = false
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            filtered.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        onOptionSelected(option)
                                        expanded = false
                                        focusManager.clearFocus()
                                    }
                                )
                                if (index < filtered.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val FILTER_DROPDOWN_MAX_ITEMS = 4

private fun toTitleCase(value: String): String {
    if (value.isBlank()) return value
    return value.split(' ').joinToString(" ") { part ->
        val lower = part.lowercase()
        lower.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }
}

@Composable
fun ConfigTopBar() {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarPadding)
        )
        Text(
            text = "Config",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
fun ConfigScreen(
    modifier: Modifier,
    uiState: DatabaseUiState,
    playlistsViewModel: PlaylistsViewModel,
    onRebuildDatabase: () -> Unit,
    onAllowBackgroundMediaServiceChange: (Boolean) -> Unit
) {
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var transferError by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    val statusText = when (uiState.status) {
        DatabaseStatus.Ready -> "Database ready."
        DatabaseStatus.Empty -> "Database is empty."
        DatabaseStatus.Error -> "Database error."
        DatabaseStatus.Building -> "Database rebuild in progress."
    }
    val statusColor = if (uiState.status == DatabaseStatus.Error) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isExporting = true
            transferError = null
            try {
                val result = playlistsViewModel.exportPlaylists()
                writeTextToUri(context, uri, result.json)
            } catch (e: Exception) {
                transferError = "Export failed: ${e.message ?: "Unknown error"}"
            } finally {
                isExporting = false
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingImportUri = uri
        showImportDialog = true
    }

    LaunchedEffect(transferError) {
        val message = transferError ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        transferError = null
    }

    val statusTone = when (uiState.status) {
        DatabaseStatus.Ready -> MaterialTheme.colorScheme.primary
        DatabaseStatus.Building -> MaterialTheme.colorScheme.secondary
        DatabaseStatus.Empty -> MaterialTheme.colorScheme.outline
        DatabaseStatus.Error -> MaterialTheme.colorScheme.error
    }
    val isRebuildInProgress = uiState.isRebuilding || uiState.status == DatabaseStatus.Building
    val showOnlyDatabase = isRebuildInProgress ||
        uiState.status == DatabaseStatus.Empty ||
        uiState.status == DatabaseStatus.Error
    val versionLabel = remember {
        runCatching {
            val pm = context.packageManager
            val pkg = context.packageName
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
            val name = info.versionName?.takeIf { it.isNotBlank() } ?: "0.0.0"
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            "Version $name ($code)"
        }.getOrElse { "Version unknown" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Config",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        ConfigSectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Database",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Live catalog cache",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(text = statusText, tone = statusTone)
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (uiState.isRebuilding) {
                val progress = uiState.progress
                val stationProgressMax = 50_000
                val stationProgress = if (
                    progress?.phase == DatabaseRebuildPhase.DownloadingStations &&
                    progress.stationCount <= stationProgressMax
                ) {
                    (progress.stationCount.toFloat() / stationProgressMax).coerceIn(0f, 1f)
                } else {
                    null
                }
                if (stationProgress != null) {
                    LinearProgressIndicator(
                        progress = { stationProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                progress?.let { status ->
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (status.stationCount > 0) {
                        Text(
                            text = "Stations downloaded: ${status.stationCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                onClick = {
                    haptics.strong()
                    onRebuildDatabase()
                },
                enabled = !uiState.isRebuilding,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rebuild database from web")
            }
            Text(
                text = "Downloads the full station catalog and rebuilds the local database used by the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "This can take several minutes and use significant data. Recommended on Wi-Fi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "This only has to be done once to get started.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            uiState.summary?.let { summary ->
                val updatedLabel = remember(summary.lastUpdated) {
                    if (summary.lastUpdated > 0L) {
                        java.text.DateFormat.getDateTimeInstance().format(java.util.Date(summary.lastUpdated))
                    } else {
                        "Unknown"
                    }
                }
                Text(
                    text = "Database stats",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                SummaryRow(label = "Stations", value = summary.stationCount.toString())
                SummaryRow(label = "Countries", value = summary.countryCount.toString())
                SummaryRow(label = "Tags", value = summary.tagCount.toString())
                SummaryRow(label = "Languages", value = summary.languageCount.toString())
                SummaryRow(label = "Last updated", value = updatedLabel)
            }
        }

        if (!showOnlyDatabase) {
            ConfigSectionCard {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Backup, move, or restore your playlists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = {
                            haptics.soft()
                            exportLauncher.launch("openair-playlists.json")
                        },
                        enabled = !isExporting && !isImporting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isExporting) "Exporting..." else "Export")
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.soft()
                            importLauncher.launch(arrayOf("application/json", "text/*"))
                        },
                        enabled = !isExporting && !isImporting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Outlined.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isImporting) "Importing..." else "Import")
                    }
                }
                Text(
                    text = "Exports include playlist order, favorites, and recents.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Import can merge into existing playlists or replace everything.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ConfigSectionCard {
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "How OpenAir handles your data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExpandableInfoBox(
                    title = "Read more",
                    body = "OpenAir does not collect or transmit telemetry data.\n\n" +
                        "Location operations are fully local. Distance sorting runs on-device, and your location is not uploaded by OpenAir.\n\n" +
                        "When you play a station, your device connects directly to that station's stream server.\n\n" +
                        "If system backup is enabled on your device, playlist data may be included in backups."
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allow background media service",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Keep playback service alive after the app UI is closed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = uiState.allowBackgroundMediaService,
                        onCheckedChange = { enabled ->
                            haptics.soft()
                            onAllowBackgroundMediaServiceChange(enabled)
                        }
                    )
                }
            }

            ConfigSectionCard {
                Text(
                    text = "Help",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Quick explanations and troubleshooting tips.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExpandableInfoBox(
                    title = "Read more",
                    body = "OpenAir: A lightweight radio browser and player built around the Radio Browser catalog.\n\n" +
                        "Database rebuild: If Browse shows only Config or search feels empty, rebuild the database from the web.\n\n" +
                        "Local search: Search and filters run on the local database for speed. Use Rebuild to refresh.\n\n" +
                        "Playlists backup: Export keeps playlists, favorites, and recents. Import can merge or replace.\n\n" +
                        versionLabel
                )
                TextButton(onClick = { uriHandler.openUri("https://github.com/tc3107/OpenAir") }) {
                    Text("github.com/tc3107/OpenAir")
                }
            }

            ConfigSectionCard {
                Text(
                    text = "Support development",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "OpenAir is free and open source.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "If you find it useful, you can support development via Ko-fi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledTonalButton(onClick = { uriHandler.openUri("https://ko-fi.com/tc3107") }) {
                        Text("Support on Ko-fi")
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                pendingImportUri = null
            },
            title = { Text("Import playlists") },
            text = {
                Text("Choose how to apply this import. Merge keeps your current playlists and adds new items. Replace overwrites everything.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingImportUri
                        if (uri == null) {
                            showImportDialog = false
                            return@TextButton
                        }
                        haptics.soft()
                        showImportDialog = false
                        scope.launch {
                            isImporting = true
                            transferError = null
                            try {
                                val payload = readTextFromUri(context, uri)
                                val result = playlistsViewModel.importPlaylists(payload, PlaylistImportMode.Merge)
                                if (!result.success) {
                                    transferError = result.message
                                }
                            } catch (e: Exception) {
                                transferError = "Import failed: ${e.message ?: "Unknown error"}"
                            } finally {
                                isImporting = false
                                pendingImportUri = null
                            }
                        }
                    }
                ) {
                    Text("Merge")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val uri = pendingImportUri
                        if (uri == null) {
                            showImportDialog = false
                            return@TextButton
                        }
                        haptics.soft()
                        showImportDialog = false
                        scope.launch {
                            isImporting = true
                            transferError = null
                            try {
                                val payload = readTextFromUri(context, uri)
                                val result = playlistsViewModel.importPlaylists(payload, PlaylistImportMode.Replace)
                                if (!result.success) {
                                    transferError = result.message
                                }
                            } catch (e: Exception) {
                                transferError = "Import failed: ${e.message ?: "Unknown error"}"
                            } finally {
                                isImporting = false
                                pendingImportUri = null
                            }
                        }
                    }
                ) {
                    Text("Replace")
                }
            }
        )
    }
}

@Composable
private fun ConfigSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun StatusChip(text: String, tone: Color) {
    Surface(
        color = tone.copy(alpha = 0.12f),
        contentColor = tone,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ExpandableInfoBox(
    title: String,
    body: String
) {
    var expanded by rememberSaveable(title, body) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                )
            }
        }
    }
}

private suspend fun readTextFromUri(context: Context, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Unable to read import file.")
    }
}

private suspend fun writeTextToUri(context: Context, uri: Uri, text: String) {
    withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
            ?: throw IllegalStateException("Unable to write export file.")
    }
}

@Composable
fun PlayerBar(
    playbackViewModel: PlaybackViewModel,
    playlistsViewModel: PlaylistsViewModel
) {
    val state by playbackViewModel.state.collectAsState()
    if (state.currentStation == null) {
        return
    }
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val playlistStore by playlistsViewModel.store.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val station = state.currentStation
    val infoParts = buildList {
        station?.country?.takeIf { it.isNotBlank() }?.let { add(it) }
        val bitrateValue = station?.bitrate ?: 0
        if (bitrateValue > 0) add("$bitrateValue kbps")
        val votesValue = station?.votes ?: 0
    if (votesValue > 0) add("$votesValue votes")
    }

    LaunchedEffect(station?.stationuuid, station?.favicon) {
        val url = station?.favicon
        if (!url.isNullOrBlank()) {
            val request = buildStationIconRequest(
                context = context,
                url = url,
                stationUuid = station?.stationuuid
            )
            context.imageLoader.enqueue(request)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
        state.error?.let { error ->
            PlaybackErrorBanner(
                error = error,
                onDismiss = { playbackViewModel.dismissError() }
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StationIcon(
                    url = station?.favicon,
                    stationUuid = station?.stationuuid,
                    size = 44.dp,
                    loadDelayMs = 0L
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station?.name ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = if (state.isPlaying) Modifier.basicMarquee() else Modifier
                    )
                    if (infoParts.isNotEmpty()) {
                        Text(
                            text = infoParts.joinToString("  •  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = if (state.isPlaying) Modifier.basicMarquee() else Modifier
                        )
                    }
                }
                IconButton(onClick = {
                    haptics.soft()
                    showAddDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = {
                        haptics.strong()
                        playbackViewModel.togglePlayback()
                    },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val icon = when {
                        state.isBuffering -> Icons.Outlined.PlayArrow
                        state.isPlaying -> Icons.Filled.Pause
                        else -> Icons.Filled.PlayArrow
                    }
                    Icon(imageVector = icon, contentDescription = null)
                }
            }
        }
    }

    if (showAddDialog && station != null) {
        AddToPlaylistDialog(
            station = station,
            store = playlistStore,
            onDismiss = { showAddDialog = false },
            onToggleFavorite = { playlistsViewModel.toggleFavorite(station) },
            onCreatePlaylist = { name ->
                playlistsViewModel.createPlaylist(name)
            },
            onTogglePlaylist = { playlistId ->
                playlistsViewModel.toggleInPlaylist(playlistId, station)
            }
        )
    }
}

@Composable
private fun PlaybackErrorBanner(
    error: PlaybackError,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = error.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss error"
                )
            }
        }
    }
}

@Composable
private fun StationIcon(
    url: String?,
    stationUuid: String?,
    size: Dp,
    loadDelayMs: Long
) {
    val shape = RoundedCornerShape(12.dp)
    val context = LocalContext.current
    if (url.isNullOrBlank()) {
        StationIconPlaceholder(size, showLoading = false)
        return
    }
    var request by remember(url, stationUuid) { mutableStateOf<ImageRequest?>(null) }
    LaunchedEffect(url, stationUuid, loadDelayMs) {
        if (loadDelayMs > 0L) {
            delay(loadDelayMs)
        }
        request = buildStationIconRequest(
            context = context,
            url = url,
            stationUuid = stationUuid,
            crossfade = true
        )
    }

    if (request == null) {
        StationIconPlaceholder(size, showLoading = true)
        return
    }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(shape),
        contentScale = ContentScale.Crop
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> StationIconPlaceholder(size, showLoading = true)
            is AsyncImagePainter.State.Error -> StationIconPlaceholder(size, showLoading = false)
            else -> SubcomposeAsyncImageContent()
        }
    }
}

private fun buildStationIconRequest(
    context: Context,
    url: String,
    stationUuid: String?,
    crossfade: Boolean = false
): ImageRequest {
    val normalizedUuid = stationUuid?.trim().orEmpty()
    val request = ImageRequest.Builder(context)
        .data(url)
    if (crossfade) {
        request.crossfade(true)
    }
    if (normalizedUuid.isNotEmpty()) {
        val cacheKey = "station_icon_$normalizedUuid"
        request.diskCacheKey(cacheKey)
        request.memoryCacheKey(cacheKey)
    }
    return request.build()
}

@Composable
private fun StationIconPlaceholder(
    size: Dp,
    showLoading: Boolean
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (showLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Radio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun imageLoadDelayForIndex(index: Int): Long {
    val bucket = index / 8
    return (bucket * 60L).coerceAtMost(600L)
}
