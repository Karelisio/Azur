package com.karelisio.azur.ui.sky

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.karelisio.azur.domain.SkyKind

/** État visuel du ciel, partagé par le fond animé. */
data class SkyState(
    val phase: DayPhase = DayPhase.DAY,
    val kind: SkyKind = SkyKind.CLEAR,
    val cloudiness: Float = 0f,
    val precipitation: Float = 0f,
)

/** Fond en dégradé qui suit l'heure et la météo (repli statique du ciel vivant). */
@Composable
fun GradientSky(state: SkyState, modifier: Modifier = Modifier) {
    val colors = skyColors(state.phase, state.kind)
    val top by animateColorAsState(colors.top, tween(1200), label = "skyTop")
    val bottom by animateColorAsState(colors.bottom, tween(1200), label = "skyBottom")
    Box(modifier.background(Brush.verticalGradient(listOf(top, bottom))))
}
