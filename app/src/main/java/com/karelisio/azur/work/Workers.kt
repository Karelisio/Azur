package com.karelisio.azur.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.karelisio.azur.container
import com.karelisio.azur.data.AppSettings
import com.karelisio.azur.widget.WidgetUpdater
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

    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun apply(context: Context, settings: AppSettings) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            WIDGETS, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES).setConstraints(network).build(),
        )
    }
}
