package com.tudorc.openair.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<StationEntity>)

    @Query("DELETE FROM stations")
    suspend fun clearStations()

    @Query("SELECT COUNT(*) FROM stations")
    suspend fun countStations(): Int

    @Query("SELECT * FROM stations WHERE stationuuid = :uuid LIMIT 1")
    suspend fun getStationByUuid(uuid: String): StationEntity?

    @Query("SELECT * FROM stations WHERE geo_lat IS NOT NULL AND geo_long IS NOT NULL")
    suspend fun getStationsWithGeo(): List<StationEntity>

    @Query("SELECT * FROM stations")
    suspend fun getAllStations(): List<StationEntity>

    @RawQuery(observedEntities = [StationEntity::class])
    suspend fun searchStations(query: SupportSQLiteQuery): List<StationEntity>

    @RawQuery(observedEntities = [StationEntity::class])
    suspend fun countStations(query: SupportSQLiteQuery): Int
}

@Dao
interface CountryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CountryEntity>)

    @Query("DELETE FROM countries")
    suspend fun clearAll()

    @Query("SELECT * FROM countries ORDER BY stationCount DESC, name ASC")
    suspend fun getAll(): List<CountryEntity>

    @Query("SELECT COUNT(*) FROM countries")
    suspend fun count(): Int
}

@Dao
interface CountryCodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CountryCodeEntity>)

    @Query("DELETE FROM country_codes")
    suspend fun clearAll()

    @Query("SELECT * FROM country_codes ORDER BY stationCount DESC, name ASC")
    suspend fun getAll(): List<CountryCodeEntity>
}

@Dao
interface LanguageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LanguageEntity>)

    @Query("DELETE FROM languages")
    suspend fun clearAll()

    @Query("SELECT * FROM languages ORDER BY stationCount DESC, name ASC")
    suspend fun getAll(): List<LanguageEntity>

    @Query("SELECT COUNT(*) FROM languages")
    suspend fun count(): Int
}

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TagEntity>)

    @Query("DELETE FROM tags")
    suspend fun clearAll()

    @Query("SELECT * FROM tags ORDER BY stationCount DESC, name ASC")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun count(): Int
}

@Dao
interface DatabaseMetaDao {
    @Query("SELECT * FROM database_meta WHERE id = 1")
    fun observeMeta(): Flow<DatabaseMetaEntity?>

    @Query("SELECT * FROM database_meta WHERE id = 1")
    suspend fun getMeta(): DatabaseMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: DatabaseMetaEntity)
}
