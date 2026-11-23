package com.karelisio.azur.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.karelisio.azur.domain.WeatherCondition
import com.karelisio.azur.domain.WeatherIcon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SunCore = Color(0xFFFFC83D)
private val SunRay = Color(0xFFFFB300)
private val MoonColor = Color(0xFFFFE08A)
private val CloudLight = Color(0xFFF1F4F8)
private val CloudShade = Color(0xFFAEBBCB)
private val CloudDark = Color(0xFFC9D3DF)
private val Rain = Color(0xFF3D8BFF)
private val Flake = Color(0xFF9CC8FF)
private val Bolt = Color(0xFFFFC83D)

/**
 * Icône météo animée dessinée sur une grille 48×48 (mêmes formes que les vecteurs des widgets) :
 * rayons qui tournent, nuages qui dérivent, gouttes et flocons qui tombent, éclairs qui clignotent.
 */
@Composable
fun AnimatedWeatherIcon(condition: WeatherCondition, isDay: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val t = rememberInfiniteTransition(label = "icon")
    val loop by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "loop")
    val slow by t.animateFloat(0f, 1f, infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "slow")
    val drift by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(3200), RepeatMode.Reverse), label = "drift")
    val p = if (reduce) 0.35f else loop
    val r = if (reduce) 0f else slow
    val d = if (reduce) 0f else drift
    Canvas(modifier.semantics { contentDescription = condition.label }) {
        val s = size.minDimension / 48f
        scale(s, s, pivot = Offset.Zero) {
            when (condition.icon) {
                WeatherIcon.CLEAR -> if (isDay) sun(24f, 24f, 9f, r) else moon(0f, 2f, 1f, d)
                WeatherIcon.PARTLY_CLOUDY -> {
                    if (isDay) sun(17f, 17f, 7f, r) else moon(-10f, -3f, 0.8f, d)
                    cloud(2f + d * 1.5f, 4f, 0.95f)
                }
                WeatherIcon.CLOUDY -> {
                    cloud(6f - d * 1.2f, -2f, 0.8f, CloudDark, Color(0xFF8E9CAD))
                    cloud(-2f + d * 1.5f, 4f, 0.95f)
                }
                WeatherIcon.FOG -> {
                    cloud(d, -6f, 1f)
                    fogLines(p)
                }
                WeatherIcon.DRIZZLE -> { drops(p, heavy = false, dots = true); cloud(d, -6f, 1f) }
                WeatherIcon.RAIN -> { drops(p, heavy = false); cloud(d, -6f, 1f) }
                WeatherIcon.HEAVY_RAIN -> { drops(p, heavy = true); cloud(d, -6f, 1f) }
                WeatherIcon.SLEET -> { drops(p, heavy = false); flakes(p, sparse = true); cloud(d, -6f, 1f) }
                WeatherIcon.SNOW -> { flakes(p, sparse = false); cloud(d, -6f, 1f) }
                WeatherIcon.STORM -> {
                    drops(p, heavy = false)
                    cloud(d, -7f, 1f, Color(0xFFD5DCE6), Color(0xFF7F8C9C))
                    val flash = if (reduce) 1f else if (p in 0.55f..0.62f || p in 0.7f..0.8f) 1f else 0.25f
                    bolt(flash)
                }
            }
        }
    }
}

private fun DrawScope.sun(cx: Float, cy: Float, radius: Float, rotation: Float) {
    rotate(rotation * 360f, Offset(cx, cy)) {
        for (i in 0 until 8) {
            val a = i * PI.toFloat() / 4
            val r1 = radius + 3f
            val r2 = radius + 6f
            drawLine(SunRay, Offset(cx + r1 * cos(a), cy + r1 * sin(a)), Offset(cx + r2 * cos(a), cy + r2 * sin(a)), 2.4f, StrokeCap.Round)
        }
    }
    drawCircle(SunCore.copy(alpha = 0.25f), radius + 1.5f, Offset(cx, cy))
    drawCircle(SunCore, radius, Offset(cx, cy))
}

private fun DrawScope.moon(dx: Float, dy: Float, s: Float, glow: Float) {
    fun pt(x: Float, y: Float) = Offset(dx + x * s, dy + y * s)
    drawCircle(MoonColor.copy(alpha = 0.12f + 0.06f * glow), 17f * s, pt(26f, 22f))
    val path = Path().apply {
        val a = pt(30f, 8f)
        moveTo(a.x, a.y)
        arcTo(Rect(pt(12f, 8f), Size(28f * s, 28f * s)), -60f, -250f, false)
        val b = pt(40f, 32f)
        lineTo(b.x, b.y)
        arcTo(Rect(pt(24f, 1f), Size(30f * s, 30f * s)), 115f, 103f, false)
        close()
    }
    drawPath(path, MoonColor)
}

private fun cloudPath(dx: Float, dy: Float, s: Float): Path = Path().apply {
    fun x(v: Float) = dx + v * s
    fun y(v: Float) = dy + v * s
    addRoundRect(RoundRect(Rect(x(9f), y(24f), x(39f), y(36f)), CornerRadius(6f * s)))
    addOval(Rect(Offset(x(18f), y(24f)), 8f * s))
    addOval(Rect(Offset(x(29f), y(20f)), 10f * s))
}

private fun DrawScope.cloud(dx: Float, dy: Float, s: Float, light: Color = CloudLight, shade: Color = CloudShade) {
    translate(0f, 1.6f) { drawPath(cloudPath(dx, dy, s), shade) }
    drawPath(cloudPath(dx, dy, s), light)
}

private fun DrawScope.drops(p: Float, heavy: Boolean, dots: Boolean = false) {
    val columns = if (heavy) listOf(12f, 19f, 26f, 33f) else listOf(16f, 24f, 32f)
    columns.forEachIndexed { i, x ->
        val phase = (p + i * 0.33f) % 1f
        val y = 31f + phase * 14f
        val alpha = 1f - phase * 0.7f
        if (dots) {
            drawCircle(Rain.copy(alpha = alpha), 1.6f, Offset(x - phase * 2f, y))
        } else {
            drawLine(Rain.copy(alpha = alpha), Offset(x - phase * 3f, y), Offset(x - phase * 3f - 2.5f, y + 6f), 2.6f, StrokeCap.Round)
        }
    }
}

private fun DrawScope.flakes(p: Float, sparse: Boolean) {
    val xs = if (sparse) listOf(22f, 34f) else listOf(14f, 24f, 34f, 19f, 29f)
    xs.forEachIndexed { i, x ->
        val phase = (p * 0.6f + i * 0.21f) % 1f
        val y = 32f + phase * 13f
        val sway = sin((phase + i) * 2f * PI.toFloat()) * 1.5f
        val c = Offset(x + sway, y)
        rotate(phase * 120f, c) {
            for (k in 0 until 3) {
                val a = k * PI.toFloat() / 3
                drawLine(Flake.copy(alpha = 1f - phase * 0.6f), Offset(c.x - 2.6f * cos(a), c.y - 2.6f * sin(a)),
                    Offset(c.x + 2.6f * cos(a), c.y + 2.6f * sin(a)), 1.6f, StrokeCap.Round)
            }
        }
    }
}

private fun DrawScope.fogLines(p: Float) {
    val shift = sin(p * 2f * PI.toFloat()) * 2f
    listOf(Triple(10f, 38f, 36f), Triple(14f, 34f, 41f), Triple(10f, 30f, 46f)).forEachIndexed { i, (x1, x2, y) ->
        val o = if (i % 2 == 0) shift else -shift
        drawLine(Color(0xFF9AA7B5), Offset(x1 + o, y), Offset(x2 + o, y), 2.4f, StrokeCap.Round)
    }
}

private fun DrawScope.bolt(alpha: Float) {
    val path = Path().apply {
        moveTo(25f, 30f); lineTo(19f, 40f); lineTo(24f, 40f); lineTo(21f, 47f); lineTo(30f, 36f); lineTo(25f, 36f); lineTo(28f, 30f); close()
    }
    drawPath(path, Bolt.copy(alpha = alpha))
}
