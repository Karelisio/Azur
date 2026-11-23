package com.karelisio.azur.ui.permissions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.karelisio.azur.ui.components.LocalReduceMotion
import com.karelisio.azur.ui.components.MorphingShape

/** Icône animée d'une autorisation : forme qui tourne et respire, puis se transforme et affiche une coche. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PermissionVisual(icon: ImageVector, granted: Boolean, size: Dp = 168.dp) {
    val reduce = LocalReduceMotion.current
    val infinite = rememberInfiniteTransition(label = "perm")
    val spin by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000)), label = "spin")
    val breathe by infinite.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "breathe")
    val morph by animateFloatAsState(if (granted) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow), label = "morph")
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        MorphingShape(
            from = MaterialShapes.Cookie9Sided,
            to = MaterialShapes.Sunny,
            progress = morph,
            color = lerp(scheme.primaryContainer, scheme.tertiaryContainer, morph),
            modifier = Modifier.fillMaxSize(),
            rotation = if (reduce) 0f else spin,
        )
        Icon(
            icon, null,
            Modifier.size(size * 0.38f).graphicsLayer {
                val s = if (reduce || granted) 1f else breathe
                scaleX = s; scaleY = s
            },
            tint = lerp(scheme.onPrimaryContainer, scheme.onTertiaryContainer, morph),
        )
        AnimatedVisibility(
            visible = granted,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            modifier = Modifier.align(Alignment.BottomEnd).offset((-8).dp, (-8).dp),
        ) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(scheme.tertiary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, "Accordée", tint = scheme.onTertiary, modifier = Modifier.size(32.dp))
            }
        }
    }
}
