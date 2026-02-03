package com.tudorc.openair.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tudorc.openair.data.repo.AppStateRepository
import com.tudorc.openair.data.repo.PlaylistRepository
import com.tudorc.openair.data.repo.RadioRepository
import com.tudorc.openair.player.PlaybackViewModel

class AppViewModelFactory(
    private val app: Application,
    private val repository: RadioRepository,
    private val playlistRepository: PlaylistRepository,
    private val appStateRepository: AppStateRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(BrowseViewModel::class.java) ->
                BrowseViewModel(repository) as T
            modelClass.isAssignableFrom(StationListViewModel::class.java) ->
                StationListViewModel(repository) as T
            modelClass.isAssignableFrom(NearMeViewModel::class.java) ->
                NearMeViewModel(repository) as T
            modelClass.isAssignableFrom(PlaybackViewModel::class.java) ->
                PlaybackViewModel(app, repository, playlistRepository, appStateRepository) as T
            modelClass.isAssignableFrom(PlaylistsViewModel::class.java) ->
                PlaylistsViewModel(playlistRepository) as T
            modelClass.isAssignableFrom(ConfigViewModel::class.java) ->
                ConfigViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
