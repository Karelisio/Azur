package com.karelisio.azur.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

/** Forme Material qui se transforme de [from] vers [to] selon [progress] (0..1). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingShape(
    from: RoundedPolygon,
    to: RoundedPolygon,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
) {
    val morph = remember(from, to) { Morph(from, to) }
    val path = remember { Path() }
    Canvas(modifier) {
        path.reset()
        morph.toPath(progress, path)
        rotate(rotation, pivot = center) {
            scale(size.width, size.height, pivot = Offset.Zero) { drawPath(path, color) }
        }
    }
}
