package com.karelisio.azur.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.karelisio.azur.domain.AirQuality
import com.karelisio.azur.domain.AqiLevel
import com.karelisio.azur.ui.components.AzurCard
import com.karelisio.azur.ui.components.levelColor
import com.karelisio.azur.ui.components.pollenLevelLabel
import com.karelisio.azur.ui.theme.AzurColors
import com.karelisio.azur.ui.theme.AzurText
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AirQualityCard(air: AirQuality) {
    AzurCard(title = "Qualité de l'air et pollens", icon = Icons.Rounded.Grass) {
        val aqi = air.europeanAqi
        val level = air.aqiLevel
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(aqi?.roundToInt()?.toString() ?: "–", style = AzurText.CardValue)
            Column {
                Text(level?.label ?: "Indisponible", style = MaterialTheme.typography.titleMedium,
                    color = level?.let { levelColor(it.ordinal) } ?: MaterialTheme.colorScheme.onSurface)
                Text("Indice européen (0-100+)", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        GradientGauge(((aqi ?: 0.0) / 100).toFloat(),
            AqiLevel.entries.map { levelColor(it.ordinal) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("PM2,5" to air.pm25, "PM10" to air.pm10, "O₃" to air.ozone, "NO₂" to air.nitrogenDioxide).forEach { (n, v) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(v?.roundToInt()?.toString() ?: "–", style = MaterialTheme.typography.titleMedium)
                    Text(n, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        val pollens = air.pollen.filterValues { it != null }
        if (pollens.isNotEmpty()) {
            Text("Pollens (grains/m³)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pollens.forEach { (pollen, value) ->
                    val lvl = pollen.level(value)
                    val color = if (lvl == 0) AzurColors.Good else levelColor(lvl)
                    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.16f)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                            Text("  ${pollen.label} · ${pollenLevelLabel(lvl)}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        } else {
            Text("Pollens : données indisponibles pour ce lieu", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
