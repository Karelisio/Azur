package com.karelisio.azur.domain

import androidx.annotation.DrawableRes
import com.karelisio.azur.R

/** Famille d'icône (utilisée par les icônes animées Compose et les vecteurs des widgets). */
enum class WeatherIcon(@param:DrawableRes val dayRes: Int, @param:DrawableRes val nightRes: Int) {
    CLEAR(R.drawable.ic_wx_clear_day, R.drawable.ic_wx_clear_night),
    PARTLY_CLOUDY(R.drawable.ic_wx_partly_day, R.drawable.ic_wx_partly_night),
    CLOUDY(R.drawable.ic_wx_cloudy, R.drawable.ic_wx_cloudy),
    FOG(R.drawable.ic_wx_fog, R.drawable.ic_wx_fog),
    DRIZZLE(R.drawable.ic_wx_drizzle, R.drawable.ic_wx_drizzle),
    RAIN(R.drawable.ic_wx_rain, R.drawable.ic_wx_rain),
    HEAVY_RAIN(R.drawable.ic_wx_heavy_rain, R.drawable.ic_wx_heavy_rain),
    SLEET(R.drawable.ic_wx_sleet, R.drawable.ic_wx_sleet),
    SNOW(R.drawable.ic_wx_snow, R.drawable.ic_wx_snow),
    STORM(R.drawable.ic_wx_storm, R.drawable.ic_wx_storm),
    ;

    @DrawableRes fun res(isDay: Boolean): Int = if (isDay) dayRes else nightRes
}

/** Type de ciel pour le fond animé. */
enum class SkyKind { CLEAR, CLOUDY, FOG, RAIN, SNOW, STORM }

enum class PrecipKind { NONE, DRIZZLE, RAIN, SNOW, SLEET, STORM }

/** Conditions issues des codes WMO (Open-Meteo). */
enum class WeatherCondition(
    val label: String,
    val icon: WeatherIcon,
    val sky: SkyKind,
    val precip: PrecipKind = PrecipKind.NONE,
    /** 0..1 : couverture nuageuse indicative pour le fond. */
    val cloudiness: Float = 0f,
) {
    CLEAR("Ensoleillé", WeatherIcon.CLEAR, SkyKind.CLEAR),
    MOSTLY_CLEAR("Peu nuageux", WeatherIcon.CLEAR, SkyKind.CLEAR, cloudiness = 0.2f),
    PARTLY_CLOUDY("Partiellement nuageux", WeatherIcon.PARTLY_CLOUDY, SkyKind.CLOUDY, cloudiness = 0.5f),
    OVERCAST("Couvert", WeatherIcon.CLOUDY, SkyKind.CLOUDY, cloudiness = 1f),
    FOG("Brouillard", WeatherIcon.FOG, SkyKind.FOG, cloudiness = 0.9f),
    RIME_FOG("Brouillard givrant", WeatherIcon.FOG, SkyKind.FOG, cloudiness = 0.9f),
    DRIZZLE_LIGHT("Bruine légère", WeatherIcon.DRIZZLE, SkyKind.RAIN, PrecipKind.DRIZZLE, 0.9f),
    DRIZZLE("Bruine", WeatherIcon.DRIZZLE, SkyKind.RAIN, PrecipKind.DRIZZLE, 0.9f),
    DRIZZLE_DENSE("Bruine forte", WeatherIcon.DRIZZLE, SkyKind.RAIN, PrecipKind.DRIZZLE, 1f),
    FREEZING_DRIZZLE("Bruine verglaçante", WeatherIcon.SLEET, SkyKind.RAIN, PrecipKind.SLEET, 1f),
    RAIN_LIGHT("Pluie faible", WeatherIcon.RAIN, SkyKind.RAIN, PrecipKind.RAIN, 0.9f),
    RAIN("Pluie", WeatherIcon.RAIN, SkyKind.RAIN, PrecipKind.RAIN, 1f),
    RAIN_HEAVY("Forte pluie", WeatherIcon.HEAVY_RAIN, SkyKind.RAIN, PrecipKind.RAIN, 1f),
    FREEZING_RAIN("Pluie verglaçante", WeatherIcon.SLEET, SkyKind.RAIN, PrecipKind.SLEET, 1f),
    SNOW_LIGHT("Neige faible", WeatherIcon.SNOW, SkyKind.SNOW, PrecipKind.SNOW, 0.9f),
    SNOW("Neige", WeatherIcon.SNOW, SkyKind.SNOW, PrecipKind.SNOW, 1f),
    SNOW_HEAVY("Forte neige", WeatherIcon.SNOW, SkyKind.SNOW, PrecipKind.SNOW, 1f),
    SNOW_GRAINS("Grains de neige", WeatherIcon.SNOW, SkyKind.SNOW, PrecipKind.SNOW, 0.9f),
    SHOWERS_LIGHT("Averses faibles", WeatherIcon.RAIN, SkyKind.RAIN, PrecipKind.RAIN, 0.6f),
    SHOWERS("Averses", WeatherIcon.RAIN, SkyKind.RAIN, PrecipKind.RAIN, 0.7f),
    SHOWERS_HEAVY("Fortes averses", WeatherIcon.HEAVY_RAIN, SkyKind.RAIN, PrecipKind.RAIN, 0.9f),
    SNOW_SHOWERS_LIGHT("Averses de neige", WeatherIcon.SNOW, SkyKind.SNOW, PrecipKind.SNOW, 0.7f),
    SNOW_SHOWERS_HEAVY("Fortes averses de neige", WeatherIcon.SNOW, SkyKind.SNOW, PrecipKind.SNOW, 0.9f),
    THUNDERSTORM("Orages", WeatherIcon.STORM, SkyKind.STORM, PrecipKind.STORM, 1f),
    THUNDERSTORM_HAIL("Orages avec grêle", WeatherIcon.STORM, SkyKind.STORM, PrecipKind.STORM, 1f),
    UNKNOWN("—", WeatherIcon.CLOUDY, SkyKind.CLOUDY, cloudiness = 0.5f),
    ;

    val isWet: Boolean get() = precip != PrecipKind.NONE

    companion object {
        fun fromWmo(code: Int?): WeatherCondition = when (code) {
            0 -> CLEAR
            1 -> MOSTLY_CLEAR
            2 -> PARTLY_CLOUDY
            3 -> OVERCAST
            45 -> FOG
            48 -> RIME_FOG
            51 -> DRIZZLE_LIGHT
            53 -> DRIZZLE
            55 -> DRIZZLE_DENSE
            56, 57 -> FREEZING_DRIZZLE
            61 -> RAIN_LIGHT
            63 -> RAIN
            65 -> RAIN_HEAVY
            66, 67 -> FREEZING_RAIN
            71 -> SNOW_LIGHT
            73 -> SNOW
            75 -> SNOW_HEAVY
            77 -> SNOW_GRAINS
            80 -> SHOWERS_LIGHT
            81 -> SHOWERS
            82 -> SHOWERS_HEAVY
            85 -> SNOW_SHOWERS_LIGHT
            86 -> SNOW_SHOWERS_HEAVY
            95 -> THUNDERSTORM
            96, 99 -> THUNDERSTORM_HAIL
            else -> UNKNOWN
        }
    }
}
