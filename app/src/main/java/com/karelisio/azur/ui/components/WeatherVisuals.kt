package com.karelisio.azur.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import com.karelisio.azur.domain.WeatherCondition
import kotlin.math.roundToInt

/** Icône météo statique (vecteur). */
@Composable
fun WeatherIconImage(condition: WeatherCondition, isDay: Boolean, modifier: Modifier = Modifier) {
    Image(painterResource(condition.icon.res(isDay)), contentDescription = condition.label, modifier = modifier)
}

/** Compteur animé : la valeur défile jusqu'à sa cible. */
@Composable
fun AnimatedNumber(
    value: Double,
    style: TextStyle,
    modifier: Modifier = Modifier,
    suffix: String = "°",
    color: Color = Color.Unspecified,
) {
    val reduce = LocalReduceMotion.current
    val animated by animateFloatAsState(
        value.toFloat(),
        animationSpec = if (reduce) snap() else tween(900),
        label = "counter",
    )
    Text("${animated.roundToInt()}$suffix", style = style, modifier = modifier, color = color)
}
