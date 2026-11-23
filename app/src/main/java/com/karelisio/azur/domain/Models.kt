package com.karelisio.azur.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Instants en secondes epoch (UTC). Valeurs internes : °C, km/h, mm, hPa, m, cm (neige). */
data class Forecast(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val timezone: String,
    val utcOffsetSeconds: Int,
    val current: CurrentWeather,
    val minutely: List<MinutelyPoint>,
    val hourly: List<HourlyPoint>,
    val daily: List<DailyPoint>,
) {
    val zone: ZoneId get() = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())

    fun dateOf(epoch: Long): LocalDate = Instant.ofEpochSecond(epoch).atZone(zone).toLocalDate()
    fun timeOf(epoch: Long): ZonedDateTime = Instant.ofEpochSecond(epoch).atZone(zone)

    /** Heures à partir de l'heure en cours. */
    fun hourlyFrom(now: Long, count: Int = 48): List<HourlyPoint> =
        hourly.filter { it.time + 3600 > now }.take(count)

    fun today(now: Long): DailyPoint? = daily.firstOrNull { dateOf(it.time) == dateOf(now) } ?: daily.firstOrNull()

    fun hoursOfDay(day: DailyPoint): List<HourlyPoint> {
        val date = dateOf(day.time)
        return hourly.filter { dateOf(it.time) == date }
    }
}

data class CurrentWeather(
    val time: Long,
    val temperature: Double,
    val apparentTemperature: Double?,
    val humidity: Double?,
    val isDay: Boolean,
    val weatherCode: Int,
    val precipitation: Double?,
    val cloudCover: Double?,
    val pressure: Double?,
    val windSpeed: Double?,
    val windDirection: Double?,
    val windGusts: Double?,
    val dewPoint: Double?,
    val visibility: Double?,
    val uvIndex: Double?,
) {
    val condition: WeatherCondition get() = WeatherCondition.fromWmo(weatherCode)
}

data class MinutelyPoint(val time: Long, val precipitation: Double)

data class HourlyPoint(
    val time: Long,
    val temperature: Double,
    val apparentTemperature: Double? = null,
    val precipitationProbability: Double? = null,
    val precipitation: Double = 0.0,
    val weatherCode: Int = 0,
    val windSpeed: Double? = null,
    val windDirection: Double? = null,
    val windGusts: Double? = null,
    val isDay: Boolean = true,
    val humidity: Double? = null,
    val dewPoint: Double? = null,
    val pressure: Double? = null,
    val visibility: Double? = null,
    val uvIndex: Double? = null,
    val cloudCover: Double? = null,
    val snowfall: Double? = null,
    val freezingLevel: Double? = null,
    val temperature850: Double? = null,
    val windSpeed850: Double? = null,
    val windDirection850: Double? = null,
    val geopotential850: Double? = null,
    val temperature700: Double? = null,
    val windSpeed700: Double? = null,
    val windDirection700: Double? = null,
    val geopotential700: Double? = null,
) {
    val condition: WeatherCondition get() = WeatherCondition.fromWmo(weatherCode)
}

data class DailyPoint(
    val time: Long,
    val weatherCode: Int,
    val temperatureMax: Double,
    val temperatureMin: Double,
    val precipitationSum: Double? = null,
    val precipitationProbabilityMax: Double? = null,
    val sunrise: Long? = null,
    val sunset: Long? = null,
    val uvIndexMax: Double? = null,
    val windSpeedMax: Double? = null,
    val windGustsMax: Double? = null,
    val windDirectionDominant: Double? = null,
    val snowfallSum: Double? = null,
) {
    val condition: WeatherCondition get() = WeatherCondition.fromWmo(weatherCode)
}

enum class Pollen(val label: String, val thresholds: List<Double>) {
    GRASS("Graminées", listOf(5.0, 20.0, 50.0, 200.0)),
    BIRCH("Bouleau", listOf(10.0, 50.0, 100.0, 500.0)),
    ALDER("Aulne", listOf(10.0, 50.0, 100.0, 500.0)),
    OLIVE("Olivier", listOf(10.0, 50.0, 100.0, 500.0)),
    MUGWORT("Armoise", listOf(5.0, 10.0, 30.0, 100.0)),
    RAGWEED("Ambroisie", listOf(3.0, 10.0, 30.0, 100.0)),
    ;

    /** Niveau 0 (nul) à 4 (très élevé). */
    fun level(value: Double?): Int = when {
        value == null || value < 1 -> 0
        else -> thresholds.indexOfFirst { value < it }.let { if (it == -1) 4 else maxOf(1, it) }
    }
}

data class AirQuality(
    val time: Long,
    val europeanAqi: Double?,
    val pm10: Double?,
    val pm25: Double?,
    val ozone: Double?,
    val nitrogenDioxide: Double?,
    val pollen: Map<Pollen, Double?>,
) {
    val aqiLevel: AqiLevel? get() = europeanAqi?.let { AqiLevel.of(it) }
}

enum class AqiLevel(val label: String, val max: Double) {
    GOOD("Bon", 20.0), FAIR("Correct", 40.0), MODERATE("Moyen", 60.0),
    POOR("Mauvais", 80.0), VERY_POOR("Très mauvais", 100.0), EXTREME("Extrêmement mauvais", Double.MAX_VALUE);

    companion object {
        fun of(aqi: Double): AqiLevel = entries.first { aqi <= it.max }
    }
}
