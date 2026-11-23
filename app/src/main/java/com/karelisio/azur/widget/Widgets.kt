package com.karelisio.azur.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.karelisio.azur.R
import com.karelisio.azur.domain.RainDetector
import com.karelisio.azur.domain.TimeText
import com.karelisio.azur.ui.home.rainHeadline
import kotlin.math.roundToInt

/** Base : charge l'instantané du cache puis affiche le contenu dans le thème Glance. */
abstract class AzurWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = runCatching { WidgetData.load(context) }.getOrNull()
        provideContent { GlanceTheme { Content(snapshot) } }
    }

    @Composable abstract fun Content(snapshot: WidgetSnapshot?)
}

// ---------- Compact : température + icône ----------
class CompactWidget : AzurWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(57.dp, 57.dp), DpSize(130.dp, 57.dp), DpSize(130.dp, 110.dp)))

    @Composable override fun Content(snapshot: WidgetSnapshot?) = WidgetContainer(snapshot, padding = 8.dp) {
        val s = snapshot!!
        val c = widgetColors(s)
        val cur = s.weather.forecast.current
        val size = LocalSize.current
        if (size.width < 120.dp) {
            Column(GlanceModifier.fillMaxWidth().fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                WxIcon(cur.condition, cur.isDay, 28.dp)
                Text(s.units.temp(cur.temperature), style = textStyle(c.primary, 18.sp, bold = true))
            }
        } else {
            Row(GlanceModifier.fillMaxWidth().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                WxIcon(cur.condition, cur.isDay, if (size.height > 100.dp) 56.dp else 40.dp)
                Spacer(GlanceModifier.width(8.dp))
                Column {
                    Text(s.units.temp(cur.temperature), style = textStyle(c.primary, if (size.height > 100.dp) 40.sp else 28.sp))
                    Text(s.place.name, style = textStyle(c.secondary, 12.sp, bold = true), maxLines = 1)
                    if (size.height > 100.dp) Text(cur.condition.label, style = textStyle(c.secondary, 12.sp), maxLines = 1)
                }
            }
        }
    }
}

// ---------- Horaire : 6 prochaines heures ----------
class HourlyWidget : AzurWidget() {
    override val sizeMode = SizeMode.Exact

    @Composable override fun Content(snapshot: WidgetSnapshot?) = WidgetContainer(snapshot) {
        val s = snapshot!!
        val c = widgetColors(s)
        val f = s.weather.forecast
        Column(GlanceModifier.fillMaxWidth()) {
            Header(s, c)
            Spacer(GlanceModifier.height(8.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                val hours = f.hourlyFrom(s.now, 7).drop(1).take(6)
                hours.forEach { h ->
                    Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(TimeText.hour(h.time, f.zone), style = textStyle(c.secondary, 12.sp))
                        WxIcon(h.condition, h.isDay, 28.dp)
                        Text(s.units.temp(h.temperature), style = textStyle(c.primary, 14.sp, bold = true))
                        val prob = h.precipitationProbability?.roundToInt()
                        if (prob != null && prob >= 20) Text("$prob %", style = textStyle(c.accent, 10.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(s: WidgetSnapshot, c: WidgetColors) {
    val f = s.weather.forecast
    val cur = f.current
    val today = f.today(s.now)
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        WxIcon(cur.condition, cur.isDay, 36.dp)
        Spacer(GlanceModifier.width(8.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(s.place.name, style = textStyle(c.primary, 14.sp, bold = true), maxLines = 1)
            Text(cur.condition.label, style = textStyle(c.secondary, 12.sp), maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(s.units.temp(cur.temperature), style = textStyle(c.primary, 28.sp))
            today?.let { Text("${s.units.temp(it.temperatureMax)} / ${s.units.temp(it.temperatureMin)}", style = textStyle(c.secondary, 11.sp)) }
        }
    }
}

// ---------- Semaine ----------
class WeekWidget : AzurWidget() {
    override val sizeMode = SizeMode.Exact

    @Composable override fun Content(snapshot: WidgetSnapshot?) = WidgetContainer(snapshot) {
        val s = snapshot!!
        val c = widgetColors(s)
        val f = s.weather.forecast
        val rows = ((LocalSize.current.height.value - 70) / 30).toInt().coerceIn(2, 7)
        Column(GlanceModifier.fillMaxWidth()) {
            Header(s, c)
            Spacer(GlanceModifier.height(6.dp))
            f.daily.take(rows).forEach { d ->
                Row(GlanceModifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(TimeText.dayShort(d.time, f.zone), style = textStyle(c.primary, 13.sp, bold = true), modifier = GlanceModifier.defaultWeight())
                    WxIcon(d.condition, true, 24.dp)
                    val prob = d.precipitationProbabilityMax?.roundToInt()
                    Text(if (prob != null && prob >= 20) "$prob %" else "", style = textStyle(c.accent, 11.sp), modifier = GlanceModifier.width(40.dp).padding(start = 6.dp))
                    Text(s.units.temp(d.temperatureMin), style = textStyle(c.secondary, 13.sp), modifier = GlanceModifier.width(36.dp))
                    Text(s.units.temp(d.temperatureMax), style = textStyle(c.primary, 13.sp, bold = true), modifier = GlanceModifier.width(36.dp))
                }
            }
        }
    }
}

// ---------- Horloge + météo ----------
class ClockWidget : AzurWidget() {
    override val sizeMode = SizeMode.Exact

    @Composable override fun Content(snapshot: WidgetSnapshot?) = WidgetContainer(snapshot) {
        val s = snapshot!!
        val c = widgetColors(s)
        val cur = s.weather.forecast.current
        val context = LocalContext.current
        Row(GlanceModifier.fillMaxWidth().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.defaultWeight()) {
                AndroidRemoteViews(RemoteViews(context.packageName, R.layout.widget_clock).apply {
                    if (s.background != com.karelisio.azur.data.WidgetBackground.SKY) {
                        val color = context.getColor(R.color.widget_text_dynamic)
                        setTextColor(R.id.clock_time, color)
                        setTextColor(R.id.clock_date, color)
                    }
                })
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WxIcon(cur.condition, cur.isDay, 40.dp)
                    Text(s.units.temp(cur.temperature), style = textStyle(c.primary, 30.sp))
                }
                Text(s.place.name, style = textStyle(c.secondary, 12.sp, bold = true), maxLines = 1)
            }
        }
    }
}

// ---------- Pluie dans l'heure ----------
class RainWidget : AzurWidget() {
    override val sizeMode = SizeMode.Exact

    @Composable override fun Content(snapshot: WidgetSnapshot?) = WidgetContainer(snapshot) {
        val s = snapshot!!
        val c = widgetColors(s)
        val f = s.weather.forecast
        val nowcast = RainDetector.detect(f.minutely, s.now)
        Column(GlanceModifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_wx_rain), null, GlanceModifier.width(22.dp).height(22.dp))
                Spacer(GlanceModifier.width(6.dp))
                Text("Pluie dans l'heure · ${s.place.name}", style = textStyle(c.secondary, 12.sp, bold = true), maxLines = 1)
            }
            Spacer(GlanceModifier.height(4.dp))
            Text(rainHeadline(nowcast, f), style = textStyle(c.primary, 15.sp, bold = true), maxLines = 2)
            Spacer(GlanceModifier.height(8.dp))
            val maxBar = 40
            Row(GlanceModifier.fillMaxWidth().height(maxBar.dp), verticalAlignment = Alignment.Bottom) {
                nowcast.slots.forEachIndexed { i, slot ->
                    val h = if (slot.precipitation <= 0.0) 3 else ((slot.precipitation / 2.0).coerceIn(0.1, 1.0) * maxBar).roundToInt()
                    Column(GlanceModifier.defaultWeight().padding(horizontal = 3.dp), verticalAlignment = Alignment.Bottom) {
                        Box(GlanceModifier.fillMaxWidth().height(h.dp).background(ImageProvider(R.drawable.widget_rain_bar))) {}
                    }
                }
            }
            Row(GlanceModifier.fillMaxWidth()) {
                nowcast.slots.forEach { slot ->
                    Text(TimeText.hm(slot.time, f.zone), style = textStyle(c.secondary, 10.sp), modifier = GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

class CompactWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = CompactWidget() }
class HourlyWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = HourlyWidget() }
class WeekWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = WeekWidget() }
class ClockWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = ClockWidget() }
class RainWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = RainWidget() }
