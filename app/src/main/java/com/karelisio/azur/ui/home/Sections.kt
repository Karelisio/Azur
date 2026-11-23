package com.karelisio.azur.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.karelisio.azur.domain.DailyPoint
import com.karelisio.azur.domain.Forecast
import com.karelisio.azur.domain.RainNowcast
import com.karelisio.azur.domain.RainState
import com.karelisio.azur.domain.TimeText
import com.karelisio.azur.domain.Units
import com.karelisio.azur.ui.components.AnimatedNumber
import com.karelisio.azur.ui.components.AzurCard
import com.karelisio.azur.ui.components.LocalReduceMotion
import com.karelisio.azur.ui.components.WeatherIconImage
import com.karelisio.azur.ui.components.temperatureColor
import com.karelisio.azur.ui.theme.AzurColors
import com.karelisio.azur.ui.theme.AzurText
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val skyShadow = Shadow(Color(0x55000000), Offset(0f, 2f), 8f)

/** En-tête : grosse température, condition, ressenti, min/max, phrase de résumé. */
@Composable
fun HeroSection(
    forecast: Forecast,
    units: Units,
    summary: String,
    now: Long,
    icon: @Composable () -> Unit,
) {
    val current = forecast.current
    val today = forecast.today(now)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        AnimatedNumber(
            units.tempValue(current.temperature),
            style = AzurText.Hero.copy(shadow = skyShadow),
            color = Color.White,
        )
        Text(
            current.condition.label,
            style = MaterialTheme.typography.headlineSmall.copy(shadow = skyShadow),
            color = Color.White,
        )
        Spacer(Modifier.height(4.dp))
        val feels = current.apparentTemperature?.let { "Ressenti ${units.temp(it)}" }
        val range = today?.let { "↑ ${units.temp(it.temperatureMax)}  ↓ ${units.temp(it.temperatureMin)}" }
        Text(
            listOfNotNull(feels, range).joinToString("   ·   "),
            style = MaterialTheme.typography.titleMedium.copy(shadow = skyShadow),
            color = Color.White.copy(alpha = 0.92f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            summary,
            style = MaterialTheme.typography.titleLarge.copy(shadow = skyShadow, fontWeight = FontWeight.Medium),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

fun rainHeadline(n: RainNowcast, forecast: Forecast): String {
    val zone = forecast.zone
    return when (n.state) {
        RainState.DRY -> "Pas de pluie prévue dans l'heure"
        RainState.STARTING -> "Pluie ${n.intensityLabel} vers ${TimeText.hm(n.startsAt!!, zone)}" +
            (n.endsAt?.let { ", fin vers ${TimeText.hm(it, zone)}" } ?: "")
        RainState.ONGOING -> "Pluie ${n.intensityLabel} pendant au moins une heure"
        RainState.STOPPING -> "Fin de la pluie vers ${TimeText.hm(n.endsAt!!, zone)}"
    }
}

/** Pluie dans l'heure : barres par quart d'heure (les 4 premiers = l'heure qui vient). */
@Composable
fun RainHourCard(nowcast: RainNowcast, slots: List<com.karelisio.azur.domain.MinutelyPoint>, forecast: Forecast, units: Units) {
    if (slots.isEmpty()) return
    val reduce = LocalReduceMotion.current
    val grow = remember(slots.first().time) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(slots.first().time) {
        if (!reduce) { delay(250); grow.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
    }
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    AzurCard(title = "Pluie dans l'heure", icon = Icons.Rounded.Umbrella) {
        Text(rainHeadline(nowcast, forecast), style = MaterialTheme.typography.titleMedium)
        if (nowcast.rainExpected) {
            Text("Cumul sur l'heure : ${units.precip(nowcast.totalMm)}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val n = slots.size
            val gap = 6.dp.toPx()
            val w = (size.width - gap * (n - 1)) / n
            slots.forEachIndexed { i, s ->
                val x = i * (w + gap)
                drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(10.dp.toPx()))
                val frac = (s.precipitation / 2.0).coerceIn(0.0, 1.0).toFloat()
                val h = if (s.precipitation > 0.0) maxOf(frac * size.height, 6.dp.toPx()) else 3.dp.toPx()
                val hh = h * grow.value
                val alpha = if (i < 4) 1f else 0.45f
                drawRoundRect(
                    Brush.verticalGradient(listOf(AzurColors.Rain.copy(alpha = alpha), Color(0xFF7FB2FF).copy(alpha = alpha))),
                    Offset(x, size.height - hh), Size(w, hh), CornerRadius(10.dp.toPx()),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            slots.forEachIndexed { i, s ->
                Text(
                    if (i % 2 == 0) TimeText.hm(s.time, forecast.zone) else "",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun HourlyCard(forecast: Forecast, units: Units, now: Long) {
    AzurCard(title = "48 heures", icon = Icons.Rounded.Schedule) {
        HourlyStrip(forecast.hourlyFrom(now, 48), units, forecast.zone, now)
        Text(
            "Pluie : probabilité et cumul · Vent en ${units.wind.label}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Ligne d'un jour : nom, icône, pluie, barre min/max. */
@Composable
fun DayRow(
    day: DailyPoint,
    forecast: Forecast,
    units: Units,
    now: Long,
    globalMin: Double,
    globalMax: Double,
    currentTemp: Double?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().height(52.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            TimeText.dayName(day.time, now, forecast.zone),
            Modifier.width(92.dp),
            style = MaterialTheme.typography.titleSmall,
        )
        WeatherIconImage(day.condition, true, Modifier.size(30.dp))
        Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            val prob = day.precipitationProbabilityMax?.roundToInt()
            val text = when {
                prob != null && prob >= 20 -> "$prob %"
                (day.precipitationSum ?: 0.0) >= 0.5 -> units.precip(day.precipitationSum).substringBefore(' ') + " mm"
                else -> ""
            }
            Text(text, style = MaterialTheme.typography.labelMedium, color = AzurColors.Rain)
        }
        Text(units.temp(day.temperatureMin), Modifier.width(36.dp), textAlign = TextAlign.End,
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TemperatureRangeBar(day.temperatureMin, day.temperatureMax, globalMin, globalMax, currentTemp,
            Modifier.weight(1f).padding(horizontal = 10.dp).height(8.dp))
        Text(units.temp(day.temperatureMax), Modifier.width(36.dp), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun TemperatureRangeBar(min: Double, max: Double, gMin: Double, gMax: Double, current: Double?, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val dotRing = MaterialTheme.colorScheme.surface
    Canvas(modifier) {
        val range = (gMax - gMin).takeIf { it > 0 } ?: 1.0
        val r = size.height / 2
        drawRoundRect(track, cornerRadius = CornerRadius(r))
        val x0 = ((min - gMin) / range * size.width).toFloat()
        val x1 = ((max - gMin) / range * size.width).toFloat().coerceAtLeast(x0 + size.height)
        drawRoundRect(
            Brush.horizontalGradient(listOf(temperatureColor(min), temperatureColor(max)), startX = x0, endX = x1),
            topLeft = Offset(x0, 0f), size = Size(x1 - x0, size.height), cornerRadius = CornerRadius(r),
        )
        if (current != null) {
            val cx = ((current - gMin) / range * size.width).toFloat().coerceIn(r, size.width - r)
            drawCircle(dotRing, radius = r + 2.dp.toPx(), center = Offset(cx, r))
            drawCircle(Color.White, radius = r, center = Offset(cx, r))
        }
    }
}

@Composable
fun DailyCardTitle(count: Int) {
    com.karelisio.azur.ui.components.CardTitle("$count jours", Icons.Rounded.CalendarMonth)
}

@Composable
fun SourcesFooter(fetchedAt: Long) {
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(com.karelisio.azur.ui.components.relativeUpdated(fetchedAt), style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f))
        Text(
            "Données Open-Meteo · modèles Météo-France (AROME, ARPEGE) · qualité de l'air CAMS",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
