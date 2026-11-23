package com.karelisio.azur.ui.mountain

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karelisio.azur.data.PlaceWeather
import com.karelisio.azur.domain.Mountain
import com.karelisio.azur.domain.TimeText
import com.karelisio.azur.domain.Units
import com.karelisio.azur.domain.windDirectionLabel
import com.karelisio.azur.ui.components.AzurCard
import com.karelisio.azur.ui.components.WeatherIconImage
import com.karelisio.azur.ui.theme.AzurColors
import com.karelisio.azur.ui.theme.AzurText
import kotlin.math.roundToInt

/** Module montagne : isotherme, limite pluie-neige, neige, vent en altitude, météo à l'altitude choisie. */
@Composable
fun MountainCard(data: PlaceWeather, units: Units, now: Long, chosenAltitude: Int) {
    val f = data.forecast
    val hours = f.hourlyFrom(now, 48)
    val h0 = hours.firstOrNull() ?: return
    val freezing = Mountain.freezingLevel(h0, f.elevation)
    val snowLimit = Mountain.snowLimit(freezing)
    AzurCard(title = "Montagne", icon = Icons.Rounded.Landscape) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat("Isotherme 0 °C", "${Mountain.round50(freezing)} m", Modifier.weight(1f))
            Stat("Limite pluie-neige", "${Mountain.round50(snowLimit)} m", Modifier.weight(1f))
        }
        FreezingChart(hours.map { Mountain.freezingLevel(it, f.elevation) }, hours.map { it.time }, f.zone, chosenAltitude)
        Text(
            "Courbe : isotherme 0 °C sur 48 h · pointillés : votre altitude ($chosenAltitude m)",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Météo à l'altitude choisie (prévision recalculée par Open-Meteo pour cette altitude).
        data.altitude?.let { alt ->
            val c = alt.current
            Text("À ${data.altitudeMeters ?: chosenAltitude} m", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeatherIconImage(c.condition, c.isDay, Modifier.size(44.dp))
                Spacer(Modifier.width(10.dp))
                Text(units.temp(c.temperature), style = AzurText.CardValue)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(c.condition.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        listOfNotNull(
                            c.apparentTemperature?.let { "Ressenti ${units.temp(it)}" },
                            c.windSpeed?.let { "vent ${units.wind(it)}" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(Modifier.fillMaxWidth()) {
                alt.hourlyFrom(now, 13).drop(1).filterIndexed { i, _ -> i % 2 == 0 }.take(6).forEach { h ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(TimeText.hour(h.time, alt.zone), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        WeatherIconImage(h.condition, h.isDay, Modifier.size(24.dp))
                        Text(units.temp(h.temperature), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }

        // Chutes de neige
        val snow24 = hours.take(24).sumOf { it.snowfall ?: 0.0 }
        val snow48 = hours.sumOf { it.snowfall ?: 0.0 }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat("Neige 24 h", "${snow24.roundToInt()} cm", Modifier.weight(1f))
            Stat("Neige 48 h", "${snow48.roundToInt()} cm", Modifier.weight(1f))
        }
        val snowyDays = f.daily.filter { (it.snowfallSum ?: 0.0) >= 1 }.take(3)
        if (snowyDays.isNotEmpty()) {
            Text(
                "Neige prévue : " + snowyDays.joinToString { "${TimeText.dayName(it.time, now, f.zone).lowercase()} ${it.snowfallSum!!.roundToInt()} cm" },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Vent en altitude
        Text("Vent en altitude", style = MaterialTheme.typography.titleSmall)
        WindAloft("850 hPa", h0.geopotential850 ?: 1500.0, h0.windSpeed850, h0.windDirection850, h0.temperature850, units)
        WindAloft("700 hPa", h0.geopotential700 ?: 3000.0, h0.windSpeed700, h0.windDirection700, h0.temperature700, units)
        Text(
            "Limite pluie-neige estimée : isotherme 0 °C − 300 m.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = AzurText.CardValue)
    }
}

@Composable
private fun WindAloft(level: String, height: Double, speed: Double?, direction: Double?, temp: Double?, units: Units) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$level (~${Mountain.round50(height)} m)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(
            Icons.Rounded.Navigation, null,
            Modifier.size(18.dp).rotate(((direction ?: 0.0) + 180).toFloat()),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            listOfNotNull(speed?.let { units.wind(it) }, direction?.let { windDirectionLabel(it) }, temp?.let { units.temp(it) })
                .joinToString(" · ").ifBlank { "–" },
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun FreezingChart(levels: List<Double>, times: List<Long>, zone: java.time.ZoneId, chosen: Int) {
    if (levels.size < 2) return
    val measurer = rememberTextMeasurer()
    val label = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val minV = (minOf(levels.min(), chosen.toDouble()) - 200).coerceAtLeast(0.0)
        val maxV = maxOf(levels.max(), chosen.toDouble()) + 200
        val left = 40.dp.toPx()
        val bottom = size.height - 14.dp.toPx()
        fun y(v: Double) = (bottom - (v - minV) / (maxV - minV) * (bottom - 6.dp.toPx())).toFloat()
        fun x(i: Int) = left + (size.width - left) * i / (levels.size - 1)
        listOf(minV, (minV + maxV) / 2, maxV).forEach { v ->
            drawLine(grid, Offset(left, y(v)), Offset(size.width, y(v)), 1f)
            val t = measurer.measure("${Mountain.round50(v)}", TextStyle(fontSize = 9.sp, color = label))
            drawText(t, topLeft = Offset(0f, y(v) - t.size.height / 2))
        }
        drawLine(AzurColors.Warm, Offset(left, y(chosen.toDouble())), Offset(size.width, y(chosen.toDouble())), 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        val path = Path().apply {
            levels.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) }
        }
        drawPath(path, primary, style = Stroke(3.dp.toPx()))
        val snow = Path().apply {
            levels.forEachIndexed { i, v -> val s = Mountain.snowLimit(v); if (i == 0) moveTo(x(i), y(s)) else lineTo(x(i), y(s)) }
        }
        drawPath(snow, AzurColors.Snow, style = Stroke(2.dp.toPx()))
        times.forEachIndexed { i, t ->
            if (i % 12 == 0) {
                val txt = measurer.measure(TimeText.hour(t, zone), TextStyle(fontSize = 9.sp, color = label))
                drawText(txt, topLeft = Offset(x(i) - txt.size.width / 2, bottom + 2.dp.toPx()))
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Legend(primary, "Isotherme 0 °C")
        Legend(AzurColors.Snow, "Limite pluie-neige")
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}
