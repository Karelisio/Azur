package com.karelisio.azur.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karelisio.azur.data.PlaceWeather
import com.karelisio.azur.data.db.PlaceEntity
import com.karelisio.azur.ui.sky.GradientSky
import com.karelisio.azur.ui.sky.SkyState
import com.karelisio.azur.ui.sky.dayPhase
import kotlinx.coroutines.flow.distinctUntilChanged

fun skyStateOf(data: PlaceWeather?, now: Long = System.currentTimeMillis() / 1000): SkyState {
    val f = data?.forecast ?: return SkyState()
    val today = f.today(now)
    val condition = f.current.condition
    val precip = when {
        !condition.isWet -> 0f
        f.current.precipitation != null -> ((f.current.precipitation) / 3.0).toFloat().coerceIn(0.25f, 1f)
        else -> 0.5f
    }
    return SkyState(
        phase = dayPhase(now, today?.sunrise, today?.sunset, f.current.isDay),
        kind = condition.sky,
        cloudiness = maxOf(condition.cloudiness, ((f.current.cloudCover ?: 0.0) / 100).toFloat()),
        precipitation = precip,
    )
}

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    selectPlaceId: Long?,
    onPlaceSelected: () -> Unit,
    onOpenPlaces: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRadar: () -> Unit,
    onOpenDay: (Long, Int) -> Unit,
    onUseLocation: (() -> Unit)?,
    sky: @Composable (SkyState, Modifier) -> Unit = { s, m -> GradientSky(s, m) },
    page: @Composable (PlaceEntity, PlaceWeather?, Boolean, androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp) -> Unit,
) {
    val places by vm.places.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshAllStale(); vm.updateLocation() }
    val settings by vm.settings.collectAsStateWithLifecycle()
    LaunchedEffect(places, settings.mountainEnabled, settings.mountainAltitude, settings.vigilanceApiKey) { vm.refreshAllStale() }

    val list = places
    val pager = rememberPagerState { list?.size ?: 0 }
    LaunchedEffect(selectPlaceId, list) {
        if (selectPlaceId != null && list != null) {
            val idx = list.indexOfFirst { it.id == selectPlaceId }
            if (idx >= 0) pager.animateScrollToPage(idx)
            onPlaceSelected()
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().collect {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }

    val currentPlace = list?.getOrNull(pager.currentPage)
    val skyState = skyStateOf(currentPlace?.let { weather[it.id] })
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize()) {
        sky(skyState, Modifier.fillMaxSize())
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyState(onOpenSearch, onUseLocation)
            else -> HorizontalPager(state = pager, key = { list[it].id }, beyondViewportPageCount = 1) { index ->
                val place = list[index]
                page(place, weather[place.id], place.id in vm.refreshing.collectAsStateWithLifecycle().value, top, bottom)
            }
        }
        TopBar(
            places = list.orEmpty(),
            current = pager.currentPage,
            onOpenPlaces = onOpenPlaces,
            onOpenRadar = onOpenRadar,
            onOpenSettings = onOpenSettings,
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = bottom + 16.dp))
    }
}

@Composable
private fun TopBar(
    places: List<PlaceEntity>,
    current: Int,
    onOpenPlaces: () -> Unit,
    onOpenRadar: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val iconColors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenPlaces, colors = iconColors) { Icon(Icons.Rounded.FormatListBulleted, "Lieux") }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            val place = places.getOrNull(current)
            AnimatedContent(place, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "placeName") { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (p?.isCurrent == true) {
                        Icon(Icons.Rounded.MyLocation, null, Modifier.size(16.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        p?.name ?: "Azur",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (places.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 4.dp)) {
                    places.forEachIndexed { i, _ ->
                        val selected = i == current
                        val w by animateDpAsState(if (selected) 16.dp else 6.dp, label = "dot")
                        val c by animateColorAsState(if (selected) Color.White else Color.White.copy(alpha = 0.45f), label = "dotColor")
                        Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(c))
                    }
                }
            }
        }
        IconButton(onClick = onOpenRadar, colors = iconColors) { Icon(Icons.Rounded.Radar, "Radar de pluie") }
        IconButton(onClick = onOpenSettings, colors = iconColors) { Icon(Icons.Rounded.Settings, "Réglages") }
    }
}

@Composable
private fun EmptyState(onSearch: () -> Unit, onUseLocation: (() -> Unit)?) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Bienvenue dans Azur", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text("Ajoutez un lieu pour voir sa météo.", color = Color.White.copy(alpha = 0.85f))
        Spacer(Modifier.height(24.dp))
        if (onUseLocation != null) {
            Button(onClick = onUseLocation, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.MyLocation, null); Spacer(Modifier.width(8.dp)); Text("Utiliser ma position")
            }
            Spacer(Modifier.height(8.dp))
        }
        FilledTonalButton(onClick = onSearch, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Search, null); Spacer(Modifier.width(8.dp)); Text("Rechercher une ville")
        }
    }
}
