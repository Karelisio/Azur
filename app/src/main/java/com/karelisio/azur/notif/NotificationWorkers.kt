package com.karelisio.azur.notif

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.karelisio.azur.container
import com.karelisio.azur.data.net.OpenMeteoParser
import com.karelisio.azur.domain.RainDetector
import com.karelisio.azur.domain.RainState
import com.karelisio.azur.domain.SummaryGenerator
import com.karelisio.azur.domain.TimeText
import com.karelisio.azur.domain.Vigilance
import com.karelisio.azur.work.WorkScheduler
import kotlin.math.abs
import kotlin.math.roundToInt

/** Toutes les 15 min : pluie dans l'heure pour la position actuelle. */
class RainAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val s = c.settings.current()
        if (!s.rainAlerts) return Result.success()
        runCatching { c.locationUpdater.update(useFreshFix = false) }
        val place = c.places.current() ?: c.places.primary() ?: return Result.success()
        val forecast = runCatching { OpenMeteoParser.parseForecast(c.openMeteo.nowcast(place.latitude, place.longitude)) }
            .getOrElse { return Result.retry() }
        val now = System.currentTimeMillis() / 1000
        val n = RainDetector.detect(forecast.minutely, now)
        if (n.state == RainState.STARTING && n.startsAt != null) {
            // Une seule alerte par épisode : ignore si un début proche (< 45 min) a déjà été notifié.
            if (abs(n.startsAt - s.lastRainNotifiedStart) > 45 * 60) {
                val minutes = ((n.startsAt - now) / 60).coerceAtLeast(0)
                Notifier.rain(
                    applicationContext,
                    title = "Pluie vers ${TimeText.hm(n.startsAt, forecast.zone)} · ${place.name}",
                    text = "Pluie ${n.intensityLabel} attendue dans environ $minutes min" +
                        " (${s.units.precip(n.totalMm)} sur l'heure)" +
                        (n.endsAt?.let { ", fin vers ${TimeText.hm(it, forecast.zone)}" } ?: "") + ".",
                    icon = com.karelisio.azur.R.drawable.ic_wx_rain,
                )
                c.settings.setLastRainNotified(n.startsAt)
            }
        }
        return Result.success()
    }
}

/** Résumé du matin à l'heure choisie ; se replanifie pour le lendemain. */
class MorningSummaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val s = c.settings.current()
        if (!s.morningEnabled) return Result.success()
        try {
            runCatching { c.locationUpdater.update(useFreshFix = false) }
            val place = c.places.primary() ?: return Result.success()
            c.weather.refresh(place)
            val data = c.weather.cached(place.id) ?: return Result.success()
            val f = data.forecast
            val now = System.currentTimeMillis() / 1000
            val today = f.today(now)
            val units = s.units
            val details = buildList {
                add(SummaryGenerator.generate(f, now, units) + ".")
                today?.let { d ->
                    d.precipitationProbabilityMax?.let { add("Pluie : ${it.roundToInt()} %, ${units.precip(d.precipitationSum ?: 0.0)}.") }
                    d.windGustsMax?.takeIf { it >= 40 }?.let { add("Rafales jusqu'à ${units.wind(it)}.") }
                    d.uvIndexMax?.takeIf { it >= 6 }?.let { add("UV élevés (${it.roundToInt()}).") }
                }
                data.vigilance?.takeIf { it.level >= 2 }?.let { add("Vigilance ${it.levelName} : ${it.active.joinToString { p -> p.name.lowercase() }}.") }
            }
            Notifier.morning(
                applicationContext,
                title = "${place.name} · ${today?.let { "${units.temp(it.temperatureMax)} / ${units.temp(it.temperatureMin)}" } ?: units.temp(f.current.temperature)}" +
                    " · ${today?.condition?.label ?: f.current.condition.label}",
                text = details.joinToString(" "),
                icon = (today?.condition ?: f.current.condition).icon.res(true),
            )
        } finally {
            WorkScheduler.scheduleMorning(applicationContext, s)
        }
        return Result.success()
    }
}

/** Toutes les heures (si une clé est saisie) : vigilance du département, notification si elle change. */
class VigilanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val s = c.settings.current()
        if (s.vigilanceApiKey.isBlank()) return Result.success()
        val place = c.places.primary() ?: return Result.success()
        val v = runCatching { c.weather.refreshVigilance(place) }.getOrElse { return Result.retry() } ?: return Result.success()
        val key = "${v.department}:${v.level}:${v.active.joinToString(",") { "${it.id}${it.level}" }}"
        if (v.level >= 2 && key != s.lastVigilanceNotified) {
            Notifier.vigilance(
                applicationContext,
                title = "Vigilance ${v.levelName} · ${place.name}",
                text = v.active.joinToString(" · ") { "${it.name} (${Vigilance.levelName(it.level)})" } +
                    (v.tomorrowLevel?.let { "\nDemain : vigilance ${Vigilance.levelName(it)}." } ?: ""),
                color = vigilanceColor(v.level),
            )
        }
        if (key != s.lastVigilanceNotified) c.settings.setLastVigilanceNotified(key)
        com.karelisio.azur.widget.WidgetUpdater.updateAll(applicationContext)
        return Result.success()
    }
}

fun vigilanceColor(level: Int): Int = when (level) {
    4 -> 0xFFE3001B.toInt()
    3 -> 0xFFFF8C00.toInt()
    2 -> 0xFFFFD400.toInt()
    else -> 0xFF2EAD4B.toInt()
}
