package com.karelisio.azur.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

enum class TempUnit(val label: String) { CELSIUS("°C"), FAHRENHEIT("°F") }
enum class WindUnit(val label: String) { KMH("km/h"), MS("m/s"), MPH("mph"), KNOTS("nœuds") }
enum class PrecipUnit(val label: String) { MM("mm"), INCH("in") }
enum class PressureUnit(val label: String) { HPA("hPa"), MMHG("mmHg"), INHG("inHg") }

/** Préférences d'unités ; toutes les données internes sont en °C, km/h, mm, hPa. */
data class Units(
    val temp: TempUnit = TempUnit.CELSIUS,
    val wind: WindUnit = WindUnit.KMH,
    val precip: PrecipUnit = PrecipUnit.MM,
    val pressure: PressureUnit = PressureUnit.HPA,
) {
    fun tempValue(c: Double): Double = if (temp == TempUnit.FAHRENHEIT) c * 9 / 5 + 32 else c

    /** « 12° » (sans unité, style météo). */
    fun temp(c: Double?): String = c?.let { "${tempValue(it).roundToInt()}°" } ?: "–"

    fun tempFull(c: Double?): String = c?.let { "${tempValue(it).roundToInt()} ${temp.label}" } ?: "–"

    fun windValue(kmh: Double): Double = when (wind) {
        WindUnit.KMH -> kmh
        WindUnit.MS -> kmh / 3.6
        WindUnit.MPH -> kmh / 1.609344
        WindUnit.KNOTS -> kmh / 1.852
    }

    fun wind(kmh: Double?): String = kmh?.let { "${windValue(it).roundToInt()} ${wind.label}" } ?: "–"

    fun precipValue(mm: Double): Double = if (precip == PrecipUnit.INCH) mm / 25.4 else mm

    fun precip(mm: Double?): String = mm?.let {
        val v = precipValue(it)
        val fmt = if (precip == PrecipUnit.INCH) "0.00" else if (v < 10) "0.#" else "0"
        "${decimal(fmt).format(v)} ${precip.label}"
    } ?: "–"

    fun pressure(hpa: Double?): String = hpa?.let {
        when (pressure) {
            PressureUnit.HPA -> "${it.roundToInt()} hPa"
            PressureUnit.MMHG -> "${(it * 0.750062).roundToInt()} mmHg"
            PressureUnit.INHG -> "${decimal("0.00").format(it * 0.0295300)} inHg"
        }
    } ?: "–"

    companion object {
        private fun decimal(pattern: String) = DecimalFormat(pattern, DecimalFormatSymbols(Locale.FRANCE))
    }
}

/** Points cardinaux en français, 16 secteurs. */
fun windDirectionLabel(degrees: Double?): String {
    if (degrees == null) return ""
    val names = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO")
    val idx = (((degrees % 360) + 360) % 360 / 22.5).roundToInt() % 16
    return names[idx]
}
