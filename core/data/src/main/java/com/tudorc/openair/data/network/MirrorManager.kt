package com.tudorc.openair.data.network

import kotlinx.coroutines.delay
import java.io.IOException
import android.util.Log

class MirrorManager(
    private val discovery: ServerDiscovery
) {
    private var mirrors: List<String> = emptyList()
    private var index = 0

    suspend fun current(): String {
        if (mirrors.isEmpty()) {
            refresh()
        }
        return mirrors.getOrElse(index) { mirrors.first() }
    }

    suspend fun refresh() {
        mirrors = discovery.discover()
        if (mirrors.isEmpty()) {
            mirrors = listOf("https://de1.api.radio-browser.info")
        }
        index = 0
        Log.i(LOG_TAG, "Mirror list: $mirrors")
    }

    fun rotate() {
        if (mirrors.isNotEmpty()) {
            index = (index + 1) % mirrors.size
            Log.w(LOG_TAG, "Rotating mirror to index=$index baseUrl=${mirrors[index]}")
        }
    }

    suspend fun <T> withMirrors(block: suspend (baseUrl: String) -> T): T {
        if (mirrors.isEmpty()) {
            refresh()
        }
        var lastError: Exception? = null
        val maxAttempts = mirrors.size.coerceAtLeast(1)
        repeat(maxAttempts) { attempt ->
            val base = current()
            try {
                Log.d(LOG_TAG, "Request attempt ${attempt + 1}/$maxAttempts using $base")
                return block(base)
            } catch (e: ApiException) {
                lastError = e
                Log.e(LOG_TAG, "API error on $base: ${e.message}", e)
                if (e.isServerError) {
                    rotate()
                } else {
                    throw e
                }
            } catch (e: IOException) {
                lastError = e
                Log.e(LOG_TAG, "Network error on $base: ${e.message}", e)
                rotate()
            } catch (e: Exception) {
                lastError = e
                Log.e(LOG_TAG, "Unexpected error on $base: ${e.message}", e)
                rotate()
            }
            delay(backoffMillis(attempt))
        }
        throw lastError ?: IllegalStateException("No mirrors available")
    }

    private fun backoffMillis(attempt: Int): Long {
        val base = 300L
        val capped = attempt.coerceAtMost(4)
        return base * (1L shl capped)
    }
}

private const val LOG_TAG = "OpenAirRadio"
