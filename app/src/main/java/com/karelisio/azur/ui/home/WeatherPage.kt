package com.karelisio.azur.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.karelisio.azur.data.AppSettings
import com.karelisio.azur.data.PlaceWeather
import com.karelisio.azur.data.db.PlaceEntity
import com.karelisio.azur.domain.RainDetector
import com.karelisio.azur.domain.SummaryGenerator
import com.karelisio.azur.ui.components.AzurCard
import com.karelisio.azur.ui.components.WeatherIconImage
import com.karelisio.azur.ui.components.cascade
import com.karelisio.azur.ui.nav.sharedBoundsIfAvailable

/** Contenu d'un lieu dans le pager. Les emplacements optionnels (vigilance, montagne) sont fournis par l'appelant. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeatherPage(
    place: PlaceEntity,
    data: PlaceWeather?,
    settings: AppSettings,
    refreshing: Boolean,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onRefresh: () -> Unit,
    onDayClick: (Int) -> Unit,
    heroIcon: @Composable (PlaceWeather) -> Unit = { WeatherIconImage(it.forecast.current.condition, it.forecast.current.isDay, Modifier.size(96.dp)) },
    banner: @Composable (PlaceWeather) -> Unit = { d -> d.vigilance?.let { VigilanceBanner(it, place.name) } },
    extraCards: @Composable (PlaceWeather) -> Unit = { d ->
        if (settings.mountainEnabled) {
            com.karelisio.azur.ui.mountain.MountainCard(d, settings.units, System.currentTimeMillis() / 1000, settings.mountainAltitude)
        }
    },
) {
    PullToRefreshBox(isRefreshing = refreshing && data != null, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        if (data == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (refreshing) {
                    LoadingIndicator(Modifier.size(72.dp), color = Color.White)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pas encore de données pour ${place.name}", color = Color.White)
                        Button(onClick = onRefresh) { Text("Réessayer") }
                    }
                }
            }
            return@PullToRefreshBox
        }
        val f = data.forecast
        val now = remember(data.fetchedAt) { System.currentTimeMillis() / 1000 }
        val units = settings.units
        val summary = remember(data.fetchedAt, units) { SummaryGenerator.generate(f, now, units) }
        val nowcast = remember(data.fetchedAt) { RainDetector.detect(f.minutely, now) }
        val rainSlots = remember(data.fetchedAt) { f.minutely.filter { it.time + 900 > now }.take(8) }
        val days = f.daily
        val gMin = days.minOfOrNull { it.temperatureMin } ?: 0.0
        val gMax = days.maxOfOrNull { it.temperatureMax } ?: 1.0
        val hoursAhead = f.hourlyFrom(now, 4)
        val pressureTrend = if (hoursAhead.size >= 4) {
            val p0 = hoursAhead.first().pressure
            val p3 = hoursAhead.last().pressure
            if (p0 != null && p3 != null) p3 - p0 else null
        } else null

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding, bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero") { HeroSection(f, units, summary, now) { heroIcon(data) } }
            item(key = "banner") { banner(data) }
            if (rainSlots.isNotEmpty()) item(key = "rain") { Box(Modifier.cascade(0)) { RainHourCard(nowcast, rainSlots, f, units) } }
            item(key = "hourly") { Box(Modifier.cascade(1)) { HourlyCard(f, units, now) } }
            item(key = "daily") {
                Box(Modifier.cascade(2)) {
                    AzurCard {
                        DailyCardTitle(days.size)
                        days.forEachIndexed { i, d ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            DayRow(
                                d, f, units, now, gMin, gMax,
                                currentTemp = if (i == 0) f.current.temperature else null,
                                modifier = Modifier
                                    .sharedBoundsIfAvailable("day-${place.id}-$i")
                                    .let { m -> m },
                                onClick = { onDayClick(i) },
                            )
                        }
                    }
                }
            }
            if (settings.mountainEnabled) item(key = "extra") { Box(Modifier.cascade(3)) { extraCards(data) } }
            item(key = "row1") {
                Row(Modifier.cascade(3), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    UvCard(f.current.uvIndex ?: f.today(now)?.uvIndexMax, Modifier.weight(1f))
                    WindCard(f.current.windSpeed, f.current.windGusts, f.current.windDirection, units, Modifier.weight(1f))
                }
            }
            item(key = "row2") {
                Row(Modifier.cascade(4), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HumidityCard(f.current.humidity, Modifier.weight(1f))
                    DewPointCard(f.current.dewPoint, units, Modifier.weight(1f))
                }
            }
            item(key = "row3") {
                Row(Modifier.cascade(5), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PressureCard(f.current.pressure, pressureTrend, units, Modifier.weight(1f))
                    VisibilityCard(f.current.visibility ?: hoursAhead.firstOrNull()?.visibility, Modifier.weight(1f))
                }
            }
            item(key = "sun") {
                Row(Modifier.cascade(6), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SunArcCard(f, now, Modifier.weight(1.4f))
                    MoonCard(now, f, Modifier.weight(1f))
                }
            }
            data.air?.let { air -> item(key = "air") { Box(Modifier.cascade(7)) { AirQualityCard(air) } } }
            item(key = "footer") { SourcesFooter(data.fetchedAt); Spacer(Modifier.height(8.dp).padding()) }
        }
    }
}
