package com.karelisio.azur.domain

import java.time.Instant
import java.time.temporal.ChronoUnit

/** Phrase de résumé : « Pluie vers 16 h, éclaircies ce soir ». */
object SummaryGenerator {
    private const val WINDOW_HOURS = 24

    fun isWetHour(h: HourlyPoint): Boolean =
        h.precipitation >= 0.2 ||
            (h.condition.isWet && h.precipitation >= RainDetector.WET_MM) ||
            (h.precipitationProbability ?: 0.0) >= 70

    private fun isClearHour(h: HourlyPoint) = !isWetHour(h) && h.weatherCode <= 2

    fun precipNoun(kind: PrecipKind): String = when (kind) {
        PrecipKind.SNOW -> "Neige"
        PrecipKind.STORM -> "Orages"
        PrecipKind.DRIZZLE -> "Bruine"
        PrecipKind.SLEET -> "Pluie verglaçante"
        else -> "Pluie"
    }

    private fun kindAround(hours: List<HourlyPoint>, epoch: Long): PrecipKind =
        hours.filter { it.condition.isWet }.minByOrNull { kotlin.math.abs(it.time - epoch) }?.condition?.precip
            ?.takeIf { it != PrecipKind.NONE } ?: PrecipKind.RAIN

    /** « vers 16 h » le jour même ou dans la nuit, sinon « demain matin ». */
    private fun whenText(epoch: Long, now: Long, f: Forecast): String {
        val zone = f.zone
        val days = ChronoUnit.DAYS.between(
            Instant.ofEpochSecond(now).atZone(zone).toLocalDate(),
            Instant.ofEpochSecond(epoch).atZone(zone).toLocalDate(),
        )
        val hour = Instant.ofEpochSecond(epoch).atZone(zone).hour
        return if (days == 0L || (days == 1L && hour < 5)) "vers ${TimeText.hm(epoch, zone)}"
        else TimeText.period(epoch, now, zone)
    }

    fun generate(f: Forecast, now: Long = f.current.time, units: Units = Units()): String {
        val zone = f.zone
        val hours = f.hourlyFrom(now, WINDOW_HOURS)
        val nowcast = RainDetector.detect(f.minutely, now)
        val parts = mutableListOf<String>()
        var rainEndIndex: Int? = null // index horaire après lequel chercher des éclaircies

        when (nowcast.state) {
            RainState.ONGOING -> {
                val noun = precipNoun(kindAround(hours, now))
                val end = hours.indexOfFirst { it.time > now && !isWetHour(it) }
                parts += if (end > 0) "$noun jusqu'à ${TimeText.hour(hours[end].time, zone)}" else "$noun durable"
                rainEndIndex = end.takeIf { it > 0 }
            }
            RainState.STOPPING -> {
                parts += "${precipNoun(kindAround(hours, now))}, fin vers ${TimeText.hm(nowcast.endsAt!!, zone)}"
                rainEndIndex = hours.indexOfFirst { it.time >= nowcast.endsAt }.takeIf { it >= 0 }
            }
            RainState.STARTING -> {
                parts += "${precipNoun(kindAround(hours, nowcast.startsAt!!))} vers ${TimeText.hm(nowcast.startsAt, zone)}"
                val startIdx = hours.indexOfFirst { it.time + 3600 > nowcast.startsAt }.coerceAtLeast(0)
                rainEndIndex = (startIdx until hours.size).firstOrNull { !isWetHour(hours[it]) && hours[it].time > nowcast.startsAt }
            }
            RainState.DRY -> {
                val wet = hours.indexOfFirst(::isWetHour)
                if (wet == 0) {
                    val noun = precipNoun(hours[0].condition.precip.takeIf { it != PrecipKind.NONE } ?: PrecipKind.RAIN)
                    val end = hours.indexOfFirst { !isWetHour(it) }
                    parts += if (end > 0) "$noun jusqu'à ${TimeText.hour(hours[end].time, zone)}" else "$noun durable"
                    rainEndIndex = end.takeIf { it > 0 }
                } else if (wet > 0) {
                    val h = hours[wet]
                    val noun = precipNoun(h.condition.precip.takeIf { it != PrecipKind.NONE } ?: PrecipKind.RAIN)
                    parts += "$noun ${whenText(h.time, now, f)}"
                    rainEndIndex = (wet until hours.size).firstOrNull { !isWetHour(hours[it]) }
                }
            }
        }

        if (parts.isNotEmpty()) {
            val from = rainEndIndex
            if (from != null) {
                hours.drop(from).firstOrNull(::isClearHour)?.let { parts += "éclaircies ${TimeText.period(it.time, now, zone)}" }
            }
        } else {
            parts += drySentence(f, hours, now)
        }

        val gust = hours.take(12).mapNotNull { it.windGusts }.maxOrNull()
        if (gust != null && gust >= 60) parts += "rafales jusqu'à ${units.wind(gust)}"
        return parts.joinToString(", ").replaceFirstChar { it.uppercase() }
    }

    private fun drySentence(f: Forecast, hours: List<HourlyPoint>, now: Long): String {
        val zone = f.zone
        val window = hours.take(12)
        if (window.isEmpty()) return f.current.condition.label
        val clear = window.count { it.weatherCode <= 1 }
        val overcast = window.count { it.weatherCode == 3 }
        val fog = f.current.condition.sky == SkyKind.FOG
        val clearing = window.drop(1).firstOrNull { it.weatherCode <= 2 }
        val isDay = f.current.isDay
        return when {
            fog -> clearing?.let { "Brouillard, éclaircies ${TimeText.period(it.time, now, zone)}" } ?: "Brouillard, temps sec"
            clear * 2 >= window.size -> if (isDay) "Temps sec et ensoleillé" else "Nuit claire, temps sec"
            overcast * 3 >= window.size * 2 ->
                if (f.current.weatherCode == 3 && clearing != null) "Ciel couvert, éclaircies ${TimeText.period(clearing.time, now, zone)}"
                else "Ciel couvert, temps sec"
            else -> "Alternance de nuages et d'éclaircies"
        }
    }
}
