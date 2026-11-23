package com.karelisio.azur.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.karelisio.azur.ui.theme.AzurColors

fun relativeUpdated(fetchedAt: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - fetchedAt) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "Mis à jour à l'instant"
        minutes < 60 -> "Mis à jour il y a $minutes min"
        minutes < 24 * 60 -> "Mis à jour il y a ${minutes / 60} h"
        else -> "Mis à jour il y a ${minutes / (24 * 60)} j"
    }
}

/** Couleur d'une température (°C) : bleu froid → vert → jaune → orange → rouge. */
fun temperatureColor(c: Double): Color {
    val stops = listOf(
        -10.0 to Color(0xFF7B8CFF),
        0.0 to Color(0xFF5AB0FF),
        8.0 to Color(0xFF4FD1C5),
        15.0 to Color(0xFF9BD45A),
        22.0 to Color(0xFFF2C12E),
        28.0 to Color(0xFFFF8A3D),
        35.0 to Color(0xFFE5484D),
    )
    if (c <= stops.first().first) return stops.first().second
    if (c >= stops.last().first) return stops.last().second
    val i = stops.indexOfFirst { it.first > c }
    val (t0, c0) = stops[i - 1]
    val (t1, c1) = stops[i]
    return lerp(c0, c1, ((c - t0) / (t1 - t0)).toFloat())
}

fun levelColor(level: Int): Color = when (level) {
    0 -> AzurColors.Good
    1 -> AzurColors.Fair
    2 -> AzurColors.Moderate
    3 -> AzurColors.Poor
    4 -> AzurColors.VeryPoor
    else -> AzurColors.Extreme
}

fun uvLevel(uv: Double): Pair<String, Int> = when {
    uv < 3 -> "Faible" to 0
    uv < 6 -> "Modéré" to 2
    uv < 8 -> "Élevé" to 3
    uv < 11 -> "Très élevé" to 4
    else -> "Extrême" to 5
}

fun visibilityLabel(m: Double): String = when {
    m >= 20_000 -> "Excellente"
    m >= 10_000 -> "Bonne"
    m >= 4_000 -> "Moyenne"
    m >= 1_000 -> "Faible"
    else -> "Brouillard"
}

fun dewPointComfort(c: Double): String = when {
    c < 10 -> "Air sec"
    c < 16 -> "Confortable"
    c < 19 -> "Légèrement lourd"
    c < 22 -> "Lourd"
    else -> "Étouffant"
}

fun pollenLevelLabel(level: Int): String = listOf("Nul", "Faible", "Modéré", "Élevé", "Très élevé")[level.coerceIn(0, 4)]
