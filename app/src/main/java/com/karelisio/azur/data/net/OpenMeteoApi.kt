package com.karelisio.azur.data.net

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Locale

class OpenMeteoApi(private val http: Http) {

    suspend fun forecast(lat: Double, lon: Double): String = http.get(forecastUrl(lat, lon).toString())

    /** Prévision recalculée à une altitude donnée (module montagne). */
    suspend fun forecastAtAltitude(lat: Double, lon: Double, elevation: Int): String =
        http.get(altitudeUrl(lat, lon, elevation).toString())

    /** Requête légère pour la pluie dans l'heure (notifications). */
    suspend fun nowcast(lat: Double, lon: Double): String = http.get(nowcastUrl(lat, lon).toString())

    suspend fun airQuality(lat: Double, lon: Double): String = http.get(airQualityUrl(lat, lon).toString())

    suspend fun search(query: String): List<OpenMeteoParser.GeoResult> =
        OpenMeteoParser.parseGeocoding(http.get(searchUrl(query).toString()))

    companion object {
        const val MODELS = "meteofrance_seamless,best_match"

        val CURRENT = listOf(
            "temperature_2m", "apparent_temperature", "relative_humidity_2m", "is_day", "weather_code",
            "precipitation", "cloud_cover", "pressure_msl", "wind_speed_10m", "wind_direction_10m",
            "wind_gusts_10m", "dew_point_2m", "visibility", "uv_index",
        )
        val HOURLY = listOf(
            "temperature_2m", "apparent_temperature", "precipitation_probability", "precipitation", "weather_code",
            "wind_speed_10m", "wind_direction_10m", "wind_gusts_10m", "is_day", "relative_humidity_2m",
            "dew_point_2m", "pressure_msl", "visibility", "uv_index", "cloud_cover", "snowfall",
            "freezing_level_height", "temperature_850hPa", "wind_speed_850hPa", "wind_direction_850hPa",
            "geopotential_height_850hPa", "temperature_700hPa", "wind_speed_700hPa", "wind_direction_700hPa",
            "geopotential_height_700hPa",
        )
        val DAILY = listOf(
            "weather_code", "temperature_2m_max", "temperature_2m_min", "precipitation_sum",
            "precipitation_probability_max", "sunrise", "sunset", "uv_index_max", "wind_speed_10m_max",
            "wind_gusts_10m_max", "wind_direction_10m_dominant", "snowfall_sum",
        )

        private fun coord(v: Double) = String.format(Locale.US, "%.4f", v)

        private fun base(url: String, lat: Double, lon: Double): HttpUrl.Builder = url.toHttpUrl().newBuilder()
            .addQueryParameter("latitude", coord(lat))
            .addQueryParameter("longitude", coord(lon))
            .addQueryParameter("timezone", "auto")
            .addQueryParameter("timeformat", "unixtime")

        fun forecastUrl(lat: Double, lon: Double): HttpUrl = base("https://api.open-meteo.com/v1/forecast", lat, lon)
            .addQueryParameter("models", MODELS)
            .addQueryParameter("current", CURRENT.joinToString(","))
            .addQueryParameter("minutely_15", "precipitation")
            .addQueryParameter("forecast_minutely_15", "12")
            .addQueryParameter("hourly", HOURLY.joinToString(","))
            .addQueryParameter("daily", DAILY.joinToString(","))
            .addQueryParameter("forecast_days", "14")
            .build()

        fun altitudeUrl(lat: Double, lon: Double, elevation: Int): HttpUrl =
            base("https://api.open-meteo.com/v1/forecast", lat, lon)
                .addQueryParameter("models", MODELS)
                .addQueryParameter("elevation", elevation.toString())
                .addQueryParameter("current", "temperature_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m,is_day")
                .addQueryParameter("hourly", "temperature_2m,apparent_temperature,weather_code,precipitation,snowfall,wind_speed_10m,wind_direction_10m,wind_gusts_10m,is_day")
                .addQueryParameter("forecast_days", "2")
                .build()

        fun nowcastUrl(lat: Double, lon: Double): HttpUrl = base("https://api.open-meteo.com/v1/forecast", lat, lon)
            .addQueryParameter("models", MODELS)
            .addQueryParameter("current", "temperature_2m,weather_code,is_day")
            .addQueryParameter("minutely_15", "precipitation")
            .addQueryParameter("forecast_minutely_15", "8")
            .addQueryParameter("forecast_days", "1")
            .build()

        fun airQualityUrl(lat: Double, lon: Double): HttpUrl =
            base("https://air-quality-api.open-meteo.com/v1/air-quality", lat, lon)
                .addQueryParameter(
                    "current",
                    "european_aqi,pm10,pm2_5,ozone,nitrogen_dioxide,grass_pollen,birch_pollen,alder_pollen,olive_pollen,mugwort_pollen,ragweed_pollen",
                )
                .build()

        fun searchUrl(query: String): HttpUrl = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", query.trim())
            .addQueryParameter("count", "20")
            .addQueryParameter("language", "fr")
            .addQueryParameter("format", "json")
            .build()
    }
}
