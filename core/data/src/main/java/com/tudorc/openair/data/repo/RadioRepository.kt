package com.tudorc.openair.data.repo

import android.content.Context
import com.tudorc.openair.data.cache.CacheStore
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.tudorc.openair.data.db.DatabaseMetaEntity
import com.tudorc.openair.data.db.OpenAirDatabase
import com.tudorc.openair.data.db.toEntity
import com.tudorc.openair.data.db.toModel
import com.tudorc.openair.data.model.Country
import com.tudorc.openair.data.model.CountryCode
import com.tudorc.openair.data.model.Language
import com.tudorc.openair.data.model.NearMeCacheEntry
import com.tudorc.openair.data.model.Station
import com.tudorc.openair.data.model.Tag
import com.tudorc.openair.data.network.MirrorManager
import com.tudorc.openair.data.network.ApiException
import com.tudorc.openair.data.network.RadioBrowserApi
import com.tudorc.openair.data.network.ServerDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val database = OpenAirDatabase.getInstance(context)
    private val stationDao = database.stationDao()
    private val countryDao = database.countryDao()
    private val countryCodeDao = database.countryCodeDao()
    private val languageDao = database.languageDao()
    private val tagDao = database.tagDao()
    private val metaDao = database.metaDao()
    private val rebuildMutex = Mutex()

    private val ttlMillis = 24L * 60L * 60L * 1000L
    private val nearMeTtlMillis = 7L * 24L * 60L * 60L * 1000L
    private val stationPageLimit = 1000

    fun observeDatabaseSummary(): Flow<DatabaseSummary?> {
        return metaDao.observeMeta().map { meta ->
            meta?.toSummary()
        }
    }

    fun observeDatabaseStatus(): Flow<DatabaseStatus> {
        return metaDao.observeMeta().map { meta ->
            when {
                meta == null -> DatabaseStatus.Empty
                meta.status == META_STATUS_BUILDING -> DatabaseStatus.Building
                meta.status == META_STATUS_READY && meta.stationCount > 0 -> DatabaseStatus.Ready
                meta.status == META_STATUS_ERROR -> DatabaseStatus.Error
                meta.stationCount <= 0 -> DatabaseStatus.Empty
                else -> DatabaseStatus.Error
            }
        }
    }

    suspend fun getCountries(): List<Country> =
        withContext(Dispatchers.IO) {
            countryDao.getAll().map { it.toModel() }
        }

    suspend fun getCountryCodes(): List<CountryCode> =
        withContext(Dispatchers.IO) {
            countryCodeDao.getAll().map { it.toModel() }
        }

    suspend fun getLanguages(): List<Language> =
        withContext(Dispatchers.IO) {
            languageDao.getAll().map { it.toModel() }
        }

    suspend fun getTags(): List<Tag> =
        withContext(Dispatchers.IO) {
            tagDao.getAll().map { it.toModel() }
        }

    suspend fun searchStations(filters: SearchFilters, offset: Int, limit: Int): List<Station> {
        val query = buildStationSearchQuery(filters, offset, limit)
        val result = withContext(Dispatchers.IO) {
            stationDao.searchStations(query).map { it.toModel() }
        }
        Log.i(LOG_TAG, "Search results: ${result.size} offset=$offset limit=$limit filters=$filters")
        cacheLastSearch(result)
        cacheSearchResult(filters.toParams(offset, limit), result)
        return result
    }

    suspend fun countStations(filters: SearchFilters): Int {
        return withContext(Dispatchers.IO) {
            stationDao.countStations(buildStationCountQuery(filters))
        }
    }

    suspend fun getStationsWithGeo(): List<Station> {
        return withContext(Dispatchers.IO) {
            val results = stationDao.getStationsWithGeo().map { it.toModel() }
            Log.i(LOG_TAG, "Geo stations fetched from db: ${results.size}")
            results
        }
    }

    suspend fun getAllStations(): List<Station> {
        return withContext(Dispatchers.IO) {
            val results = stationDao.getAllStations().map { it.toModel() }
            Log.i(LOG_TAG, "All stations fetched from db: ${results.size}")
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
        return withContext(Dispatchers.IO) {
            stationDao.searchStations(buildStationSearchQuery(filters, offset, limit))
                .map { it.toModel() }
        }
    }

    suspend fun readCountriesCache(): List<Country>? =
        withContext(Dispatchers.IO) {
            countryDao.getAll().map { it.toModel() }
        }

    suspend fun readTagsCache(): List<Tag>? =
        withContext(Dispatchers.IO) {
            tagDao.getAll().map { it.toModel() }
        }

    suspend fun readLanguagesCache(): List<Language>? =
        withContext(Dispatchers.IO) {
            languageDao.getAll().map { it.toModel() }
        }

    suspend fun getStationByUuid(uuid: String): Station? {
        val local = withContext(Dispatchers.IO) { stationDao.getStationByUuid(uuid) }
        if (local != null) return local.toModel()
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

    fun rebuildDatabase(): Flow<DatabaseRebuildState> = flow {
        if (!rebuildMutex.tryLock()) {
            emit(DatabaseRebuildState.Error("Database rebuild already running."))
            return@flow
        }
        val startTime = System.currentTimeMillis()
        try {
            suspend fun emitProgress(
                phase: DatabaseRebuildPhase,
                message: String,
                stationCount: Int = 0,
                page: Int = 0
            ) {
                emit(
                    DatabaseRebuildState.Running(
                        DatabaseRebuildProgress(
                            phase = phase,
                            message = message,
                            stationCount = stationCount,
                            page = page
                        )
                    )
                )
            }

            fun isRetryable(error: Exception): Boolean {
                return when (error) {
                    is IOException -> true
                    is ApiException -> error.isServerError
                    else -> false
                }
            }

            suspend fun <T> retryWithBackoff(
                phase: DatabaseRebuildPhase,
                baseMessage: String,
                stationCount: Int = 0,
                page: Int = 0,
                block: suspend () -> T
            ): T {
                var attempt = 0
                var delayMillis = 1000L
                while (true) {
                    try {
                        return block()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (!isRetryable(e)) throw e
                        attempt += 1
                        val wait = delayMillis.coerceAtMost(30_000L)
                        emitProgress(
                            phase = phase,
                            message = "$baseMessage (network error, retrying in ${wait / 1000}s)",
                            stationCount = stationCount,
                            page = page
                        )
                        runCatching { mirrors.refresh() }
                        delay(wait)
                        delayMillis = (delayMillis * 2).coerceAtMost(30_000L)
                        if (attempt >= 30) {
                            throw e
                        }
                    }
                }
            }

            val existingMeta = metaDao.getMeta()
            val buildingMeta = (existingMeta ?: DatabaseMetaEntity(
                id = 1,
                status = META_STATUS_BUILDING,
                stationCount = 0,
                countryCount = 0,
                tagCount = 0,
                languageCount = 0,
                lastUpdated = 0L,
                lastDurationMillis = 0L,
                lastError = null
            )).copy(status = META_STATUS_BUILDING, lastError = null)
            metaDao.upsert(buildingMeta)
            emitProgress(DatabaseRebuildPhase.Starting, "Starting rebuild...")
            emitProgress(DatabaseRebuildPhase.FetchingMetadata, "Fetching countries, tags, and languages...")

            val countries = retryWithBackoff(
                phase = DatabaseRebuildPhase.FetchingMetadata,
                baseMessage = "Fetching countries"
            ) { api.getCountries() }
            val tags = retryWithBackoff(
                phase = DatabaseRebuildPhase.FetchingMetadata,
                baseMessage = "Fetching tags"
            ) { api.getTags() }
            val languages = retryWithBackoff(
                phase = DatabaseRebuildPhase.FetchingMetadata,
                baseMessage = "Fetching languages"
            ) { api.getLanguages() }
            val countryCodes = retryWithBackoff(
                phase = DatabaseRebuildPhase.FetchingMetadata,
                baseMessage = "Fetching country codes"
            ) { api.getCountryCodes() }

            database.withTransaction {
                countryDao.clearAll()
                tagDao.clearAll()
                languageDao.clearAll()
                countryCodeDao.clearAll()
                countryDao.insertAll(countries.map { it.toEntity() })
                tagDao.insertAll(tags.map { it.toEntity() })
                languageDao.insertAll(languages.map { it.toEntity() })
                countryCodeDao.insertAll(countryCodes.map { it.toEntity() })
            }

            emitProgress(
                phase = DatabaseRebuildPhase.DownloadingStations,
                message = "Downloading stations...",
                stationCount = 0,
                page = 0
            )

            stationDao.clearStations()
            var offset = 0
            var total = 0
            var page = 0
            while (true) {
                val params = SearchFilters(
                    order = "name",
                    reverse = false,
                    hidebroken = true
                ).toParams(offset, stationPageLimit)
                val batch = retryWithBackoff(
                    phase = DatabaseRebuildPhase.DownloadingStations,
                    baseMessage = "Downloading stations (page ${page + 1})",
                    stationCount = total,
                    page = page + 1
                ) { api.searchStations(params) }
                if (batch.isEmpty()) break
                val entities = batch.map { it.toEntity() }
                stationDao.insertStations(entities)
                total += entities.size
                offset += entities.size
                page += 1
                emitProgress(
                    phase = DatabaseRebuildPhase.DownloadingStations,
                    message = "Downloaded $total stations...",
                    stationCount = total,
                    page = page
                )
            }

            emitProgress(DatabaseRebuildPhase.Finalizing, "Finalizing database...")

            val duration = System.currentTimeMillis() - startTime
            val meta = DatabaseMetaEntity(
                id = 1,
                status = META_STATUS_READY,
                stationCount = total,
                countryCount = countries.size,
                tagCount = tags.size,
                languageCount = languages.size,
                lastUpdated = System.currentTimeMillis(),
                lastDurationMillis = duration,
                lastError = null
            )
            metaDao.upsert(meta)
            emit(DatabaseRebuildState.Success(meta.toSummary()))
        } catch (e: CancellationException) {
            val message = "Database rebuild canceled."
            val errorMeta = DatabaseMetaEntity(
                id = 1,
                status = META_STATUS_ERROR,
                stationCount = 0,
                countryCount = 0,
                tagCount = 0,
                languageCount = 0,
                lastUpdated = System.currentTimeMillis(),
                lastDurationMillis = System.currentTimeMillis() - startTime,
                lastError = message
            )
            metaDao.upsert(errorMeta)
            emit(DatabaseRebuildState.Error(message))
            throw e
        } catch (e: Exception) {
            val message = e.message ?: "Database rebuild failed."
            val errorMeta = DatabaseMetaEntity(
                id = 1,
                status = META_STATUS_ERROR,
                stationCount = 0,
                countryCount = 0,
                tagCount = 0,
                languageCount = 0,
                lastUpdated = System.currentTimeMillis(),
                lastDurationMillis = System.currentTimeMillis() - startTime,
                lastError = message
            )
            metaDao.upsert(errorMeta)
            emit(DatabaseRebuildState.Error(message))
        } finally {
            rebuildMutex.unlock()
        }
    }.flowOn(Dispatchers.IO)

    private fun buildStationSearchQuery(
        filters: SearchFilters,
        offset: Int,
        limit: Int
    ): SimpleSQLiteQuery {
        val sql = StringBuilder("SELECT * FROM stations")
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Any>()

        val name = filters.name?.trim().orEmpty()
        if (name.isNotBlank()) {
            if (filters.nameExact == true) {
                conditions += "name_normalized = ?"
                args += name.lowercase()
            } else {
                conditions += "name_normalized LIKE ?"
                val value = "%${name.lowercase()}%"
                args += value
            }
        }

        val country = filters.country?.trim().orEmpty()
        if (country.isNotBlank()) {
            if (filters.countryExact == true) {
                conditions += "country_normalized = ?"
                args += country.lowercase()
            } else {
                conditions += "country_normalized LIKE ?"
                args += "%${country.lowercase()}%"
            }
        }

        val countryCode = filters.countrycode?.trim().orEmpty()
        if (countryCode.isNotBlank()) {
            conditions += "countrycode = ?"
            args += countryCode.uppercase()
        }

        val state = filters.state?.trim().orEmpty()
        if (state.isNotBlank()) {
            conditions += "state_normalized LIKE ?"
            args += "%${state.lowercase()}%"
        }

        val language = filters.language?.trim().orEmpty()
        if (language.isNotBlank()) {
            conditions += "language_normalized LIKE ?"
            args += "%${language.lowercase()}%"
        }

        val tag = filters.tag?.trim().orEmpty()
        if (tag.isNotBlank()) {
            val normalizedTag = tag.lowercase()
            if (filters.tagExact == true) {
                conditions += "(" +
                    "tags_normalized = ? OR " +
                    "tags_normalized LIKE ? OR " +
                    "tags_normalized LIKE ? OR " +
                    "tags_normalized LIKE ?" +
                    ")"
                args += normalizedTag
                args += "$normalizedTag,%"
                args += "%,$normalizedTag,%"
                args += "%,$normalizedTag"
            } else {
                conditions += "tags_normalized LIKE ?"
                args += "%$normalizedTag%"
            }
        }

        val codec = filters.codec?.trim().orEmpty()
        if (codec.isNotBlank()) {
            conditions += "codec = ?"
            args += codec
        }

        val votesMin = filters.votesMin
        if (votesMin != null) {
            conditions += "votes >= ?"
            args += votesMin
        }

        val bitrateMin = filters.bitrateMin
        val bitrateMax = filters.bitrateMax
        if (bitrateMin != null) {
            conditions += "bitrate >= ?"
            args += bitrateMin
        }
        if (bitrateMax != null) {
            conditions += "bitrate <= ?"
            args += bitrateMax
        }

        if (filters.hasGeoInfo == true) {
            conditions += "geo_lat IS NOT NULL AND geo_long IS NOT NULL"
        }

        filters.isHttps?.let { isHttps ->
            conditions += "is_https = ?"
            args += if (isHttps) 1 else 0
        }

        if (filters.hidebroken == true) {
            conditions += "lastcheckok IS NULL OR lastcheckok != 0"
        }

        if (conditions.isNotEmpty()) {
            sql.append(" WHERE ")
            sql.append(conditions.joinToString(" AND "))
        }

        val orderColumn = when (filters.order) {
            "name" -> "name_normalized"
            "votes" -> "votes"
            "bitrate" -> "bitrate"
            "clickcount" -> "clickcount"
            "lastcheckok" -> "lastcheckok"
            "country" -> "country_normalized"
            "state" -> "state_normalized"
            "language" -> "language_normalized"
            else -> null
        }
        if (orderColumn != null) {
            val direction = if (filters.reverse == true) "DESC" else "ASC"
            sql.append(" ORDER BY $orderColumn $direction")
        } else {
            sql.append(" ORDER BY name_normalized ASC")
        }

        sql.append(" LIMIT ? OFFSET ?")
        args += limit
        args += offset
        return SimpleSQLiteQuery(sql.toString(), args.toTypedArray())
    }

    private fun buildStationCountQuery(filters: SearchFilters): SimpleSQLiteQuery {
        val sql = StringBuilder("SELECT COUNT(*) FROM stations")
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Any>()

        val name = filters.name?.trim().orEmpty()
        if (name.isNotBlank()) {
            if (filters.nameExact == true) {
                conditions += "name_normalized = ?"
                args += name.lowercase()
            } else {
                conditions += "name_normalized LIKE ?"
                val value = "%${name.lowercase()}%"
                args += value
            }
        }

        val country = filters.country?.trim().orEmpty()
        if (country.isNotBlank()) {
            if (filters.countryExact == true) {
                conditions += "country_normalized = ?"
                args += country.lowercase()
            } else {
                conditions += "country_normalized LIKE ?"
                args += "%${country.lowercase()}%"
            }
        }

        val countryCode = filters.countrycode?.trim().orEmpty()
        if (countryCode.isNotBlank()) {
            conditions += "countrycode = ?"
            args += countryCode.uppercase()
        }

        val state = filters.state?.trim().orEmpty()
        if (state.isNotBlank()) {
            conditions += "state_normalized LIKE ?"
            args += "%${state.lowercase()}%"
        }

        val language = filters.language?.trim().orEmpty()
        if (language.isNotBlank()) {
            conditions += "language_normalized LIKE ?"
            args += "%${language.lowercase()}%"
        }

        val tag = filters.tag?.trim().orEmpty()
        if (tag.isNotBlank()) {
            val normalizedTag = tag.lowercase()
            if (filters.tagExact == true) {
                conditions += "(" +
                    "tags_normalized = ? OR " +
                    "tags_normalized LIKE ? OR " +
                    "tags_normalized LIKE ? OR " +
                    "tags_normalized LIKE ?" +
                    ")"
                args += normalizedTag
                args += "$normalizedTag,%"
                args += "%,$normalizedTag,%"
                args += "%,$normalizedTag"
            } else {
                conditions += "tags_normalized LIKE ?"
                args += "%$normalizedTag%"
            }
        }

        val codec = filters.codec?.trim().orEmpty()
        if (codec.isNotBlank()) {
            conditions += "codec = ?"
            args += codec
        }

        val votesMin = filters.votesMin
        if (votesMin != null) {
            conditions += "votes >= ?"
            args += votesMin
        }

        val bitrateMin = filters.bitrateMin
        val bitrateMax = filters.bitrateMax
        if (bitrateMin != null) {
            conditions += "bitrate >= ?"
            args += bitrateMin
        }
        if (bitrateMax != null) {
            conditions += "bitrate <= ?"
            args += bitrateMax
        }

        if (filters.hasGeoInfo == true) {
            conditions += "geo_lat IS NOT NULL AND geo_long IS NOT NULL"
        }

        filters.isHttps?.let { isHttps ->
            conditions += "is_https = ?"
            args += if (isHttps) 1 else 0
        }

        if (filters.hidebroken == true) {
            conditions += "lastcheckok IS NULL OR lastcheckok != 0"
        }

        if (conditions.isNotEmpty()) {
            sql.append(" WHERE ")
            sql.append(conditions.joinToString(" AND "))
        }

        return SimpleSQLiteQuery(sql.toString(), args.toTypedArray())
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
            } catch (e: com.tudorc.openair.data.network.ApiException) {
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
private const val META_STATUS_READY = "ready"
private const val META_STATUS_BUILDING = "building"
private const val META_STATUS_ERROR = "error"

private fun DatabaseMetaEntity.toSummary(): DatabaseSummary {
    return DatabaseSummary(
        stationCount = stationCount,
        countryCount = countryCount,
        tagCount = tagCount,
        languageCount = languageCount,
        lastUpdated = lastUpdated,
        lastDurationMillis = lastDurationMillis,
        lastError = lastError
    )
}

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
    val votesMin: Int? = null,
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
        votesMin?.let { params["votesMin"] = it.toString() }
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
