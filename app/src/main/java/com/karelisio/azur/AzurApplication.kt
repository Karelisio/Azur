package com.karelisio.azur

import android.app.Application
import android.content.Context
import com.karelisio.azur.data.CurrentLocationUpdater
import com.karelisio.azur.data.LocationProvider
import com.karelisio.azur.data.PlaceRepository
import com.karelisio.azur.data.SettingsRepository
import com.karelisio.azur.data.WeatherRepository
import com.karelisio.azur.data.db.AzurDatabase
import com.karelisio.azur.data.net.Http
import com.karelisio.azur.data.net.OpenMeteoApi
import com.karelisio.azur.update.UpdateRepository
import com.karelisio.azur.widget.WidgetUpdater
import com.karelisio.azur.work.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AzurApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch { container.onSettingsChanged() }
    }
}

/** Injection de dépendances manuelle. */
class AppContainer(private val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val http = Http()
    val database = AzurDatabase.build(context)
    val settings = SettingsRepository(context)
    val updates = UpdateRepository(context, http, settings)
    val openMeteo = OpenMeteoApi(http)
    val places = PlaceRepository(database, openMeteo)
    val weather = WeatherRepository(database, openMeteo, settings) { WidgetUpdater.updateAll(context) }
    val location = LocationProvider(context)
    val locationUpdater = CurrentLocationUpdater(location, places)

    /** Appelé après chaque modification des réglages (planification des tâches, widgets…). */
    suspend fun onSettingsChanged() {
        WorkScheduler.apply(context, settings.current())
        WidgetUpdater.updateAll(context)
    }
}

val Context.container: AppContainer get() = (applicationContext as AzurApplication).container
