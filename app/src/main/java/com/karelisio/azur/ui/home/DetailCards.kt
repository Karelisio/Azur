package com.karelisio.azur.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karelisio.azur.domain.Forecast
import com.karelisio.azur.domain.Moon
import com.karelisio.azur.domain.Sun
import com.karelisio.azur.domain.TimeText
import com.karelisio.azur.domain.Units
import com.karelisio.azur.domain.windDirectionLabel
import com.karelisio.azur.ui.components.AzurCard
import com.karelisio.azur.ui.components.LocalReduceMotion
import com.karelisio.azur.ui.components.dewPointComfort
import com.karelisio.azur.ui.components.levelColor
import com.karelisio.azur.ui.components.uvLevel
import com.karelisio.azur.ui.components.visibilityLabel
import com.karelisio.azur.ui.theme.AzurColors
import com.karelisio.azur.ui.theme.AzurText
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
private fun SmallCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, content: @Composable () -> Unit) {
    AzurCard(modifier = modifier, title = title, icon = icon, shape = MaterialTheme.shapes.large) { content() }
}

@Composable
private fun Value(text: String) = Text(text, style = AzurText.CardValue)

@Composable
private fun Caption(text: String) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

/** Jauge horizontale dégradée avec curseur. */
@Composable
fun GradientGauge(fraction: Float, colors: List<Color>, modifier: Modifier = Modifier) {
    val ring = MaterialTheme.colorScheme.surfaceContainer
    Canvas(modifier.fillMaxWidth().height(8.dp)) {
        val r = size.height / 2
        drawRoundRect(Brush.horizontalGradient(colors), cornerRadius = CornerRadius(r))
        val x = (fraction.coerceIn(0f, 1f) * size.width).coerceIn(r, size.width - r)
        drawCircle(ring, r + 3.dp.toPx(), Offset(x, r))
        drawCircle(Color.White, r + 1.dp.toPx(), Offset(x, r))
    }
}

@Composable
fun UvCard(uv: Double?, modifier: Modifier) = SmallCard("Indice UV", Icons.Rounded.WbSunny, modifier) {
    if (uv == null) { Value("–"); return@SmallCard }
    val (label, level) = uvLevel(uv)
    Value(uv.roundToInt().toString())
    Text(label, style = MaterialTheme.typography.titleSmall, color = levelColor(level))
    GradientGauge((uv / 11).toFloat(), listOf(AzurColors.Good, AzurColors.Moderate, AzurColors.Poor, AzurColors.VeryPoor, AzurColors.Extreme))
}

@Composable
fun WindCard(speed: Double?, gusts: Double?, direction: Double?, units: Units, modifier: Modifier) =
    SmallCard("Vent", Icons.Rounded.Air, modifier) {
        val measurer = rememberTextMeasurer()
        val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        val primary = MaterialTheme.colorScheme.primary
        val ring = MaterialTheme.colorScheme.outlineVariant
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Canvas(Modifier.size(64.dp)) {
                val r = size.minDimension / 2
                val c = center
                drawCircle(ring, r - 6.dp.toPx(), c, style = Stroke(1.dp.toPx()))
                for (i in 0 until 36) {
                    val a = i * 10f * PI.toFloat() / 180f
                    val len = if (i % 9 == 0) 6.dp.toPx() else 3.dp.toPx()
                    val r1 = r - 6.dp.toPx()
                    drawLine(ring, Offset(c.x + r1 * sin(a), c.y - r1 * cos(a)),
                        Offset(c.x + (r1 - len) * sin(a), c.y - (r1 - len) * cos(a)), 1.dp.toPx())
                }
                listOf("N" to 0f, "E" to 90f, "S" to 180f, "O" to 270f).forEach { (t, deg) ->
                    val layout = measurer.measure(t, TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor))
                    val a = deg * PI.toFloat() / 180f
                    val rr = r - 16.dp.toPx()
                    drawText(layout, topLeft = Offset(c.x + rr * sin(a) - layout.size.width / 2, c.y - rr * cos(a) - layout.size.height / 2))
                }
                if (direction != null) {
                    // La flèche indique où va le vent (direction d'origine + 180°).
                    rotate(direction.toFloat() + 180f, c) {
                        val tip = Offset(c.x, c.y - r + 10.dp.toPx())
                        val tail = Offset(c.x, c.y + r - 14.dp.toPx())
                        drawLine(primary, tail, tip, 3.dp.toPx(), StrokeCap.Round)
                        val head = Path().apply {
                            moveTo(tip.x, tip.y - 2.dp.toPx())
                            lineTo(tip.x - 6.dp.toPx(), tip.y + 8.dp.toPx())
                            lineTo(tip.x + 6.dp.toPx(), tip.y + 8.dp.toPx())
                            close()
                        }
                        drawPath(head, primary)
                    }
                }
            }
            Column {
                Value(speed?.let { units.windValue(it).roundToInt().toString() } ?: "–")
                Caption(units.wind.label)
            }
        }
        Text(
            listOfNotNull(direction?.let { windDirectionLabel(it) }, gusts?.let { "rafales ${units.wind(it)}" }).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
        )
    }

@Composable
fun HumidityCard(humidity: Double?, modifier: Modifier) = SmallCard("Humidité", Icons.Rounded.WaterDrop, modifier) {
    Value(humidity?.let { "${it.roundToInt()} %" } ?: "–")
    GradientGauge(((humidity ?: 0.0) / 100).toFloat(), listOf(Color(0xFFF2C12E), Color(0xFF4FD1C5), AzurColors.Rain))
    Caption(
        when {
            humidity == null -> ""
            humidity < 30 -> "Air très sec"
            humidity < 60 -> "Agréable"
            humidity < 80 -> "Humide"
            else -> "Très humide"
        },
    )
}

@Composable
fun DewPointCard(dew: Double?, units: Units, modifier: Modifier) = SmallCard("Point de rosée", Icons.Rounded.Thermostat, modifier) {
    Value(units.temp(dew))
    Caption(dew?.let { dewPointComfort(it) } ?: "")
}

@Composable
fun PressureCard(pressure: Double?, trend: Double?, units: Units, modifier: Modifier) =
    SmallCard("Pression", Icons.Rounded.Compress, modifier) {
        val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        val primary = MaterialTheme.colorScheme.primary
        Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.BottomCenter) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(2f)) {
                val stroke = 8.dp.toPx()
                val arcSize = Size(size.width - stroke, (size.width - stroke))
                val tl = Offset(stroke / 2, stroke / 2)
                drawArc(track, 180f, 180f, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                val f = (((pressure ?: 1013.0) - 960) / 100).toFloat().coerceIn(0f, 1f)
                drawArc(primary, 180f, 180f * f, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text(units.pressure(pressure).substringBefore(' '), style = AzurText.CardValue)
        }
        Caption(
            units.pressure.label + " · " + when {
                trend == null -> "stable"
                trend > 1.5 -> "en hausse ↗"
                trend < -1.5 -> "en baisse ↘"
                else -> "stable →"
            },
        )
    }

@Composable
fun VisibilityCard(visibility: Double?, modifier: Modifier) = SmallCard("Visibilité", Icons.Rounded.Visibility, modifier) {
    Value(visibility?.let { if (it >= 10_000) "${(it / 1000).roundToInt()} km" else "%.1f km".format(Locale.FRANCE, it / 1000) } ?: "–")
    Caption(visibility?.let { visibilityLabel(it) } ?: "")
}

/** Arc solaire : le soleil glisse jusqu'à sa position actuelle entre lever et coucher. */
@Composable
fun SunArcCard(forecast: Forecast, now: Long, modifier: Modifier = Modifier) {
    val today = forecast.today(now) ?: return
    val sunrise = today.sunrise ?: return
    val sunset = today.sunset ?: return
    val zone = forecast.zone
    val target = Sun.dayFraction(now, sunrise, sunset) ?: if (now > sunset) 1f else 0f
    val isDayNow = now in sunrise..sunset
    val reduce = LocalReduceMotion.current
    val pos = remember(sunrise) { Animatable(if (reduce) target else 0f) }
    LaunchedEffect(target) { if (!reduce) pos.animateTo(target, tween(1800, delayMillis = 300, easing = FastOutSlowInEasing)) }
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val horizon = MaterialTheme.colorScheme.outlineVariant
    AzurCard(modifier = modifier, title = "Soleil", icon = Icons.Rounded.WbTwilight, shape = MaterialTheme.shapes.large) {
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            val pad = 16.dp.toPx()
            val w = size.width - 2 * pad
            val baseY = size.height - 14.dp.toPx()
            val h = baseY - 12.dp.toPx()
            fun point(t: Float) = Offset(pad + w * t, baseY - h * sin(PI * t).toFloat())
            val path = Path().apply {
                moveTo(pad, baseY)
                var t = 0f
                while (t <= 1.001f) { val p = point(t); lineTo(p.x, p.y); t += 0.02f }
            }
            drawLine(horizon, Offset(0f, baseY), Offset(size.width, baseY), 1.dp.toPx())
            drawPath(path, track, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
            // Partie parcourue
            val done = Path().apply {
                moveTo(pad, baseY)
                var t = 0f
                while (t <= pos.value) { val p = point(t); lineTo(p.x, p.y); t += 0.02f }
                val p = point(pos.value); lineTo(p.x, p.y)
            }
            drawPath(done, Brush.horizontalGradient(listOf(Color(0xFFFFB74D), AzurColors.Sun)), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            val sun = point(pos.value)
            if (isDayNow || reduce || pos.value < 1f) {
                drawCircle(AzurColors.Sun.copy(alpha = 0.3f), 16.dp.toPx(), sun)
                drawCircle(AzurColors.Sun, 9.dp.toPx(), sun)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Caption("Lever"); Text(TimeText.hm(sunrise, zone), style = MaterialTheme.typography.titleMedium) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val len = sunset - sunrise
                Caption("Jour")
                Text("${len / 3600} h ${"%02d".format((len % 3600) / 60)}", style = MaterialTheme.typography.titleMedium)
            }
            Column(horizontalAlignment = Alignment.End) { Caption("Coucher"); Text(TimeText.hm(sunset, zone), style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
fun MoonCard(now: Long, forecast: Forecast, modifier: Modifier) = SmallCard("Lune", Icons.Rounded.DarkMode, modifier) {
    val phase = Moon.phase(now)
    val illum = Moon.illumination(phase)
    val dark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(Modifier.size(44.dp)) { drawMoon(phase, dark) }
        Text("${(illum * 100).roundToInt()} %", style = MaterialTheme.typography.titleLarge, maxLines = 1)
    }
    Text(Moon.name(phase), style = MaterialTheme.typography.titleSmall)
    val full = Instant.ofEpochSecond(Moon.nextFullMoon(now)).atZone(forecast.zone)
    Caption("Pleine lune le " + full.format(DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH)))
}

/** Dessine la Lune éclairée selon la phase (0 nouvelle, 0,5 pleine). */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMoon(phase: Double, darkColor: Color) {
    val r = size.minDimension / 2
    val c = center
    drawCircle(darkColor, r, c)
    val lit = Color(0xFFF5E9C8)
    val waxing = phase < 0.5
    // terminator : demi-ellipse dont la largeur dépend de la phase
    val k = cos(2 * PI * phase).toFloat() // 1 nouvelle, -1 pleine
    val path = Path().apply {
        val steps = 48
        // demi-cercle côté éclairé
        for (i in 0..steps) {
            val a = -PI / 2 + PI * i / steps
            val x = c.x + (if (waxing) 1 else -1) * r * cos(a).toFloat()
            val y = c.y + r * sin(a).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        // retour par le terminator
        for (i in steps downTo 0) {
            val a = -PI / 2 + PI * i / steps
            val x = c.x + (if (waxing) 1 else -1) * k * r * cos(a).toFloat()
            val y = c.y + r * sin(a).toFloat()
            lineTo(x, y)
        }
        close()
    }
    drawPath(path, lit)
}
