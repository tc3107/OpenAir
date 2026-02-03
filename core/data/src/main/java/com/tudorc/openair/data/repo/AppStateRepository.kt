package com.tudorc.openair.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tudorc.openair.data.model.Station
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.appStateDataStore by preferencesDataStore("openair_app_state")

class AppStateRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val lastStationKey = stringPreferencesKey("last_station_json")
    private val navigationKey = stringPreferencesKey("navigation_state_json")

    suspend fun readLastStation(): Station? {
        val prefs = context.appStateDataStore.data.first()
        val payload = prefs[lastStationKey] ?: return null
        return runCatching { json.decodeFromString(Station.serializer(), payload) }.getOrNull()
    }

    suspend fun saveLastStation(station: Station?) {
        context.appStateDataStore.edit { prefs ->
            if (station == null) {
                prefs.remove(lastStationKey)
            } else {
                prefs[lastStationKey] = json.encodeToString(Station.serializer(), station)
            }
        }
    }

    suspend fun readNavigationState(): NavigationState? {
        val prefs = context.appStateDataStore.data.first()
        val payload = prefs[navigationKey] ?: return null
        return runCatching { json.decodeFromString(NavigationState.serializer(), payload) }.getOrNull()
    }

    suspend fun saveNavigationState(state: NavigationState) {
        val payload = json.encodeToString(NavigationState.serializer(), state)
        context.appStateDataStore.edit { prefs ->
            prefs[navigationKey] = payload
        }
    }
}
