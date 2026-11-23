package com.karelisio.azur.domain

import kotlin.math.PI
import kotlin.math.cos

object Moon {
    private const val SYNODIC_DAYS = 29.530588853
    /** Nouvelle lune de référence : 6 janvier 2000, 18 h 14 UTC. */
    private const val REFERENCE_NEW_MOON = 947182440L

    /** Phase 0..1 (0 = nouvelle lune, 0,5 = pleine lune). */
    fun phase(epoch: Long): Double {
        val days = (epoch - REFERENCE_NEW_MOON) / 86400.0
        return ((days / SYNODIC_DAYS) % 1.0 + 1.0) % 1.0
    }

    fun illumination(phase: Double): Double = (1 - cos(2 * PI * phase)) / 2

    fun name(phase: Double): String = when {
        phase < 0.0339 || phase >= 0.9661 -> "Nouvelle lune"
        phase < 0.2161 -> "Premier croissant"
        phase < 0.2839 -> "Premier quartier"
        phase < 0.4661 -> "Gibbeuse croissante"
        phase < 0.5339 -> "Pleine lune"
        phase < 0.7161 -> "Gibbeuse décroissante"
        phase < 0.7839 -> "Dernier quartier"
        else -> "Dernier croissant"
    }

    /** Prochaine pleine lune après [epoch]. */
    fun nextFullMoon(epoch: Long): Long {
        val p = phase(epoch)
        val delta = ((0.5 - p + 1.0) % 1.0) * SYNODIC_DAYS * 86400
        return epoch + delta.toLong()
    }
}

object Sun {
    /** Position 0..1 du soleil entre lever et coucher (null la nuit). */
    fun dayFraction(now: Long, sunrise: Long?, sunset: Long?): Float? {
        if (sunrise == null || sunset == null || sunset <= sunrise) return null
        if (now < sunrise || now > sunset) return null
        return ((now - sunrise).toDouble() / (sunset - sunrise)).toFloat()
    }

    fun dayLength(sunrise: Long?, sunset: Long?): Long? =
        if (sunrise != null && sunset != null) sunset - sunrise else null
}
