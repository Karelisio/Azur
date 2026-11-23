package com.karelisio.azur.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PlaceEntity::class, ForecastCacheEntity::class], version = 1, exportSchema = true)
abstract class AzurDatabase : RoomDatabase() {
    abstract fun places(): PlaceDao
    abstract fun cache(): ForecastCacheDao

    companion object {
        fun build(context: Context): AzurDatabase =
            Room.databaseBuilder(context, AzurDatabase::class.java, "azur.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
