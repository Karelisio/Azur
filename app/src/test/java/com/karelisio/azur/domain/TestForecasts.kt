package com.karelisio.azur.domain

/** Fabrique de prévisions synthétiques. Heure de référence : 23/11/2026 13 h 45 (Europe/Paris). */
object TestForecasts {
    const val NOW = 1795437900L // 13 h 45
    const val H13 = 1795435200L // 13 h 00

    fun hour(index: Int, code: Int = 3, mm: Double = 0.0, prob: Double? = null, gust: Double? = 10.0, temp: Double = 10.0) =
        HourlyPoint(time = H13 + index * 3600L, temperature = temp, weatherCode = code, precipitation = mm,
            precipitationProbability = prob, windGusts = gust, isDay = true)

    fun forecast(
        hours: List<HourlyPoint>,
        minutely: List<Double> = List(8) { 0.0 },
        currentCode: Int = hours.firstOrNull()?.weatherCode ?: 3,
        isDay: Boolean = true,
    ) = Forecast(
        latitude = 45.19, longitude = 5.72, elevation = 214.0, timezone = "Europe/Paris", utcOffsetSeconds = 3600,
        current = CurrentWeather(NOW, 10.0, 9.0, 70.0, isDay, currentCode, 0.0, 100.0, 1015.0, 10.0, 270.0, 20.0, 5.0, 20000.0, 1.0),
        minutely = minutely.mapIndexed { i, mm -> MinutelyPoint(NOW + i * 900L, mm) },
        hourly = hours,
        daily = emptyList(),
    )
}
