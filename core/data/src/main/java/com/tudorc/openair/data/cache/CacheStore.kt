package com.tudorc.openair.data.cache

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("openair_cache")

class CacheStore(
    private val context: Context,
    private val json: Json
) {
    suspend fun <T> getOrFetch(
        key: String,
        ttlMillis: Long,
        serializer: KSerializer<T>,
        fetch: suspend () -> T
    ): T {
        val now = System.currentTimeMillis()
        val cached = readCache(key, serializer, now, ttlMillis)
        if (cached != null) {
            return cached
        }
        val fresh = fetch()
        writeCache(key, serializer, fresh, now)
        return fresh
    }

    suspend fun <T> readCache(
        key: String,
        serializer: KSerializer<T>,
        now: Long = System.currentTimeMillis(),
        ttlMillis: Long
    ): T? {
        val jsonKey = stringPreferencesKey("${key}_json")
        val timeKey = longPreferencesKey("${key}_ts")
        val prefs = context.dataStore.data.first()
        val jsonValue = prefs[jsonKey] ?: return null
        val ts = prefs[timeKey] ?: return null
        if (now - ts > ttlMillis) {
            return null
        }
        return runCatching { json.decodeFromString(serializer, jsonValue) }.getOrNull()
    }

    suspend fun <T> readCacheAnyAge(
        key: String,
        serializer: KSerializer<T>
    ): T? {
        val jsonKey = stringPreferencesKey("${key}_json")
        val prefs = context.dataStore.data.first()
        val jsonValue = prefs[jsonKey] ?: return null
        return runCatching { json.decodeFromString(serializer, jsonValue) }.getOrNull()
    }

    suspend fun <T> writeCache(
        key: String,
        serializer: KSerializer<T>,
        value: T,
        now: Long = System.currentTimeMillis()
    ) {
        val jsonKey = stringPreferencesKey("${key}_json")
        val timeKey = longPreferencesKey("${key}_ts")
        val payload = json.encodeToString(serializer, value)
        context.dataStore.edit { prefs ->
            prefs[jsonKey] = payload
            prefs[timeKey] = now
        }
    }
}
