package com.karelisio.azur.ui.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karelisio.azur.data.AppSettings
import com.karelisio.azur.data.PlaceWeather
import com.karelisio.azur.data.db.PlaceEntity
import com.karelisio.azur.ui.components.WeatherIconImage
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    vm: PlacesViewModel,
    weather: Map<Long, PlaceWeather>,
    settings: AppSettings,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    val places by vm.places.collectAsStateWithLifecycle(emptyList())
    val current = places.firstOrNull { it.isCurrent }
    var favorites by remember { mutableStateOf(places.filterNot { it.isCurrent }) }
    LaunchedEffect(places) { favorites = places.filterNot { it.isCurrent } }
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIdx = favorites.indexOfFirst { it.id == from.key }
        val toIdx = favorites.indexOfFirst { it.id == to.key }
        if (fromIdx >= 0 && toIdx >= 0) {
            favorites = favorites.toMutableList().apply { add(toIdx, removeAt(fromIdx)) }
            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Lieux") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Retour") } },
                scrollBehavior = scroll,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Ajouter une ville") })
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (current != null) {
                item(key = "current") {
                    PlaceRow(current, weather[current.id], settings, onClick = { onSelect(current.id) }) {
                        Icon(Icons.Rounded.MyLocation, "Position actuelle", tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp))
                    }
                }
            }
            if (favorites.isEmpty() && current == null) {
                item { Text("Aucun lieu. Ajoutez une ville avec le bouton ci-dessous.", Modifier.padding(16.dp)) }
            }
            items(favorites, key = { it.id }) { place ->
                ReorderableItem(reorderState, key = place.id) { dragging ->
                    PlaceRow(
                        place, weather[place.id], settings,
                        modifier = if (dragging) Modifier.shadow(8.dp, MaterialTheme.shapes.large) else Modifier,
                        onClick = { onSelect(place.id) },
                    ) {
                        IconButton(onClick = { vm.remove(place.id) }) { Icon(Icons.Rounded.DeleteOutline, "Supprimer") }
                        IconButton(
                            onClick = {},
                            modifier = Modifier.draggableHandle(
                                onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
                                onDragStopped = {
                                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    vm.reorder(favorites.map { it.id })
                                },
                            ),
                        ) { Icon(Icons.Rounded.DragHandle, "Réordonner") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(
    place: PlaceEntity,
    data: PlaceWeather?,
    settings: AppSettings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(place.region, place.country).joinToString(" · ").ifBlank { if (place.isCurrent) "Position actuelle" else "" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            data?.forecast?.current?.let { c ->
                WeatherIconImage(c.condition, c.isDay, Modifier.size(32.dp))
                Spacer(Modifier.width(6.dp))
                Text(settings.units.temp(c.temperature), style = MaterialTheme.typography.headlineSmall)
            }
            trailing()
        }
    }
}
