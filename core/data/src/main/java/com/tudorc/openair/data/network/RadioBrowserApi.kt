package com.tudorc.openair.data.network

import com.tudorc.openair.data.model.Country
import com.tudorc.openair.data.model.CountryCode
import com.tudorc.openair.data.model.Language
import com.tudorc.openair.data.model.Station
import com.tudorc.openair.data.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log

class RadioBrowserApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val mirrors: MirrorManager,
    private val userAgent: String
) {
    suspend fun getCountries(): List<Country> =
        getList("/json/countries", ListSerializer(Country.serializer()))

    suspend fun getCountryCodes(): List<CountryCode> =
        getList("/json/countrycodes", ListSerializer(CountryCode.serializer()))

    suspend fun getLanguages(): List<Language> =
        getList("/json/languages", ListSerializer(Language.serializer()))

    suspend fun getTags(): List<Tag> =
        getList("/json/tags", ListSerializer(Tag.serializer()))

    suspend fun searchStations(params: Map<String, String>): List<Station> =
        getList("/json/stations/search", ListSerializer(Station.serializer()), params)

    suspend fun getStationByUuid(uuid: String): List<Station> =
        getList("/json/stations/byuuid/$uuid", ListSerializer(Station.serializer()))

    suspend fun getPlayableUrl(uuid: String): String? {
        return mirrors.withMirrors { baseUrl ->
            val url = buildUrl(baseUrl, "/json/url/$uuid", emptyMap())
            val body = executeGet(url)
            parsePlayableUrl(body)
        }
    }

    suspend fun vote(uuid: String) {
        mirrors.withMirrors { baseUrl ->
            val url = buildUrl(baseUrl, "/json/vote/$uuid", emptyMap())
            executeGet(url)
        }
    }

    private suspend fun <T> getList(
        path: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        params: Map<String, String> = emptyMap()
    ): T {
        return mirrors.withMirrors { baseUrl ->
            val url = buildUrl(baseUrl, path, params)
            Log.d(LOG_TAG, "GET $url")
            val body = executeGet(url)
            withContext(Dispatchers.Default) {
                json.decodeFromString(serializer, body)
            }
        }
    }

    private fun buildUrl(
        baseUrl: String,
        path: String,
        params: Map<String, String>
    ): okhttp3.HttpUrl {
        val builder = baseUrl.toHttpUrl().newBuilder()
        builder.addPathSegments(path.trimStart('/'))
        params.forEach { (key, value) ->
            builder.addQueryParameter(key, value)
        }
        return builder.build()
    }

    private suspend fun executeGet(url: okhttp3.HttpUrl): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                throw ApiException(code, "HTTP $code")
            }
            val body = response.body?.string().orEmpty()
            response.close()
            body
        }
    }

    private fun parsePlayableUrl(body: String): String? {
        val element = runCatching { json.parseToJsonElement(body) }.getOrNull()
        return when (element) {
            is JsonPrimitive -> element.contentOrNull()?.trim()
            is JsonObject -> element["url"]?.jsonPrimitive?.contentOrNull()?.trim()
            is JsonArray -> element.firstOrNull()?.let { parseFromElement(it) }
            else -> null
        }
    }

    private fun parseFromElement(element: JsonElement): String? {
        return when (element) {
            is JsonObject -> element["url"]?.jsonPrimitive?.contentOrNull()?.trim()
            is JsonPrimitive -> element.contentOrNull()?.trim()
            else -> null
        }
    }

    private fun JsonPrimitive.contentOrNull(): String? {
        return if (isString) content else null
    }
}

private const val LOG_TAG = "OpenAirRadio"
