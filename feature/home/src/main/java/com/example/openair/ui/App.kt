package com.example.openair.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import coil.request.ImageRequest
import coil.imageLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import com.example.openair.data.model.Country
import com.example.openair.data.model.Language
import com.example.openair.data.model.Playlist
import com.example.openair.data.model.PlaylistStore
import com.example.openair.data.model.Station
import com.example.openair.data.model.SystemPlaylistNames
import com.example.openair.data.model.Tag
import com.example.openair.data.model.normalizePlaylistName
import com.example.openair.data.repo.AppStateRepository
import com.example.openair.data.repo.NavigationState
import com.example.openair.data.repo.PlaylistRefType
import com.example.openair.data.repo.PlaylistRepository
import com.example.openair.data.repo.RadioRepository
import com.example.openair.data.repo.ScreenType
import com.example.openair.data.repo.SearchFilters
import com.example.openair.player.PlaybackViewModel
import java.util.UUID

sealed class Screen {
    data object Browse : Screen()
    data object Playlists : Screen()
    data object NearMe : Screen()
    data class StationList(val title: String, val filters: SearchFilters) : Screen()
    data class PlaylistStations(val title: String, val ref: PlaylistRef) : Screen()
}

sealed class PlaylistRef(val label: String) {
    data object Favorites : PlaylistRef(SystemPlaylistNames.FAVORITES)
    data object Recents : PlaylistRef(SystemPlaylistNames.RECENTS)
    data object Custom : PlaylistRef(SystemPlaylistNames.CUSTOM)
    data class User(val id: String, val name: String) : PlaylistRef(name)
}

enum class BrowseMode(val label: String) {
    Countries("Countries"),
    Tags("Tags"),
    Languages("Languages")
}

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

    var screen by remember { mutableStateOf<Screen>(Screen.Browse) }
    var navRestored by remember { mutableStateOf(false) }
    var navLoaded by remember { mutableStateOf(false) }
    var pendingNavigation by remember { mutableStateOf<NavigationState?>(null) }
    var browseQuery by rememberSaveable { mutableStateOf("") }
    var playlistsQuery by rememberSaveable { mutableStateOf("") }
    var stationListQuery by rememberSaveable { mutableStateOf("") }
    var playlistStationsQuery by rememberSaveable { mutableStateOf("") }
    var browseMode by rememberSaveable { mutableStateOf(BrowseMode.Countries) }
    var resumeBrowseAfterStationLoad by remember { mutableStateOf(false) }
    val playlistStore by playlistsViewModel.store.collectAsState()
    val browseLoading by browseViewModel.isLoading.collectAsState()
    val stationListLoading by stationListViewModel.isLoading.collectAsState()
    val stationListRetrying by stationListViewModel.isRetrying.collectAsState()
    val stationListLoaded by stationListViewModel.hasLoaded.collectAsState()
    val nearMeFetching by nearMeViewModel.isFetching.collectAsState()
    val nearMeComputing by nearMeViewModel.isComputing.collectAsState()
    val playbackState by playbackViewModel.state.collectAsState()
    val currentTab = when (screen) {
        Screen.Browse, is Screen.StationList -> Screen.Browse
        Screen.Playlists, is Screen.PlaylistStations -> Screen.Playlists
        Screen.NearMe -> Screen.NearMe
    }
    val currentQuery = when (screen) {
        Screen.Browse -> browseQuery
        Screen.Playlists -> playlistsQuery
        Screen.NearMe -> ""
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

    LaunchedEffect(pendingNavigation, playlistStore, navLoaded, navRestored) {
        if (!navLoaded || navRestored) return@LaunchedEffect
        pendingNavigation?.let { saved ->
            screen = saved.toScreen(playlistStore)
        }
        navRestored = true
    }

    LaunchedEffect(screen, navRestored) {
        if (navRestored) {
            appStateRepository.saveNavigationState(screen.toNavigationState())
        }
    }

    LaunchedEffect(resumeBrowseAfterStationLoad, stationListLoaded) {
        if (resumeBrowseAfterStationLoad && stationListLoaded) {
            browseViewModel.resumeDeferredLoad()
            resumeBrowseAfterStationLoad = false
        }
    }

    val topBarLoading = when (screen) {
        Screen.Browse -> browseLoading
        Screen.NearMe -> nearMeFetching || nearMeComputing
        is Screen.StationList -> stationListLoading || stationListRetrying
        Screen.Playlists, is Screen.PlaylistStations -> false
    } || playbackState.isBuffering
    val topBarError = screen is Screen.StationList && stationListRetrying

    Scaffold(
        topBar = {
            SearchTopBar(
                query = currentQuery,
                showLoading = topBarLoading,
                isError = topBarError,
                onQueryChange = { value ->
                    when (screen) {
                        Screen.Browse -> browseQuery = value
                        Screen.Playlists -> playlistsQuery = value
                        Screen.NearMe -> {}
                        is Screen.StationList -> stationListQuery = value
                        is Screen.PlaylistStations -> playlistStationsQuery = value
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = dismissKeyboardOnTap) {
                PlayerBar(playbackViewModel, playlistsViewModel)
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == Screen.Browse,
                        onClick = {
                            haptics.soft()
                            browseQuery = ""
                            screen = Screen.Browse
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
                            playlistsQuery = ""
                            screen = Screen.Playlists
                        },
                        label = { Text("Playlists") },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                                contentDescription = null
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = currentTab == Screen.NearMe,
                        onClick = {
                            haptics.soft()
                            screen = Screen.NearMe
                        },
                        label = { Text("Near Me") },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Place,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        val contentModifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .then(dismissKeyboardOnTap)
        when (val current = screen) {
            Screen.Browse -> BrowseScreen(
                modifier = contentModifier,
                viewModel = browseViewModel,
                searchQuery = browseQuery,
                onClearSearch = { browseQuery = "" },
                selectedMode = browseMode,
                onModeChange = { browseMode = it },
                onFacetSelected = { title, filters ->
                    val deferred = browseViewModel.deferCurrentLoad()
                    resumeBrowseAfterStationLoad = deferred
                    stationListQuery = ""
                    screen = Screen.StationList(title, filters)
                }
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
                searchQuery = playlistsQuery,
                onPlaylistSelected = { ref, title ->
                    playlistStationsQuery = ""
                    screen = Screen.PlaylistStations(title, ref)
                }
            )

            Screen.NearMe -> NearMeScreen(
                modifier = contentModifier,
                viewModel = nearMeViewModel,
                playbackViewModel = playbackViewModel,
                playlistsViewModel = playlistsViewModel
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

@Composable
fun BrowseScreen(
    modifier: Modifier,
    viewModel: BrowseViewModel,
    searchQuery: String,
    onClearSearch: () -> Unit,
    selectedMode: BrowseMode,
    onModeChange: (BrowseMode) -> Unit,
    onFacetSelected: (String, SearchFilters) -> Unit
) {
    val countries by viewModel.countries.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val languages by viewModel.languages.collectAsState()
    val error by viewModel.error.collectAsState()
    val haptics = rememberHaptics()

    val query = searchQuery.trim()

    LaunchedEffect(selectedMode) {
        viewModel.loadMode(selectedMode)
    }

    Column(modifier = modifier) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val modes = BrowseMode.values()
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = selectedMode == mode,
                    onClick = {
                        haptics.strong()
                        onModeChange(mode)
                        onClearSearch()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size)
                ) {
                    Text(mode.label)
                }
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        when (selectedMode) {
            BrowseMode.Countries -> {
                val filtered = countries
                    .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                FacetList(
                    items = filtered,
                    modifier = Modifier.fillMaxSize(),
                    pageSize = 25,
                    onSelect = { country ->
                        val filters = SearchFilters(
                            country = country.name,
                            countryExact = true,
                            order = "votes",
                            reverse = true,
                            hidebroken = true
                        )
                        onFacetSelected("Country: ${country.name}", filters)
                    }
                )
            }

            BrowseMode.Tags -> {
                val filtered = tags
                    .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                FacetList(
                    items = filtered,
                    modifier = Modifier.fillMaxSize(),
                    pageSize = 25,
                    onSelect = { tag ->
                        val filters = SearchFilters(
                            tag = tag.name,
                            tagExact = true,
                            order = "votes",
                            reverse = true,
                            hidebroken = true
                        )
                        onFacetSelected("Tag: ${tag.name}", filters)
                    }
                )
            }

            BrowseMode.Languages -> {
                val filtered = languages
                    .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                FacetList(
                    items = filtered,
                    modifier = Modifier.fillMaxSize(),
                    pageSize = 25,
                    onSelect = { language ->
                        val filters = SearchFilters(
                            language = language.name,
                            order = "votes",
                            reverse = true,
                            hidebroken = true
                        )
                        onFacetSelected("Language: ${language.name}", filters)
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
    val filtered = if (query.isBlank()) {
        stations
    } else {
        stations.filter { station ->
            station.name.contains(query, ignoreCase = true) ||
                (station.tags?.contains(query, ignoreCase = true) == true)
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
    onAddToPlaylist: () -> Unit
) {
    val haptics = rememberHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var marqueeEnabled by remember { mutableStateOf(false) }
    val countryText = station.country?.takeIf { it.isNotBlank() && it != "0" } ?: "Unknown country"
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
    val isCustomStation = station.stationuuid.startsWith("custom_")
    val votesLabel = when {
        isCustomStation -> "Custom"
        votesValue > 0 -> "$votesValue votes"
        else -> null
    }
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
                    val isCustomStation = station.stationuuid.startsWith("custom_")
                }
                Text(
                    text = countryText,
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

private fun formatFacetLabel(name: String, count: Int): String {
    return if (count > 0) "$name ($count)" else name
}

private fun Screen.toNavigationState(): NavigationState {
    return when (this) {
        Screen.Browse -> NavigationState(screen = ScreenType.Browse)
        Screen.Playlists -> NavigationState(screen = ScreenType.Playlists)
        Screen.NearMe -> NavigationState(screen = ScreenType.NearMe)
        is Screen.StationList -> NavigationState(
            screen = ScreenType.StationList,
            stationListTitle = title,
            stationFilters = filters
        )
        is Screen.PlaylistStations -> {
            val (type, id, name) = when (ref) {
                PlaylistRef.Favorites -> Triple(PlaylistRefType.Favorites, null, ref.label)
                PlaylistRef.Recents -> Triple(PlaylistRefType.Recents, null, ref.label)
                PlaylistRef.Custom -> Triple(PlaylistRefType.Custom, null, ref.label)
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
        ScreenType.NearMe -> Screen.NearMe
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
                PlaylistRefType.Custom ->
                    Screen.PlaylistStations(SystemPlaylistNames.CUSTOM, PlaylistRef.Custom)
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
    searchQuery: String,
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
        SystemPlaylistItem(PlaylistRef.Recents, store.recents, Icons.Outlined.History),
        SystemPlaylistItem(PlaylistRef.Custom, store.custom, Icons.AutoMirrored.Outlined.QueueMusic)
    ).filter { item ->
        query.isBlank() || item.ref.label.contains(query, ignoreCase = true)
    }

    val userPlaylists = store.playlists.map { playlist ->
        UserPlaylistItem(playlist, store.playlistStations[playlist.id].orEmpty())
    }.filter { item ->
        query.isBlank() || item.playlist.name.contains(query, ignoreCase = true)
    }

    var showNewDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
    var reorderTarget by remember { mutableStateOf<Playlist?>(null) }

    LazyColumn(modifier = modifier) {
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

        item {
            NewPlaylistRow(
                onClick = {
                    haptics.soft()
                    showNewDialog = true
                }
            )
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
        PlaylistRef.Custom -> store.custom
        is PlaylistRef.User -> store.playlistStations[ref.id].orEmpty()
    }
    val sorted = when (ref) {
        PlaylistRef.Recents, PlaylistRef.Custom -> baseStations
        else -> baseStations.sortedWith(compareByDescending<Station> { it.votes ?: 0 })
    }
    val filtered = if (query.isBlank()) {
        sorted
    } else {
        sorted.filter { station ->
            station.name.contains(query, ignoreCase = true) ||
                (station.tags?.contains(query, ignoreCase = true) == true)
        }
    }
    var addDialogStation by remember { mutableStateOf<Station?>(null) }
    var showCustomDialog by remember { mutableStateOf(false) }

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
        if (ref == PlaylistRef.Custom) {
            Button(
                onClick = {
                    haptics.soft()
                    showCustomDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("Add custom station")
            }
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
                        onPlay = { playbackViewModel.playStation(station) },
                        iconLoadDelayMs = imageLoadDelayForIndex(index),
                        onAddToPlaylist = { addDialogStation = station }
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

    if (showCustomDialog) {
        CustomStationDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { name, url, location ->
                val trimmedName = name.trim()
                val trimmedUrl = url.trim()
                val trimmedLocation = location.trim()
                val station = Station(
                    stationuuid = "custom_${UUID.randomUUID()}",
                    name = trimmedName,
                    country = trimmedLocation.ifBlank { null },
                    url = trimmedUrl,
                    url_resolved = trimmedUrl,
                    url_https = if (trimmedUrl.startsWith("https://")) trimmedUrl else null
                )
                viewModel.addCustomStation(station)
                showCustomDialog = false
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
private fun CustomStationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    val haptics = rememberHaptics()
    val trimmedName = name.trim()
    val trimmedUrl = url.trim()
    val urlValid = trimmedUrl.toHttpUrlOrNull() != null
    val canConfirm = trimmedName.isNotBlank() && urlValid
    val error = when {
        trimmedName.isBlank() -> "Name is required."
        trimmedUrl.isBlank() -> "URL is required."
        !urlValid -> "Enter a valid http(s) URL."
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add custom station") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Station name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                TextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("Stream URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    isError = error != null && !urlValid,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                TextField(
                    value = location,
                    onValueChange = { location = it },
                    placeholder = { Text("Location (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
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
                    onConfirm(trimmedName, trimmedUrl, location)
                },
                enabled = canConfirm
            ) {
                Text("Add")
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
    onQueryChange: (String) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarPadding)
        ) {
            if (showLoading || isError) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomStart),
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp
    )
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
