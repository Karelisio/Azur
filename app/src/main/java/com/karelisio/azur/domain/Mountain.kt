package com.karelisio.azur.domain

import kotlin.math.max

/** Calculs du module montagne. */
object Mountain {
    /** Gradient thermique standard (°C/m). */
    const val LAPSE_RATE = 0.0065

    /** Écart moyen entre isotherme 0 °C et limite pluie-neige (m). */
    const val SNOW_LIMIT_OFFSET = 300.0

    /**
     * Isotherme 0 °C (m) : valeur du modèle si disponible, sinon interpolation entre les niveaux
     * 850 et 700 hPa, sinon extrapolation depuis la température au sol.
     */
    fun freezingLevel(h: HourlyPoint, stationElevation: Double): Double {
        h.freezingLevel?.let { return it }
        val t850 = h.temperature850
        val z850 = h.geopotential850
        val t700 = h.temperature700
        val z700 = h.geopotential700
        if (t850 != null && z850 != null && t700 != null && z700 != null) {
            return when {
                t850 > 0 && t700 <= 0 -> z850 + t850 / (t850 - t700) * (z700 - z850)
                t700 > 0 -> z700 + t700 / LAPSE_RATE
                h.temperature > 0 && stationElevation < z850 ->
                    stationElevation + h.temperature / (h.temperature - t850) * (z850 - stationElevation)
                else -> max(0.0, stationElevation + h.temperature / LAPSE_RATE)
            }
        }
        return max(0.0, stationElevation + h.temperature / LAPSE_RATE)
    }

    fun snowLimit(freezingLevel: Double): Double = max(0.0, freezingLevel - SNOW_LIMIT_OFFSET)

    /** Arrondi à 50 m pour l'affichage. */
    fun round50(m: Double): Int = (Math.round(m / 50.0) * 50).toInt()
}
