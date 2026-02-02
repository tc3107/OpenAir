package com.example.openair.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Country(
    val name: String = "",
    val stationcount: Int = 0,
    val countrycode: String = ""
)

@Serializable
data class CountryCode(
    val name: String = "",
    val stationcount: Int = 0
)

@Serializable
data class Language(
    val name: String = "",
    val stationcount: Int = 0
)

@Serializable
data class Tag(
    val name: String = "",
    val stationcount: Int = 0
)

@Serializable
data class Codec(
    val name: String = "",
    val stationcount: Int = 0
)

@Serializable
data class Station(
    val stationuuid: String = "",
    val name: String = "",
    val country: String? = null,
    val countrycode: String? = null,
    val state: String? = null,
    val language: String? = null,
    val tags: String? = null,
    val codec: String? = null,
    val bitrate: Int? = null,
    val favicon: String? = null,
    val votes: Int? = null,
    val clickcount: Int? = null,
    val lastcheckok: Int? = null,
    val has_geo_info: Boolean? = null,
    val is_https: Boolean? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val geo_lat: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val geo_long: Double? = null,
    val url: String? = null,
    val url_resolved: String? = null,
    val url_https: String? = null
)

@Serializable
data class ServerInfo(
    val name: String = "",
    val ip: String? = null,
    val serverip: String? = null,
    val servername: String? = null
)
