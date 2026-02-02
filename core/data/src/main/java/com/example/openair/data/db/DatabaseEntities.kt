package com.example.openair.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.openair.data.model.Country
import com.example.openair.data.model.CountryCode
import com.example.openair.data.model.Language
import com.example.openair.data.model.Station
import com.example.openair.data.model.Tag

@Entity(
    tableName = "stations",
    indices = [
        Index(value = ["name"]),
        Index(value = ["name_normalized"]),
        Index(value = ["country"]),
        Index(value = ["country_normalized"]),
        Index(value = ["countrycode"]),
        Index(value = ["state"]),
        Index(value = ["state_normalized"]),
        Index(value = ["language"]),
        Index(value = ["language_normalized"]),
        Index(value = ["tags"]),
        Index(value = ["tags_normalized"]),
        Index(value = ["codec"]),
        Index(value = ["bitrate"]),
        Index(value = ["votes"]),
        Index(value = ["clickcount"]),
        Index(value = ["lastcheckok"]),
        Index(value = ["has_geo_info"]),
        Index(value = ["is_https"])
    ]
)
data class StationEntity(
    @PrimaryKey val stationuuid: String,
    val name: String,
    @ColumnInfo(name = "name_normalized") val nameNormalized: String,
    val country: String?,
    @ColumnInfo(name = "country_normalized") val countryNormalized: String?,
    val countrycode: String?,
    val state: String?,
    @ColumnInfo(name = "state_normalized") val stateNormalized: String?,
    val language: String?,
    @ColumnInfo(name = "language_normalized") val languageNormalized: String?,
    val tags: String?,
    @ColumnInfo(name = "tags_normalized") val tagsNormalized: String?,
    val codec: String?,
    val bitrate: Int?,
    val favicon: String?,
    val votes: Int?,
    val clickcount: Int?,
    val lastcheckok: Int?,
    @ColumnInfo(name = "has_geo_info") val hasGeoInfo: Boolean?,
    @ColumnInfo(name = "is_https") val isHttps: Boolean?,
    @ColumnInfo(name = "geo_lat") val geoLat: Double?,
    @ColumnInfo(name = "geo_long") val geoLong: Double?,
    val url: String?,
    @ColumnInfo(name = "url_resolved") val urlResolved: String?,
    @ColumnInfo(name = "url_https") val urlHttps: String?
)

@Entity(tableName = "countries")
data class CountryEntity(
    @PrimaryKey val name: String,
    val stationCount: Int,
    val countrycode: String
)

@Entity(tableName = "country_codes")
data class CountryCodeEntity(
    @PrimaryKey val name: String,
    val stationCount: Int
)

@Entity(tableName = "languages")
data class LanguageEntity(
    @PrimaryKey val name: String,
    val stationCount: Int
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val name: String,
    val stationCount: Int
)

@Entity(tableName = "database_meta")
data class DatabaseMetaEntity(
    @PrimaryKey val id: Int = 1,
    val status: String,
    val stationCount: Int,
    val countryCount: Int,
    val tagCount: Int,
    val languageCount: Int,
    val lastUpdated: Long,
    val lastDurationMillis: Long,
    val lastError: String?
)

fun Station.toEntity(): StationEntity {
    val nameValue = name.ifBlank { "" }
    val countryValue = country?.trim().orEmpty().ifBlank { null }
    val languageValue = language?.trim().orEmpty().ifBlank { null }
    val stateValue = state?.trim().orEmpty().ifBlank { null }
    val tagsValue = tags?.trim().orEmpty().ifBlank { null }
    val normalizedTags = tagsValue
        ?.split(',')
        ?.map { it.trim().lowercase() }
        ?.filter { it.isNotBlank() }
        ?.joinToString(",")
    val countryCodeValue = countrycode?.trim().orEmpty().ifBlank { null }?.uppercase()
    return StationEntity(
        stationuuid = stationuuid,
        name = nameValue,
        nameNormalized = nameValue.lowercase(),
        country = countryValue,
        countryNormalized = countryValue?.lowercase(),
        countrycode = countryCodeValue,
        state = stateValue,
        stateNormalized = stateValue?.lowercase(),
        language = languageValue,
        languageNormalized = languageValue?.lowercase(),
        tags = tagsValue,
        tagsNormalized = normalizedTags,
        codec = codec?.trim().orEmpty().ifBlank { null },
        bitrate = bitrate,
        favicon = favicon?.trim().orEmpty().ifBlank { null },
        votes = votes,
        clickcount = clickcount,
        lastcheckok = lastcheckok,
        hasGeoInfo = has_geo_info,
        isHttps = is_https,
        geoLat = geo_lat,
        geoLong = geo_long,
        url = url?.trim().orEmpty().ifBlank { null },
        urlResolved = url_resolved?.trim().orEmpty().ifBlank { null },
        urlHttps = url_https?.trim().orEmpty().ifBlank { null }
    )
}

fun StationEntity.toModel(): Station {
    return Station(
        stationuuid = stationuuid,
        name = name,
        country = country,
        countrycode = countrycode,
        state = state,
        language = language,
        tags = tags,
        codec = codec,
        bitrate = bitrate,
        favicon = favicon,
        votes = votes,
        clickcount = clickcount,
        lastcheckok = lastcheckok,
        has_geo_info = hasGeoInfo,
        is_https = isHttps,
        geo_lat = geoLat,
        geo_long = geoLong,
        url = url,
        url_resolved = urlResolved,
        url_https = urlHttps
    )
}

fun Country.toEntity(): CountryEntity {
    return CountryEntity(
        name = name,
        stationCount = stationcount,
        countrycode = countrycode.trim().uppercase()
    )
}

fun CountryEntity.toModel(): Country {
    return Country(
        name = name,
        stationcount = stationCount,
        countrycode = countrycode
    )
}

fun CountryCode.toEntity(): CountryCodeEntity {
    return CountryCodeEntity(
        name = name,
        stationCount = stationcount
    )
}

fun CountryCodeEntity.toModel(): CountryCode {
    return CountryCode(
        name = name,
        stationcount = stationCount
    )
}

fun Language.toEntity(): LanguageEntity {
    return LanguageEntity(
        name = name,
        stationCount = stationcount
    )
}

fun LanguageEntity.toModel(): Language {
    return Language(
        name = name,
        stationcount = stationCount
    )
}

fun Tag.toEntity(): TagEntity {
    return TagEntity(
        name = name,
        stationCount = stationcount
    )
}

fun TagEntity.toModel(): Tag {
    return Tag(
        name = name,
        stationcount = stationCount
    )
}
