package com.karelisio.azur.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.karelisio.azur.domain.SkyKind
import com.karelisio.azur.domain.WeatherCondition

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun shapeFor(c: WeatherCondition): RoundedPolygon = when (c.sky) {
    SkyKind.CLEAR -> MaterialShapes.Sunny
    SkyKind.CLOUDY -> MaterialShapes.Cookie9Sided
    SkyKind.FOG -> MaterialShapes.Pill
    SkyKind.RAIN -> MaterialShapes.Clover4Leaf
    SkyKind.SNOW -> MaterialShapes.Flower
    SkyKind.STORM -> MaterialShapes.Burst
}

/** Icône animée posée sur une forme Material qui se transforme quand la condition change. */
@Composable
fun ConditionBadge(condition: WeatherCondition, isDay: Boolean, size: Dp = 120.dp) {
    val reduce = LocalReduceMotion.current
    var from by remember { mutableStateOf(shapeFor(condition)) }
    var to by remember { mutableStateOf(shapeFor(condition)) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(condition) {
        val target = shapeFor(condition)
        if (target != to) {
            from = to
            to = target
            progress.snapTo(0f)
            if (reduce) progress.snapTo(1f) else progress.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
        }
    }
    val spin by rememberInfiniteTransition(label = "badge").animateFloat(
        0f, 360f, infiniteRepeatable(tween(30_000, easing = LinearEasing)), label = "spin",
    )
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        MorphingShape(from, to, progress.value, Color.White.copy(alpha = 0.16f), Modifier.fillMaxSize(), rotation = if (reduce) 0f else spin)
        AnimatedWeatherIcon(condition, isDay, Modifier.fillMaxSize().padding(size * 0.12f))
    }
}
