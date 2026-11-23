package com.karelisio.azur.widget

import android.content.Context
import androidx.annotation.DrawableRes
import com.karelisio.azur.R
import com.karelisio.azur.container
import com.karelisio.azur.data.PlaceWeather
import com.karelisio.azur.data.WidgetBackground
import com.karelisio.azur.data.db.PlaceEntity
import com.karelisio.azur.domain.SkyKind
import com.karelisio.azur.domain.Units
import com.karelisio.azur.ui.sky.DayPhase
import com.karelisio.azur.ui.sky.dayPhase

/** Données nécessaires à un widget, lues dans le cache (aucun appel réseau). */
data class WidgetSnapshot(
    val place: PlaceEntity,
    val weather: PlaceWeather,
    val units: Units,
    val background: WidgetBackground,
) {
    val now: Long get() = System.currentTimeMillis() / 1000

    @get:DrawableRes
    val skyBackground: Int
        get() {
            val f = weather.forecast
            val today = f.today(now)
            val phase = dayPhase(now, today?.sunrise, today?.sunset, f.current.isDay)
            val kind = f.current.condition.sky
            return when (phase) {
                DayPhase.DAWN -> R.drawable.widget_sky_dawn
                DayPhase.DUSK -> R.drawable.widget_sky_dusk
                DayPhase.NIGHT -> if (kind == SkyKind.CLEAR) R.drawable.widget_sky_night else R.drawable.widget_sky_night_cloudy
                DayPhase.DAY -> when (kind) {
                    SkyKind.CLEAR -> R.drawable.widget_sky_day
                    SkyKind.CLOUDY -> R.drawable.widget_sky_cloudy
                    SkyKind.FOG -> R.drawable.widget_sky_fog
                    SkyKind.RAIN -> R.drawable.widget_sky_rain
                    SkyKind.STORM -> R.drawable.widget_sky_storm
                    SkyKind.SNOW -> R.drawable.widget_sky_snow
                }
            }
        }
}

object WidgetData {
    suspend fun load(context: Context): WidgetSnapshot? {
        val c = context.container
        val place = c.places.primary() ?: return null
        val weather = c.weather.cached(place.id) ?: return null
        val s = c.settings.current()
        return WidgetSnapshot(place, weather, s.units, s.widgetBackground)
    }
}
