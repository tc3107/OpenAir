package com.example.openair.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StationEntity::class,
        CountryEntity::class,
        CountryCodeEntity::class,
        LanguageEntity::class,
        TagEntity::class,
        DatabaseMetaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OpenAirDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun countryDao(): CountryDao
    abstract fun countryCodeDao(): CountryCodeDao
    abstract fun languageDao(): LanguageDao
    abstract fun tagDao(): TagDao
    abstract fun metaDao(): DatabaseMetaDao

    companion object {
        @Volatile
        private var instance: OpenAirDatabase? = null

        fun getInstance(context: Context): OpenAirDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OpenAirDatabase::class.java,
                    "openair.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
