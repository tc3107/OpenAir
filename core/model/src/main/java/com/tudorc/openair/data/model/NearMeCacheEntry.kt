package com.tudorc.openair.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NearMeCacheEntry(
    val station: Station,
    val distanceMeters: Int
)
