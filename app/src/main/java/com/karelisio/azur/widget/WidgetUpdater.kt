package com.karelisio.azur.widget

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import androidx.glance.appwidget.updateAll
import com.karelisio.azur.tile.TemperatureTileService

/** Rafraîchit l'affichage de tous les widgets et de la tuile à partir du cache. */
object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        runCatching {
            CompactWidget().updateAll(context)
            HourlyWidget().updateAll(context)
            WeekWidget().updateAll(context)
            ClockWidget().updateAll(context)
            RainWidget().updateAll(context)
        }
        runCatching { TileService.requestListeningState(context, ComponentName(context, TemperatureTileService::class.java)) }
    }
}
