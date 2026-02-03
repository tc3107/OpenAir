package com.tudorc.openair.data.network

import com.tudorc.openair.data.model.ServerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import kotlin.random.Random
import android.util.Log

class ServerDiscovery(
    private val client: OkHttpClient,
    private val json: Json,
    private val userAgent: String
) {
    suspend fun discover(): List<String> {
        val dnsHosts = resolveDnsHosts()
        if (dnsHosts.isNotEmpty()) {
            Log.i(LOG_TAG, "Discovered ${dnsHosts.size} DNS hosts: $dnsHosts")
            return dnsHosts.shuffled(Random(System.currentTimeMillis()))
                .map { "https://$it" }
        }
        Log.w(LOG_TAG, "DNS discovery returned empty list, falling back to /json/servers")
        return fetchServersFallback()
    }

    private suspend fun resolveDnsHosts(): List<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                InetAddress.getAllByName("all.api.radio-browser.info")
                    .mapNotNull { address ->
                        val host = address.canonicalHostName
                        if (host.isNullOrBlank()) {
                            null
                        } else if (host == address.hostAddress) {
                            null
                        } else {
                            host
                        }
                    }
                    .distinct()
            }.onFailure { error ->
                Log.e(LOG_TAG, "DNS discovery failed", error)
            }.getOrDefault(emptyList())
        }
    }

    private suspend fun fetchServersFallback(): List<String> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://de1.api.radio-browser.info/json/servers")
                .header("User-Agent", userAgent)
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(LOG_TAG, "Fallback /json/servers failed: HTTP ${response.code}")
                response.close()
                return@withContext emptyList()
            }
            val body = response.body?.string().orEmpty()
            response.close()
            val servers = runCatching {
                json.decodeFromString(ListSerializer(ServerInfo.serializer()), body)
            }.getOrDefault(emptyList())
            Log.i(LOG_TAG, "Fallback servers returned ${servers.size} entries")
            servers.mapNotNull { server ->
                val name = server.name.ifBlank { server.servername.orEmpty() }
                if (name.isBlank()) null else "https://$name"
            }.distinct()
        }
    }
}

private const val LOG_TAG = "OpenAirRadio"
