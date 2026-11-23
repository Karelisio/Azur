package com.karelisio.azur.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.karelisio.azur.domain.PrecipUnit
import com.karelisio.azur.domain.PressureUnit
import com.karelisio.azur.domain.TempUnit
import com.karelisio.azur.domain.Units
import com.karelisio.azur.domain.WindUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class WidgetBackground { DYNAMIC, SKY }

data class AppSettings(
    val units: Units = Units(),
    val onboardingDone: Boolean = false,
    val mountainEnabled: Boolean = false,
    val mountainAltitude: Int = 1800,
    val rainAlerts: Boolean = false,
    val morningEnabled: Boolean = false,
    val morningHour: Int = 7,
    val morningMinute: Int = 0,
    val widgetBackground: WidgetBackground = WidgetBackground.SKY,
    val vigilanceApiKey: String = "",
    val lastUpdateCheck: Long = 0,
    val lastRainNotifiedStart: Long = 0,
    val lastVigilanceNotified: String = "",
)

class SettingsRepository(private val context: Context) {
    private object K {
        val temp = stringPreferencesKey("unit_temp")
        val wind = stringPreferencesKey("unit_wind")
        val precip = stringPreferencesKey("unit_precip")
        val pressure = stringPreferencesKey("unit_pressure")
        val onboarding = booleanPreferencesKey("onboarding_done")
        val mountain = booleanPreferencesKey("mountain_enabled")
        val mountainAlt = intPreferencesKey("mountain_altitude")
        val rainAlerts = booleanPreferencesKey("rain_alerts")
        val morning = booleanPreferencesKey("morning_enabled")
        val morningHour = intPreferencesKey("morning_hour")
        val morningMinute = intPreferencesKey("morning_minute")
        val widgetBg = stringPreferencesKey("widget_background")
        val vigilanceKey = stringPreferencesKey("vigilance_api_key")
        val lastUpdateCheck = longPreferencesKey("last_update_check")
        val lastRain = longPreferencesKey("last_rain_notified")
        val lastVigilance = stringPreferencesKey("last_vigilance_notified")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    private inline fun <reified T : Enum<T>> Preferences.enum(key: Preferences.Key<String>, default: T): T =
        this[key]?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: default

    private fun Preferences.toSettings() = AppSettings(
        units = Units(
            temp = enum(K.temp, TempUnit.CELSIUS),
            wind = enum(K.wind, WindUnit.KMH),
            precip = enum(K.precip, PrecipUnit.MM),
            pressure = enum(K.pressure, PressureUnit.HPA),
        ),
        onboardingDone = this[K.onboarding] ?: false,
        mountainEnabled = this[K.mountain] ?: false,
        mountainAltitude = this[K.mountainAlt] ?: 1800,
        rainAlerts = this[K.rainAlerts] ?: false,
        morningEnabled = this[K.morning] ?: false,
        morningHour = this[K.morningHour] ?: 7,
        morningMinute = this[K.morningMinute] ?: 0,
        widgetBackground = enum(K.widgetBg, WidgetBackground.SKY),
        vigilanceApiKey = this[K.vigilanceKey] ?: "",
        lastUpdateCheck = this[K.lastUpdateCheck] ?: 0,
        lastRainNotifiedStart = this[K.lastRain] ?: 0,
        lastVigilanceNotified = this[K.lastVigilance] ?: "",
    )

    suspend fun setUnits(units: Units) = context.dataStore.edit {
        it[K.temp] = units.temp.name
        it[K.wind] = units.wind.name
        it[K.precip] = units.precip.name
        it[K.pressure] = units.pressure.name
    }

    suspend fun setOnboardingDone(done: Boolean = true) = context.dataStore.edit { it[K.onboarding] = done }
    suspend fun setMountain(enabled: Boolean) = context.dataStore.edit { it[K.mountain] = enabled }
    suspend fun setMountainAltitude(m: Int) = context.dataStore.edit { it[K.mountainAlt] = m }
    suspend fun setRainAlerts(enabled: Boolean) = context.dataStore.edit { it[K.rainAlerts] = enabled }
    suspend fun setMorning(enabled: Boolean, hour: Int, minute: Int) = context.dataStore.edit {
        it[K.morning] = enabled
        it[K.morningHour] = hour
        it[K.morningMinute] = minute
    }
    suspend fun setWidgetBackground(bg: WidgetBackground) = context.dataStore.edit { it[K.widgetBg] = bg.name }
    suspend fun setVigilanceKey(key: String) = context.dataStore.edit { it[K.vigilanceKey] = key.trim() }
    suspend fun setLastUpdateCheck(t: Long) = context.dataStore.edit { it[K.lastUpdateCheck] = t }
    suspend fun setLastRainNotified(t: Long) = context.dataStore.edit { it[K.lastRain] = t }
    suspend fun setLastVigilanceNotified(v: String) = context.dataStore.edit { it[K.lastVigilance] = v }
}
