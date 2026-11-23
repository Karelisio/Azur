package com.karelisio.azur.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karelisio.azur.domain.HourlyPoint
import com.karelisio.azur.domain.TimeText
import com.karelisio.azur.domain.Units
import com.karelisio.azur.ui.components.LocalReduceMotion
import com.karelisio.azur.ui.components.WeatherIconImage
import com.karelisio.azur.ui.components.temperatureColor
import com.karelisio.azur.ui.theme.AzurColors
import java.time.ZoneId
import kotlin.math.roundToInt

private val COLUMN = 58.dp

/** Courbe « overshoot » : les points dépassent puis reviennent (rebond). */
private fun overshoot(t: Float, tension: Float = 2.2f): Float {
    if (t <= 0f) return 0f
    if (t >= 1f) return 1f
    val x = t - 1f
    return x * x * ((tension + 1) * x + tension) + 1f
}

/** 48 h heure par heure : heure, icône, courbe de température animée, pluie, vent. */
@Composable
fun HourlyStrip(hours: List<HourlyPoint>, units: Units, zone: ZoneId, now: Long, modifier: Modifier = Modifier) {
    if (hours.isEmpty()) return
    val reduce = LocalReduceMotion.current
    val progress = remember(hours.first().time) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(hours.first().time) {
        if (!reduce) progress.animateTo(1f, tween(1400, easing = FastOutSlowInEasing))
    }
    val measurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val labelStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = onSurface)
    val temps = hours.map { units.tempValue(it.temperature) }
    val tMin = temps.min()
    val tMax = temps.max()

    Box(modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Column(Modifier.width(COLUMN * hours.size)) {
            Row {
                hours.forEachIndexed { i, h ->
                    val label = if (i == 0 && h.time <= now) "Maint." else TimeText.hour(h.time, zone)
                    val midnight = h.time != hours.first().time && TimeText.hour(h.time, zone) == "0 h"
                    Text(
                        if (midnight) TimeText.dayShort(h.time, zone) else label,
                        Modifier.width(COLUMN),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (midnight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (midnight) FontWeight.Bold else null,
                    )
                }
            }
            Row(Modifier.height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                hours.forEach { h ->
                    Box(Modifier.width(COLUMN), contentAlignment = Alignment.Center) {
                        WeatherIconImage(h.condition, h.isDay, Modifier.size(30.dp))
                    }
                }
            }
            // Courbe de températures : se dessine de gauche à droite, points qui rebondissent.
            Canvas(Modifier.fillMaxWidth().height(84.dp)) {
                val colW = size.width / hours.size
                val top = 26.dp.toPx()
                val bottom = size.height - 10.dp.toPx()
                val range = (tMax - tMin).takeIf { it > 0.5 } ?: 1.0
                val pts = temps.mapIndexed { i, t ->
                    Offset(colW * (i + 0.5f), (bottom - (t - tMin) / range * (bottom - top)).toFloat())
                }
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) {
                        val p0 = pts[i - 1]
                        val p1 = pts[i]
                        val cx = (p0.x + p1.x) / 2
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }
                val fill = Path().apply {
                    addPath(path)
                    lineTo(pts.last().x, size.height)
                    lineTo(pts.first().x, size.height)
                    close()
                }
                val revealX = size.width * progress.value
                val gradient = Brush.horizontalGradient(hours.map { temperatureColor(it.temperature) })
                clipRect(right = revealX) {
                    drawPath(fill, Brush.verticalGradient(listOf(Color(0x553D8BFF), Color.Transparent)))
                    drawPath(path, gradient, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
                pts.forEachIndexed { i, p ->
                    val local = ((revealX - p.x) / (colW * 1.5f)).coerceIn(0f, 1f)
                    val s = overshoot(local)
                    if (s <= 0f) return@forEachIndexed
                    drawCircle(Color.White, radius = 4.5.dp.toPx() * s, center = p)
                    drawCircle(temperatureColor(hours[i].temperature), radius = 3.dp.toPx() * s, center = p)
                    val text = "${temps[i].roundToInt()}°"
                    val layout = measurer.measure(text, labelStyle)
                    drawText(
                        layout,
                        topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height - 6.dp.toPx() * s),
                        alpha = local,
                    )
                }
            }
            // Pluie : probabilité et cumul.
            Row(Modifier.height(40.dp)) {
                hours.forEach { h ->
                    Column(Modifier.width(COLUMN), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        val prob = h.precipitationProbability?.roundToInt()
                        val wet = h.precipitation >= 0.1
                        Text(
                            prob?.let { "$it %" } ?: if (wet) "" else "–",
                            style = MaterialTheme.typography.labelSmall,
                            color = if ((prob ?: 0) >= 30 || wet) AzurColors.Rain else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (wet) {
                            Text(
                                units.precip(h.precipitation).substringBefore(' '),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AzurColors.Rain,
                            )
                        }
                    }
                }
            }
            // Vent : flèche orientée vers où va le vent + vitesse.
            Row(Modifier.height(40.dp)) {
                hours.forEach { h ->
                    Column(Modifier.width(COLUMN), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Navigation, null,
                            Modifier.size(16.dp).rotate(((h.windDirection ?: 0.0) + 180).toFloat()),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            h.windSpeed?.let { units.windValue(it).roundToInt().toString() } ?: "–",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
