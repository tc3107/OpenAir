package com.example.openair.data.repo

import android.content.Context
import com.example.openair.data.cache.CacheStore
import com.example.openair.data.model.Country
import com.example.openair.data.model.CountryCode
import com.example.openair.data.model.Language
import com.example.openair.data.model.NearMeCacheEntry
import com.example.openair.data.model.Station
import com.example.openair.data.model.Tag
import com.example.openair.data.network.MirrorManager
import com.example.openair.data.network.RadioBrowserApi
import com.example.openair.data.network.ServerDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import android.os.Build
import android.util.Log
import android.content.pm.PackageManager
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import java.security.MessageDigest

class RadioRepository(context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val userAgent = buildUserAgent(context)

    private val discovery = ServerDiscovery(client, json, userAgent)
    private val mirrors = MirrorManager(discovery)
    private val api = RadioBrowserApi(client, json, mirrors, userAgent)
    private val cache = CacheStore(context, json)

    private val ttlMillis = 24L * 60L * 60L * 1000L
    private val nearMeTtlMillis = 7L * 24L * 60L * 60L * 1000L

    suspend fun getCountries(): List<Country> =
        fetchAndCache("countries", ListSerializer(Country.serializer())) {
            api.getCountries()
        }.also { Log.i(LOG_TAG, "Countries fetched: ${it.size}") }

    suspend fun getCountryCodes(): List<CountryCode> =
        withContext(Dispatchers.IO) {
            cache.getOrFetch("countrycodes", ttlMillis, ListSerializer(CountryCode.serializer())) {
                api.getCountryCodes()
            }
        }.also { Log.i(LOG_TAG, "Country codes fetched: ${it.size}") }

    suspend fun getLanguages(): List<Language> =
        fetchAndCache("languages", ListSerializer(Language.serializer())) {
            api.getLanguages()
        }.also { Log.i(LOG_TAG, "Languages fetched: ${it.size}") }

    suspend fun getTags(): List<Tag> =
        fetchAndCache("tags", ListSerializer(Tag.serializer())) {
            api.getTags()
        }.also { Log.i(LOG_TAG, "Tags fetched: ${it.size}") }

    suspend fun searchStations(filters: SearchFilters, offset: Int, limit: Int): List<Station> {
        val params = filters.toParams(offset, limit)
        val result = retryNetwork { api.searchStations(params) }
        val sorted = withContext(Dispatchers.Default) {
            result.sortedWith(
                compareByDescending<Station> { it.votes ?: 0 }
                    .thenByDescending { it.lastcheckok ?: 0 }
            )
        }
        Log.i(LOG_TAG, "Search results: ${result.size} offset=$offset limit=$limit params=$params")
        cacheLastSearch(sorted)
        cacheSearchResult(params, sorted)
        return sorted
    }

    suspend fun getStationsWithGeo(): List<Station> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Station>()
            var offset = 0
            val limit = 1000
            while (true) {
                val params = SearchFilters(hasGeoInfo = true).toParams(offset, limit)
                val batch = retryNetwork { api.searchStations(params) }
                if (batch.isEmpty()) break
                results.addAll(batch)
                if (batch.size < limit) break
                offset += batch.size
            }
            Log.i(LOG_TAG, "Geo stations fetched: ${results.size}")
            results
        }
    }

    suspend fun readNearMeCache(): List<NearMeCacheEntry>? {
        return withContext(Dispatchers.IO) {
            cache.readCache(
                "near_me",
                ListSerializer(NearMeCacheEntry.serializer()),
                ttlMillis = nearMeTtlMillis
            )
        }
    }

    suspend fun writeNearMeCache(items: List<NearMeCacheEntry>) {
        withContext(Dispatchers.IO) {
            cache.writeCache("near_me", ListSerializer(NearMeCacheEntry.serializer()), items)
        }
    }

    suspend fun readLastSearch(): List<Station>? {
        return withContext(Dispatchers.IO) {
            cache.readCache(
                "last_search",
                ListSerializer(Station.serializer()),
                ttlMillis = ttlMillis
            )
        }
    }

    suspend fun readSearchCache(filters: SearchFilters, offset: Int, limit: Int): List<Station>? {
        val params = filters.toParams(offset, limit)
        return withContext(Dispatchers.IO) {
            cache.readCacheAnyAge(
                searchCacheKey(params),
                ListSerializer(Station.serializer())
            )
        }
    }

    suspend fun readCountriesCache(): List<Country>? =
        withContext(Dispatchers.IO) {
            cache.readCacheAnyAge("countries", ListSerializer(Country.serializer()))
        }

    suspend fun readTagsCache(): List<Tag>? =
        withContext(Dispatchers.IO) {
            cache.readCacheAnyAge("tags", ListSerializer(Tag.serializer()))
        }

    suspend fun readLanguagesCache(): List<Language>? =
        withContext(Dispatchers.IO) {
            cache.readCacheAnyAge("languages", ListSerializer(Language.serializer()))
        }

    suspend fun getStationByUuid(uuid: String): Station? {
        return api.getStationByUuid(uuid).firstOrNull()
    }

    suspend fun resolvePlaybackUrls(uuid: String): List<String> = withContext(Dispatchers.IO) {
        val urls = mutableListOf<String>()
        val playable = runCatching { api.getPlayableUrl(uuid) }.getOrNull()
        if (!playable.isNullOrBlank()) {
            urls.add(playable)
        }
        val station = runCatching { getStationByUuid(uuid) }.getOrNull()
        listOf(
            station?.url_resolved,
            station?.url_https,
            station?.url
        ).forEach { candidate ->
            if (!candidate.isNullOrBlank()) {
                urls.add(candidate)
            }
        }
        val distinct = urls.distinct()
        Log.i(LOG_TAG, "Resolved playback urls for $uuid: $distinct")
        distinct
    }

    suspend fun vote(uuid: String) {
        api.vote(uuid)
    }

    private suspend fun cacheLastSearch(stations: List<Station>) {
        if (stations.isEmpty()) return
        withContext(Dispatchers.IO) {
            cache.writeCache("last_search", ListSerializer(Station.serializer()), stations)
        }
    }

    private suspend fun cacheSearchResult(params: Map<String, String>, stations: List<Station>) {
        if (stations.isEmpty()) return
        withContext(Dispatchers.IO) {
            cache.writeCache(
                searchCacheKey(params),
                ListSerializer(Station.serializer()),
                stations
            )
        }
    }

    private fun searchCacheKey(params: Map<String, String>): String {
        val canonical = params.toSortedMap()
            .entries
            .joinToString("&") { "${it.key}=${it.value}" }
        val digest = MessageDigest.getInstance("MD5")
            .digest(canonical.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "search_$digest"
    }

    private suspend fun <T> fetchAndCache(
        key: String,
        serializer: KSerializer<T>,
        fetch: suspend () -> T
    ): T {
        return withContext(Dispatchers.IO) {
            val fresh = retryNetwork { fetch() }
            cache.writeCache(key, serializer, fresh)
            fresh
        }
    }

    private suspend fun <T> retryNetwork(
        attempts: Int = 2,
        initialDelayMillis: Long = 300L,
        block: suspend () -> T
    ): T {
        var lastError: Exception? = null
        var delayMillis = initialDelayMillis
        repeat(attempts) { attempt ->
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: com.example.openair.data.network.ApiException) {
                if (!e.isServerError) throw e
                lastError = e
            } catch (e: IOException) {
                lastError = e
            }
            if (attempt < attempts - 1) {
                delay(delayMillis)
                delayMillis = (delayMillis * 2).coerceAtMost(2000L)
            }
        }
        throw lastError ?: IllegalStateException("Network request failed")
    }
}

private const val LOG_TAG = "OpenAirRadio"

private fun buildUserAgent(context: Context): String {
    val version = runCatching {
        val pm = context.packageManager
        val pkg = context.packageName
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0)
        }
        info.versionName
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "0.0.0"
    return "OpenAir/v$version (Android; https://github.com/tc3107/OpenAir)"
}

@Serializable
data class SearchFilters(
    val name: String? = null,
    val nameExact: Boolean? = null,
    val country: String? = null,
    val countryExact: Boolean? = null,
    val countrycode: String? = null,
    val state: String? = null,
    val language: String? = null,
    val tag: String? = null,
    val tagExact: Boolean? = null,
    val codec: String? = null,
    val bitrateMin: Int? = null,
    val bitrateMax: Int? = null,
    val hasGeoInfo: Boolean? = null,
    val isHttps: Boolean? = null,
    val order: String? = null,
    val reverse: Boolean? = null,
    val hidebroken: Boolean? = true
) {
    fun toParams(offset: Int, limit: Int): Map<String, String> {
        val params = mutableMapOf<String, String>()
        name?.let { params["name"] = it }
        nameExact?.let { params["nameExact"] = it.toString() }
        country?.let { params["country"] = it }
        countryExact?.let { params["countryExact"] = it.toString() }
        countrycode?.let { params["countrycode"] = it }
        state?.let { params["state"] = it }
        language?.let { params["language"] = it }
        tag?.let { params["tag"] = it }
        tagExact?.let { params["tagExact"] = it.toString() }
        codec?.let { params["codec"] = it }
        bitrateMin?.let { params["bitrateMin"] = it.toString() }
        bitrateMax?.let { params["bitrateMax"] = it.toString() }
        hasGeoInfo?.let { params["has_geo_info"] = it.toString() }
        isHttps?.let { params["is_https"] = it.toString() }
        order?.let { params["order"] = it }
        reverse?.let { params["reverse"] = it.toString() }
        hidebroken?.let { params["hidebroken"] = it.toString() }
        params["offset"] = offset.toString()
        params["limit"] = limit.toString()
        return params
    }
}
