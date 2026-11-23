package com.karelisio.azur.data.net

import com.karelisio.azur.domain.AirQuality
import com.karelisio.azur.domain.CurrentWeather
import com.karelisio.azur.domain.DailyPoint
import com.karelisio.azur.domain.Forecast
import com.karelisio.azur.domain.HourlyPoint
import com.karelisio.azur.domain.MinutelyPoint
import com.karelisio.azur.domain.Pollen
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/**
 * Analyse des réponses Open-Meteo (format `timeformat=unixtime`).
 *
 * Avec `models=meteofrance_seamless,best_match`, chaque variable est suffixée par le modèle.
 * Pour chaque variable et chaque pas de temps, on prend la première valeur non nulle dans l'ordre
 * Météo-France → sans suffixe → best_match.
 */
object OpenMeteoParser {
    val MODEL_PRIORITY = listOf("_meteofrance_seamless", "", "_best_match")

    class OpenMeteoError(message: String) : Exception(message)

    private class Section(private val obj: JsonObject) {
        val times: List<Long> = (obj["time"] as? JsonArray)?.map { (it as? JsonPrimitive)?.longOrNull ?: 0L } ?: emptyList()

        fun series(name: String): List<Double?> {
            val candidates = MODEL_PRIORITY.mapNotNull { suffix -> obj["$name$suffix"] as? JsonArray }
            return List(times.size) { i ->
                candidates.firstNotNullOfOrNull { arr -> arr.getOrNull(i)?.asDouble() }
            }
        }
    }

    private class Scalars(private val obj: JsonObject) {
        fun get(name: String): Double? =
            MODEL_PRIORITY.firstNotNullOfOrNull { suffix -> obj["$name$suffix"]?.asDouble() }
    }

    private fun JsonElement.asDouble(): Double? =
        if (this is JsonNull) null else (this as? JsonPrimitive)?.doubleOrNull

    private fun root(text: String): JsonObject {
        val obj = AzurJson.parseToJsonElement(text).jsonObject
        if ((obj["error"] as? JsonPrimitive)?.content == "true") {
            throw OpenMeteoError((obj["reason"] as? JsonPrimitive)?.content ?: "Erreur Open-Meteo")
        }
        return obj
    }

    fun parseForecast(text: String): Forecast {
        val obj = root(text)
        val currentObj = obj["current"] as? JsonObject ?: JsonObject(emptyMap())
        val c = Scalars(currentObj)
        val currentTime = (currentObj["time"] as? JsonPrimitive)?.longOrNull ?: 0L

        val hourly = (obj["hourly"] as? JsonObject)?.let(::Section)
        val hourlyPoints = hourly?.let { h ->
            val temp = h.series("temperature_2m")
            val app = h.series("apparent_temperature")
            val prob = h.series("precipitation_probability")
            val precip = h.series("precipitation")
            val code = h.series("weather_code")
            val ws = h.series("wind_speed_10m")
            val wd = h.series("wind_direction_10m")
            val wg = h.series("wind_gusts_10m")
            val day = h.series("is_day")
            val rh = h.series("relative_humidity_2m")
            val dew = h.series("dew_point_2m")
            val pres = h.series("pressure_msl")
            val vis = h.series("visibility")
            val uv = h.series("uv_index")
            val cloud = h.series("cloud_cover")
            val snow = h.series("snowfall")
            val fl = h.series("freezing_level_height")
            val t850 = h.series("temperature_850hPa")
            val ws850 = h.series("wind_speed_850hPa")
            val wd850 = h.series("wind_direction_850hPa")
            val z850 = h.series("geopotential_height_850hPa")
            val t700 = h.series("temperature_700hPa")
            val ws700 = h.series("wind_speed_700hPa")
            val wd700 = h.series("wind_direction_700hPa")
            val z700 = h.series("geopotential_height_700hPa")
            h.times.indices.mapNotNull { i ->
                val t = temp[i] ?: return@mapNotNull null
                HourlyPoint(
                    time = h.times[i], temperature = t, apparentTemperature = app[i],
                    precipitationProbability = prob[i], precipitation = precip[i] ?: 0.0,
                    weatherCode = code[i]?.toInt() ?: 0, windSpeed = ws[i], windDirection = wd[i],
                    windGusts = wg[i], isDay = (day[i] ?: 1.0) >= 0.5, humidity = rh[i], dewPoint = dew[i],
                    pressure = pres[i], visibility = vis[i], uvIndex = uv[i], cloudCover = cloud[i],
                    snowfall = snow[i], freezingLevel = fl[i],
                    temperature850 = t850[i], windSpeed850 = ws850[i], windDirection850 = wd850[i], geopotential850 = z850[i],
                    temperature700 = t700[i], windSpeed700 = ws700[i], windDirection700 = wd700[i], geopotential700 = z700[i],
                )
            }
        } ?: emptyList()

        val daily = (obj["daily"] as? JsonObject)?.let(::Section)
        val dailyPoints = daily?.let { d ->
            val code = d.series("weather_code")
            val tmax = d.series("temperature_2m_max")
            val tmin = d.series("temperature_2m_min")
            val psum = d.series("precipitation_sum")
            val pprob = d.series("precipitation_probability_max")
            val sunrise = d.series("sunrise")
            val sunset = d.series("sunset")
            val uv = d.series("uv_index_max")
            val wmax = d.series("wind_speed_10m_max")
            val gmax = d.series("wind_gusts_10m_max")
            val wdir = d.series("wind_direction_10m_dominant")
            val snow = d.series("snowfall_sum")
            d.times.indices.mapNotNull { i ->
                val hi = tmax[i] ?: return@mapNotNull null
                val lo = tmin[i] ?: return@mapNotNull null
                DailyPoint(
                    time = d.times[i], weatherCode = code[i]?.toInt() ?: 0, temperatureMax = hi, temperatureMin = lo,
                    precipitationSum = psum[i], precipitationProbabilityMax = pprob[i],
                    sunrise = sunrise[i]?.toLong(), sunset = sunset[i]?.toLong(), uvIndexMax = uv[i],
                    windSpeedMax = wmax[i], windGustsMax = gmax[i], windDirectionDominant = wdir[i], snowfallSum = snow[i],
                )
            }
        } ?: emptyList()

        val minutely = (obj["minutely_15"] as? JsonObject)?.let(::Section)?.let { m ->
            val p = m.series("precipitation")
            m.times.indices.map { i -> MinutelyPoint(m.times[i], p[i] ?: 0.0) }
        } ?: emptyList()

        // « current » peut manquer pour certains modèles : repli sur l'heure la plus proche.
        val nearest = hourlyPoints.minByOrNull { kotlin.math.abs(it.time - currentTime) }
        val current = CurrentWeather(
            time = currentTime,
            temperature = c.get("temperature_2m") ?: nearest?.temperature ?: 0.0,
            apparentTemperature = c.get("apparent_temperature") ?: nearest?.apparentTemperature,
            humidity = c.get("relative_humidity_2m") ?: nearest?.humidity,
            isDay = (c.get("is_day") ?: if (nearest?.isDay != false) 1.0 else 0.0) >= 0.5,
            weatherCode = c.get("weather_code")?.toInt() ?: nearest?.weatherCode ?: 0,
            precipitation = c.get("precipitation"),
            cloudCover = c.get("cloud_cover") ?: nearest?.cloudCover,
            pressure = c.get("pressure_msl") ?: nearest?.pressure,
            windSpeed = c.get("wind_speed_10m") ?: nearest?.windSpeed,
            windDirection = c.get("wind_direction_10m") ?: nearest?.windDirection,
            windGusts = c.get("wind_gusts_10m") ?: nearest?.windGusts,
            dewPoint = c.get("dew_point_2m") ?: nearest?.dewPoint,
            visibility = c.get("visibility") ?: nearest?.visibility,
            uvIndex = c.get("uv_index") ?: nearest?.uvIndex,
        )

        return Forecast(
            latitude = (obj["latitude"] as? JsonPrimitive)?.doubleOrNull ?: 0.0,
            longitude = (obj["longitude"] as? JsonPrimitive)?.doubleOrNull ?: 0.0,
            elevation = (obj["elevation"] as? JsonPrimitive)?.doubleOrNull ?: 0.0,
            timezone = (obj["timezone"] as? JsonPrimitive)?.content ?: "Europe/Paris",
            utcOffsetSeconds = (obj["utc_offset_seconds"] as? JsonPrimitive)?.intOrNull ?: 0,
            current = current,
            minutely = minutely,
            hourly = hourlyPoints,
            daily = dailyPoints,
        )
    }

    fun parseAirQuality(text: String): AirQuality {
        val obj = root(text)
        val current = obj["current"] as? JsonObject ?: JsonObject(emptyMap())
        val c = Scalars(current)
        return AirQuality(
            time = (current["time"] as? JsonPrimitive)?.longOrNull ?: 0L,
            europeanAqi = c.get("european_aqi"),
            pm10 = c.get("pm10"),
            pm25 = c.get("pm2_5"),
            ozone = c.get("ozone"),
            nitrogenDioxide = c.get("nitrogen_dioxide"),
            pollen = mapOf(
                Pollen.GRASS to c.get("grass_pollen"),
                Pollen.BIRCH to c.get("birch_pollen"),
                Pollen.ALDER to c.get("alder_pollen"),
                Pollen.OLIVE to c.get("olive_pollen"),
                Pollen.MUGWORT to c.get("mugwort_pollen"),
                Pollen.RAGWEED to c.get("ragweed_pollen"),
            ),
        )
    }

    @Serializable
    data class GeoResult(
        val id: Long = 0,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val elevation: Double? = null,
        val country: String? = null,
        val country_code: String? = null,
        val admin1: String? = null,
        val admin2: String? = null,
        val postcodes: List<String>? = null,
        val population: Long? = null,
        val feature_code: String? = null,
    ) {
        /** Département français à partir du code postal (2A/2B pour la Corse). */
        val department: String?
            get() {
                if (country_code != "FR") return null
                val cp = postcodes?.firstOrNull()?.take(5) ?: return null
                return departmentFromPostcode(cp)
            }
    }

    @Serializable
    private data class GeoResponse(val results: List<GeoResult>? = null)

    fun parseGeocoding(text: String): List<GeoResult> =
        AzurJson.decodeFromString(GeoResponse.serializer(), text).results.orEmpty()
}

fun departmentFromPostcode(postcode: String): String? {
    val cp = postcode.trim().take(5)
    if (cp.length < 2 || !cp.all { it.isDigit() }) return null
    return when {
        cp.startsWith("97") || cp.startsWith("98") -> cp.take(3)
        cp.startsWith("20") -> if (cp.length >= 3 && cp[2] < '2') "2A" else "2B"
        else -> cp.take(2)
    }
}
