package com.karelisio.azur.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Formats horaires à la française : « 16 h », « 14 h 15 ». */
object TimeText {
    fun hm(epoch: Long, zone: ZoneId): String {
        val t = Instant.ofEpochSecond(epoch).atZone(zone)
        return if (t.minute == 0) "${t.hour} h" else "${t.hour} h ${"%02d".format(t.minute)}"
    }

    fun hour(epoch: Long, zone: ZoneId): String = "${Instant.ofEpochSecond(epoch).atZone(zone).hour} h"

    /** « lun. 24 » */
    fun dayShort(epoch: Long, zone: ZoneId): String {
        val d = Instant.ofEpochSecond(epoch).atZone(zone)
        return "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.FRENCH)} ${d.dayOfMonth}"
    }

    fun weekday(epoch: Long, zone: ZoneId): String =
        Instant.ofEpochSecond(epoch).atZone(zone).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH)

    /** « Aujourd'hui », « Demain », sinon « mercredi ». */
    fun dayName(epoch: Long, now: Long, zone: ZoneId): String {
        val d = Instant.ofEpochSecond(epoch).atZone(zone).toLocalDate()
        val today = Instant.ofEpochSecond(now).atZone(zone).toLocalDate()
        return when (ChronoUnit.DAYS.between(today, d)) {
            0L -> "Aujourd'hui"
            1L -> "Demain"
            else -> weekday(epoch, zone).replaceFirstChar { it.titlecase(Locale.FRENCH) }
        }
    }

    /** Moment de la journée relatif à maintenant : « ce soir », « demain matin », « mercredi »… */
    fun period(epoch: Long, now: Long, zone: ZoneId): String {
        val t = Instant.ofEpochSecond(epoch).atZone(zone)
        val today = Instant.ofEpochSecond(now).atZone(zone).toLocalDate()
        val offset = ChronoUnit.DAYS.between(today, t.toLocalDate())
        val h = t.hour
        val night = h < 5 || h >= 23
        return when {
            offset == 0L && night -> "cette nuit"
            offset == 0L && h < 12 -> "ce matin"
            offset == 0L && h < 18 -> "cet après-midi"
            offset == 0L -> "ce soir"
            offset == 1L && h < 5 -> "cette nuit"
            offset == 1L && night -> "demain soir"
            offset == 1L && h < 12 -> "demain matin"
            offset == 1L && h < 18 -> "demain après-midi"
            offset == 1L -> "demain soir"
            else -> weekday(epoch, zone)
        }
    }
}
