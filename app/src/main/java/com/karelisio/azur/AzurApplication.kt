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

class AzurApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Injection de dépendances manuelle. */
class AppContainer(context: Context) {
    val http = Http()
    val database = AzurDatabase.build(context)
    val settings = SettingsRepository(context)
    val updates = UpdateRepository(context, http, settings)
    val openMeteo = OpenMeteoApi(http)
    val places = PlaceRepository(database, openMeteo)
    val weather = WeatherRepository(database, openMeteo, settings)
    val location = LocationProvider(context)
    val locationUpdater = CurrentLocationUpdater(location, places)

    /** Appelé après chaque modification des réglages (planification des tâches, widgets…). */
    suspend fun onSettingsChanged() = Unit
}

val Context.container: AppContainer get() = (applicationContext as AzurApplication).container
