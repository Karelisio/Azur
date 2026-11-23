package com.karelisio.azur.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY isCurrent DESC, sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places ORDER BY isCurrent DESC, sortOrder ASC, id ASC")
    suspend fun all(): List<PlaceEntity>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun byId(id: Long): PlaceEntity?

    @Query("SELECT * FROM places WHERE isCurrent = 1 LIMIT 1")
    suspend fun current(): PlaceEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM places")
    suspend fun maxOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: PlaceEntity): Long

    @Update
    suspend fun update(places: List<PlaceEntity>)

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun delete(id: Long)

    @Transaction
    suspend fun reorder(idsInOrder: List<Long>) {
        val byId = all().associateBy { it.id }
        update(idsInOrder.mapIndexedNotNull { i, id -> byId[id]?.copy(sortOrder = i + 1) })
    }
}

@Dao
interface ForecastCacheDao {
    @Query("SELECT * FROM forecast_cache WHERE placeId = :placeId")
    fun observe(placeId: Long): Flow<ForecastCacheEntity?>

    @Query("SELECT * FROM forecast_cache WHERE placeId = :placeId")
    suspend fun get(placeId: Long): ForecastCacheEntity?

    @Upsert
    suspend fun upsert(entity: ForecastCacheEntity)

    @Query("DELETE FROM forecast_cache WHERE placeId = :placeId")
    suspend fun delete(placeId: Long)
}
