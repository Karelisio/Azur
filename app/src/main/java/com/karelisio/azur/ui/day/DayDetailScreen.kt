package com.karelisio.azur.ui.day

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karelisio.azur.domain.TimeText
import com.karelisio.azur.domain.windDirectionLabel
import com.karelisio.azur.ui.components.AzurCard
import com.karelisio.azur.ui.components.WeatherIconImage
import com.karelisio.azur.ui.components.cascade
import com.karelisio.azur.ui.components.uvLevel
import com.karelisio.azur.ui.home.HomeViewModel
import com.karelisio.azur.ui.home.HourlyStrip
import com.karelisio.azur.ui.home.skyStateOf
import com.karelisio.azur.ui.nav.sharedBoundsIfAvailable
import com.karelisio.azur.ui.sky.LiveSky
import com.karelisio.azur.ui.sky.SkyState
import com.karelisio.azur.ui.theme.AzurText
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    vm: HomeViewModel,
    placeId: Long,
    dayIndex: Int,
    onBack: () -> Unit,
    sky: @Composable (SkyState, Modifier) -> Unit = { s, m -> LiveSky(s, m) },
) {
    val weather by vm.weather.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val data = weather[placeId]
    val f = data?.forecast
    val day = f?.daily?.getOrNull(dayIndex)
    val units = settings.units
    val now = System.currentTimeMillis() / 1000

    Box(Modifier.fillMaxSize()) {
        sky(skyStateOf(data), Modifier.fillMaxSize())
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(day?.let { TimeText.dayName(it.time, now, f.zone) } ?: "") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Retour") } },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
                )
            },
        ) { padding ->
            if (f == null || day == null) return@Scaffold
            val hours = f.hoursOfDay(day)
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AzurCard(Modifier.sharedBoundsIfAvailable("day-$placeId-$dayIndex")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WeatherIconImage(day.condition, true, Modifier.size(72.dp))
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    Instant.ofEpochSecond(day.time).atZone(f.zone)
                                        .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                                        .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(day.condition.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(units.temp(day.temperatureMax), style = AzurText.Big)
                                Text(units.temp(day.temperatureMin), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (hours.isNotEmpty()) item {
                    AzurCard(Modifier.cascade(1), title = "Heure par heure", icon = Icons.Rounded.Schedule) {
                        HourlyStrip(hours, units, f.zone, now)
                    }
                }
                item {
                    Column(Modifier.cascade(2), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Stat("Précipitations", Icons.Rounded.Umbrella, units.precip(day.precipitationSum ?: 0.0),
                                day.precipitationProbabilityMax?.let { "Probabilité ${it.roundToInt()} %" } ?: "", Modifier.weight(1f))
                            Stat("Vent max", Icons.Rounded.Air, units.wind(day.windSpeedMax),
                                listOfNotNull(day.windGustsMax?.let { "Rafales ${units.wind(it)}" }, windDirectionLabel(day.windDirectionDominant).ifBlank { null }).joinToString(" · "),
                                Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Stat("UV max", Icons.Rounded.WbSunny, day.uvIndexMax?.roundToInt()?.toString() ?: "–",
                                day.uvIndexMax?.let { uvLevel(it).first } ?: "", Modifier.weight(1f))
                            Stat("Soleil", Icons.Rounded.WbTwilight,
                                day.sunrise?.let { TimeText.hm(it, f.zone) } ?: "–",
                                day.sunset?.let { "Coucher ${TimeText.hm(it, f.zone)}" } ?: "", Modifier.weight(1f))
                        }
                        if ((day.snowfallSum ?: 0.0) > 0) {
                            Stat("Neige", Icons.Rounded.AcUnit, "${day.snowfallSum!!.roundToInt()} cm", "Cumul de la journée", Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(title: String, icon: ImageVector, value: String, caption: String, modifier: Modifier) {
    AzurCard(modifier, title = title, icon = icon, shape = MaterialTheme.shapes.large) {
        Text(value, style = AzurText.CardValue)
        if (caption.isNotBlank()) Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
