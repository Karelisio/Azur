package com.karelisio.azur.data

import com.karelisio.azur.data.db.AzurDatabase
import com.karelisio.azur.data.db.ForecastCacheEntity
import com.karelisio.azur.data.db.PlaceEntity
import com.karelisio.azur.data.net.OpenMeteoApi
import com.karelisio.azur.data.net.OpenMeteoParser
import com.karelisio.azur.domain.AirQuality
import com.karelisio.azur.domain.Forecast
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Données météo prêtes à afficher pour un lieu (issues du cache). */
data class PlaceWeather(
    val placeId: Long,
    val forecast: Forecast,
    val air: AirQuality?,
    val altitude: Forecast?,
    val altitudeMeters: Int?,
    val vigilanceJson: String?,
    val fetchedAt: Long,
)

class WeatherRepository(
    private val db: AzurDatabase,
    private val api: OpenMeteoApi,
    private val settings: SettingsRepository,
) {
    fun observe(placeId: Long): Flow<PlaceWeather?> = db.cache().observe(placeId).map { it?.toWeather() }

    suspend fun cached(placeId: Long): PlaceWeather? = db.cache().get(placeId)?.toWeather()

    fun isStale(fetchedAt: Long, maxAgeMs: Long = STALE_MS) = System.currentTimeMillis() - fetchedAt > maxAgeMs

    /** Télécharge prévision + air (+ altitude si le module montagne est actif) et met le cache à jour. */
    suspend fun refresh(place: PlaceEntity): Result<Unit> = runCatching {
        val s = settings.current()
        coroutineScope {
            val forecast = async { api.forecast(place.latitude, place.longitude) }
            val air = async { runCatching { api.airQuality(place.latitude, place.longitude) }.getOrNull() }
            val altitude = async {
                if (s.mountainEnabled) {
                    runCatching { api.forecastAtAltitude(place.latitude, place.longitude, s.mountainAltitude) }.getOrNull()
                } else null
            }
            val forecastJson = forecast.await()
            OpenMeteoParser.parseForecast(forecastJson) // valide avant d'écraser le cache
            val previous = db.cache().get(place.id)
            db.cache().upsert(
                ForecastCacheEntity(
                    placeId = place.id,
                    forecastJson = forecastJson,
                    fetchedAt = System.currentTimeMillis(),
                    airJson = air.await() ?: previous?.airJson,
                    altitudeJson = altitude.await(),
                    altitude = if (s.mountainEnabled) s.mountainAltitude else null,
                    vigilanceJson = previous?.vigilanceJson,
                ),
            )
        }
    }

    suspend fun saveVigilance(placeId: Long, json: String?) {
        val previous = db.cache().get(placeId) ?: return
        db.cache().upsert(previous.copy(vigilanceJson = json))
    }

    private fun ForecastCacheEntity.toWeather(): PlaceWeather? = runCatching {
        PlaceWeather(
            placeId = placeId,
            forecast = OpenMeteoParser.parseForecast(forecastJson),
            air = airJson?.let { runCatching { OpenMeteoParser.parseAirQuality(it) }.getOrNull() },
            altitude = altitudeJson?.let { runCatching { OpenMeteoParser.parseForecast(it) }.getOrNull() },
            altitudeMeters = altitude,
            vigilanceJson = vigilanceJson,
            fetchedAt = fetchedAt,
        )
    }.getOrNull()

    companion object {
        const val STALE_MS = 15 * 60 * 1000L
    }
}
