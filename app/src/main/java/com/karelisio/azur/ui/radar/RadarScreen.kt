package com.karelisio.azur.ui.radar

import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karelisio.azur.data.net.RainViewerMaps
import com.karelisio.azur.domain.TimeText
import com.karelisio.azur.ui.components.LocalReduceMotion
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import java.time.ZoneId

private const val OSM_STYLE = """
{"version":8,
 "sources":{"osm":{"type":"raster","tiles":["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],"tileSize":256,"maxzoom":19,
   "attribution":"© OpenStreetMap"}},
 "layers":[{"id":"osm","type":"raster","source":"osm","paint":{"raster-saturation":-0.4}}]}
"""

@Composable
fun RadarScreen(vm: RadarViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val reduce = LocalReduceMotion.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(Bundle()) }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause(); mapView.onStop(); mapView.onDestroy()
        }
    }
    LaunchedEffect(Unit) {
        mapView.getMapAsync { m ->
            map = m
            m.uiSettings.isRotateGesturesEnabled = false
            m.uiSettings.isTiltGesturesEnabled = false
            m.uiSettings.isAttributionEnabled = false
            m.uiSettings.isLogoEnabled = false
            m.setStyle(Style.Builder().fromJson(OSM_STYLE)) { style = it }
        }
    }
    // Centre sur le lieu principal (France par défaut).
    LaunchedEffect(map, state.center) {
        val m = map ?: return@LaunchedEffect
        val c = state.center
        m.cameraPosition = CameraPosition.Builder()
            .target(if (c != null) LatLng(c.latitude, c.longitude) else LatLng(46.6, 2.4))
            .zoom(if (c != null) 7.0 else 5.0).build()
    }
    // Ajout des couches radar et du point du lieu.
    LaunchedEffect(style, state.maps) {
        val s = style ?: return@LaunchedEffect
        val maps = state.maps ?: return@LaunchedEffect
        maps.frames.forEachIndexed { i, frame ->
            if (s.getSource("radar-$i") == null) {
                val tiles = TileSet("2.2.0", maps.tileUrl(frame)).apply { maxZoom = 7f }
                s.addSource(RasterSource("radar-$i", tiles, 256))
                s.addLayer(RasterLayer("radar-$i", "radar-$i").withProperties(
                    PropertyFactory.rasterOpacity(0f), PropertyFactory.rasterFadeDuration(0f)))
            }
        }
        state.center?.let { c ->
            if (s.getSource("place") == null) {
                s.addSource(GeoJsonSource("place", """{"type":"Point","coordinates":[${c.longitude},${c.latitude}]}"""))
                s.addLayer(CircleLayer("place", "place").withProperties(
                    PropertyFactory.circleRadius(7f), PropertyFactory.circleColor("#1E88E5"),
                    PropertyFactory.circleStrokeColor("#FFFFFF"), PropertyFactory.circleStrokeWidth(3f)))
            }
        }
    }
    // Affiche uniquement l'image courante.
    LaunchedEffect(style, state.index, state.maps) {
        val s = style ?: return@LaunchedEffect
        val frames = state.maps?.frames ?: return@LaunchedEffect
        frames.indices.forEach { i ->
            (s.getLayer("radar-$i") as? RasterLayer)?.setProperties(PropertyFactory.rasterOpacity(if (i == state.index) 0.75f else 0f))
        }
    }
    LaunchedEffect(reduce) { if (reduce) vm.pause() }

    Box(Modifier.fillMaxSize()) {
        AndroidView({ mapView }, Modifier.fillMaxSize())
        FilledTonalIconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(12.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Retour")
        }
        Surface(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val maps = state.maps
                when {
                    state.error != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.error!!, Modifier.weight(1f))
                        TextButton(onClick = { vm.load() }) { Text("Réessayer") }
                    }
                    maps == null -> Text("Chargement du radar…")
                    else -> RadarControls(maps, state, vm)
                }
                Legend()
                Text("Radar RainViewer · fond © contributeurs OpenStreetMap", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RadarControls(maps: RainViewerMaps, state: RadarState, vm: RadarViewModel) {
    val frames = maps.frames
    if (frames.isEmpty()) { Text("Aucune image radar"); return }
    val frame = frames[state.index.coerceIn(0, frames.lastIndex)]
    val zone = ZoneId.systemDefault()
    val nowSec = System.currentTimeMillis() / 1000
    val delta = (frame.time - nowSec) / 60
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(onClick = vm::toggle) {
            Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.playing) "Pause" else "Lecture")
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(TimeText.hm(frame.time, zone), style = MaterialTheme.typography.titleLarge)
            Text(
                when {
                    delta > 1 -> "prévision dans $delta min"
                    delta < -1 -> "il y a ${-delta} min"
                    else -> "maintenant"
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Slider(
        value = state.index.toFloat(),
        onValueChange = { vm.select(it.toInt()) },
        valueRange = 0f..(frames.size - 1).coerceAtLeast(1).toFloat(),
        steps = (frames.size - 2).coerceAtLeast(0),
    )
}

@Composable
private fun Legend() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Faible", style = MaterialTheme.typography.labelSmall)
        Canvas(Modifier.weight(1f).height(8.dp).padding(horizontal = 8.dp)) {
            drawRoundRect(
                Brush.horizontalGradient(listOf(Color(0xFF88DDEE), Color(0xFF0099CC), Color(0xFF0077AA), Color(0xFFFFEE00), Color(0xFFFF4400), Color(0xFFC10000))),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
            )
        }
        Text("Forte", style = MaterialTheme.typography.labelSmall)
    }
}
