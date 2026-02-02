package com.example.openair.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openair.data.repo.DatabaseRebuildState
import com.example.openair.data.repo.DatabaseStatus
import com.example.openair.data.repo.DatabaseSummary
import com.example.openair.data.repo.DatabaseRebuildProgress
import com.example.openair.data.repo.RadioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class DatabaseUiState(
    val status: DatabaseStatus = DatabaseStatus.Empty,
    val summary: DatabaseSummary? = null,
    val isRebuilding: Boolean = false,
    val progress: DatabaseRebuildProgress? = null,
    val errorMessage: String? = null
)

class ConfigViewModel(
    private val repository: RadioRepository
) : ViewModel() {
    private val rebuildState = MutableStateFlow<DatabaseRebuildState>(DatabaseRebuildState.Idle)
    private var rebuildJob: Job? = null
    private val statusLoaded = MutableStateFlow(false)

    val isStatusLoaded: StateFlow<Boolean> = statusLoaded

    private val statusFlow = repository.observeDatabaseStatus()
        .onEach { statusLoaded.value = true }

    val uiState: StateFlow<DatabaseUiState> = combine(
        statusFlow,
        repository.observeDatabaseSummary(),
        rebuildState
    ) { status, summary, rebuild ->
        val isRebuilding = rebuild is DatabaseRebuildState.Running
        val progress = (rebuild as? DatabaseRebuildState.Running)?.progress
        val rebuildError = (rebuild as? DatabaseRebuildState.Error)?.message
        val errorMessage = rebuildError ?: summary?.lastError
        DatabaseUiState(
            status = status,
            summary = summary,
            isRebuilding = isRebuilding,
            progress = progress,
            errorMessage = if (status == DatabaseStatus.Error) errorMessage else rebuildError
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DatabaseUiState())

    fun rebuildDatabase() {
        if (rebuildJob?.isActive == true) return
        rebuildJob = viewModelScope.launch {
            repository.rebuildDatabase().collect { update ->
                rebuildState.value = update
            }
        }
    }
}
