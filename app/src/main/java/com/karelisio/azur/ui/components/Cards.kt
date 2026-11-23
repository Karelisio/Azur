package com.karelisio.azur.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Carte translucide posée sur le ciel. */
@Composable
fun AzurCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
    val contentColor = MaterialTheme.colorScheme.onSurface
    val inner: @Composable () -> Unit = {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) CardTitle(title, icon)
            content()
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = color, contentColor = contentColor, content = inner)
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color, contentColor = contentColor, content = inner)
    }
}

@Composable
fun CardTitle(title: String, icon: ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        }
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Apparition en cascade : chaque élément glisse et s'affiche avec un léger décalage. */
@Composable
fun Modifier.cascade(index: Int): Modifier {
    if (LocalReduceMotion.current) return this
    val alpha = remember { Animatable(0f) }
    val offset = remember { Animatable(40f) }
    LaunchedEffect(Unit) {
        delay(60L * index.coerceAtMost(8))
        launch { alpha.animateTo(1f, tween(380)) }
        offset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
    }
    return graphicsLayer {
        this.alpha = alpha.value
        translationY = offset.value * density
    }
}
