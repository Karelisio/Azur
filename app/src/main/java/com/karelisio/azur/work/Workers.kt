package com.karelisio.azur.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.karelisio.azur.container
import com.karelisio.azur.data.AppSettings
import com.karelisio.azur.notif.MorningSummaryWorker
import com.karelisio.azur.notif.RainAlertWorker
import com.karelisio.azur.notif.VigilanceWorker
import com.karelisio.azur.widget.WidgetUpdater
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Toutes les 30 min : dernière position connue → météo → widgets et tuile. */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        runCatching { c.locationUpdater.update(useFreshFix = false) }
        val place = c.places.primary() ?: return Result.success()
        val ok = c.weather.refresh(place).isSuccess
        WidgetUpdater.updateAll(applicationContext)
        return if (ok || runAttemptCount >= 2) Result.success() else Result.retry()
    }
}

object WorkScheduler {
    const val WIDGETS = "azur-widgets"
    const val RAIN = "azur-rain"
    const val MORNING = "azur-morning"
    const val VIGILANCE = "azur-vigilance"

    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun apply(context: Context, settings: AppSettings) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            WIDGETS, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES).setConstraints(network).build(),
        )
        if (settings.rainAlerts) {
            wm.enqueueUniquePeriodicWork(
                RAIN, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RainAlertWorker>(15, TimeUnit.MINUTES).setConstraints(network).build(),
            )
        } else {
            wm.cancelUniqueWork(RAIN)
        }
        if (settings.vigilanceApiKey.isNotBlank()) {
            wm.enqueueUniquePeriodicWork(
                VIGILANCE, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<VigilanceWorker>(1, TimeUnit.HOURS).setConstraints(network).build(),
            )
        } else {
            wm.cancelUniqueWork(VIGILANCE)
        }
        scheduleMorning(context, settings)
    }

    /** Délai jusqu'à la prochaine occurrence de hh:mm (heure locale). */
    fun delayUntil(hour: Int, minute: Int, now: LocalDateTime = LocalDateTime.now()): Duration {
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now.plusMinutes(1))) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    fun scheduleMorning(context: Context, settings: AppSettings) {
        val wm = WorkManager.getInstance(context)
        if (!settings.morningEnabled) {
            wm.cancelUniqueWork(MORNING)
            return
        }
        val request = OneTimeWorkRequestBuilder<MorningSummaryWorker>()
            .setInitialDelay(delayUntil(settings.morningHour, settings.morningMinute))
            .build()
        wm.enqueueUniqueWork(MORNING, ExistingWorkPolicy.REPLACE, request)
    }
}
