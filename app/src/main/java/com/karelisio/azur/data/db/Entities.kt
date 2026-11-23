package com.karelisio.azur.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Lieu enregistré. La position actuelle est une ligne unique avec [isCurrent] = true. */
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val region: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val department: String? = null,
    val sortOrder: Int = 0,
    val isCurrent: Boolean = false,
)

/** Cache hors ligne : réponses JSON brutes, ré-analysées à la lecture. */
@Entity(tableName = "forecast_cache")
data class ForecastCacheEntity(
    @PrimaryKey val placeId: Long,
    val forecastJson: String,
    val fetchedAt: Long,
    val airJson: String? = null,
    val altitudeJson: String? = null,
    val altitude: Int? = null,
    val vigilanceJson: String? = null,
)
