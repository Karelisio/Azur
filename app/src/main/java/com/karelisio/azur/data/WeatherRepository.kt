package com.karelisio.azur.data

import com.karelisio.azur.data.db.AzurDatabase
import com.karelisio.azur.data.db.ForecastCacheEntity
import com.karelisio.azur.data.db.PlaceEntity
import com.karelisio.azur.data.net.AzurJson
import com.karelisio.azur.data.net.OpenMeteoApi
import com.karelisio.azur.data.net.VigilanceApi
import com.karelisio.azur.data.net.OpenMeteoParser
import com.karelisio.azur.domain.AirQuality
import com.karelisio.azur.domain.Forecast
import com.karelisio.azur.domain.Vigilance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Données météo prêtes à afficher pour un lieu (issues du cache). */
data class PlaceWeather(
    val placeId: Long,
    val forecast: Forecast,
    val air: AirQuality?,
    val altitude: Forecast?,
    val altitudeMeters: Int?,
    val vigilance: Vigilance?,
    val fetchedAt: Long,
)

class WeatherRepository(
    private val db: AzurDatabase,
    private val api: OpenMeteoApi,
    private val vigilanceApi: VigilanceApi,
    private val settings: SettingsRepository,
    /** Appelé après chaque mise à jour réussie (widgets, tuile). */
    private val onUpdated: suspend () -> Unit = {},
) {
    fun observe(placeId: Long): Flow<PlaceWeather?> = db.cache().observe(placeId).map { it?.toWeather() }

    /** Toutes les données en cache, par identifiant de lieu (analyse hors du thread principal). */
    fun observeAll(): Flow<Map<Long, PlaceWeather>> = db.cache().observeAll()
        .map { list -> list.mapNotNull { it.toWeather() }.associateBy { it.placeId } }
        .flowOn(Dispatchers.Default)

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
            val vigilance = async {
                val dept = place.department
                if (s.vigilanceApiKey.isNotBlank() && dept != null) {
                    runCatching { vigilanceApi.fetch(s.vigilanceApiKey, dept) }.getOrNull()
                        ?.let { AzurJson.encodeToString(Vigilance.serializer(), it) }
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
                    vigilanceJson = if (s.vigilanceApiKey.isBlank()) null else vigilance.await() ?: previous?.vigilanceJson,
                ),
            )
        }
        runCatching { onUpdated() }
        Unit
    }

    /** Met à jour uniquement la vigilance (tâche horaire). Renvoie la vigilance obtenue. */
    suspend fun refreshVigilance(place: PlaceEntity): Vigilance? {
        val key = settings.current().vigilanceApiKey
        val dept = place.department ?: return null
        if (key.isBlank()) return null
        val v = vigilanceApi.fetch(key, dept) ?: return null
        db.cache().get(place.id)?.let { db.cache().upsert(it.copy(vigilanceJson = AzurJson.encodeToString(Vigilance.serializer(), v))) }
        return v
    }

    private fun ForecastCacheEntity.toWeather(): PlaceWeather? = runCatching {
        PlaceWeather(
            placeId = placeId,
            forecast = OpenMeteoParser.parseForecast(forecastJson),
            air = airJson?.let { runCatching { OpenMeteoParser.parseAirQuality(it) }.getOrNull() },
            altitude = altitudeJson?.let { runCatching { OpenMeteoParser.parseForecast(it) }.getOrNull() },
            altitudeMeters = altitude,
            vigilance = vigilanceJson?.let { runCatching { AzurJson.decodeFromString(Vigilance.serializer(), it) }.getOrNull() },
            fetchedAt = fetchedAt,
        )
    }.getOrNull()

    companion object {
        const val STALE_MS = 15 * 60 * 1000L
    }
}
