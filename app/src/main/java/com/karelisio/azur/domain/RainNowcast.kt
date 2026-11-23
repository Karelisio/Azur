package com.karelisio.azur.domain

enum class RainState { DRY, STARTING, ONGOING, STOPPING }

/** Pluie dans l'heure, quart d'heure par quart d'heure (pas daté au début de l'intervalle). */
data class RainNowcast(
    val state: RainState,
    val slots: List<MinutelyPoint>,
    val startsAt: Long? = null,
    val endsAt: Long? = null,
    val maxMm: Double = 0.0,
    val totalMm: Double = 0.0,
) {
    val rainExpected: Boolean get() = state != RainState.DRY

    /** Intensité en mm/h à partir du max sur 15 min. */
    val intensityLabel: String
        get() = when {
            maxMm * 4 < 2.5 -> "faible"
            maxMm * 4 < 7.6 -> "modérée"
            else -> "forte"
        }
}

object RainDetector {
    /** Seuil de pluie significative par quart d'heure. */
    const val WET_MM = 0.1
    private const val SLOT = 900L

    fun detect(minutely: List<MinutelyPoint>, now: Long, slotCount: Int = 4): RainNowcast {
        val slots = minutely.filter { it.time + SLOT > now }.take(slotCount)
        if (slots.isEmpty()) return RainNowcast(RainState.DRY, emptyList())
        val wet = slots.map { it.precipitation >= WET_MM }
        val max = slots.maxOf { it.precipitation }
        val total = slots.sumOf { it.precipitation }
        if (wet.none { it }) return RainNowcast(RainState.DRY, slots, maxMm = max, totalMm = total)
        return if (wet.first()) {
            val firstDry = wet.indexOfFirst { !it }
            if (firstDry == -1) {
                RainNowcast(RainState.ONGOING, slots, startsAt = slots.first().time, maxMm = max, totalMm = total)
            } else {
                RainNowcast(RainState.STOPPING, slots, startsAt = slots.first().time, endsAt = slots[firstDry].time, maxMm = max, totalMm = total)
            }
        } else {
            val start = wet.indexOfFirst { it }
            val end = (start until wet.size).firstOrNull { !wet[it] }
            RainNowcast(
                RainState.STARTING, slots,
                startsAt = slots[start].time,
                endsAt = end?.let { slots[it].time },
                maxMm = max, totalMm = total,
            )
        }
    }
}
