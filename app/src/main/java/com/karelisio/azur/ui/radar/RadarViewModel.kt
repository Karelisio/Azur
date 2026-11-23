package com.karelisio.azur.ui.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karelisio.azur.data.PlaceRepository
import com.karelisio.azur.data.db.PlaceEntity
import com.karelisio.azur.data.net.RainViewerApi
import com.karelisio.azur.data.net.RainViewerMaps
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RadarState(
    val maps: RainViewerMaps? = null,
    val index: Int = 0,
    val playing: Boolean = false,
    val error: String? = null,
    val center: PlaceEntity? = null,
)

class RadarViewModel(private val api: RainViewerApi, private val places: PlaceRepository) : ViewModel() {
    private val _state = MutableStateFlow(RadarState())
    val state: StateFlow<RadarState> = _state
    private var animation: Job? = null

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(error = null, center = places.primary()) }
        runCatching { api.maps() }
            .onSuccess { m -> _state.update { it.copy(maps = m, index = (m.radar.past.size - 1).coerceAtLeast(0)) }; play() }
            .onFailure { _state.update { it.copy(error = "Radar indisponible (hors ligne ?)") } }
    }

    fun select(i: Int) {
        pause()
        _state.update { it.copy(index = i) }
    }

    fun toggle() = if (_state.value.playing) pause() else play()

    fun play(reduceMotion: Boolean = false) {
        val frames = _state.value.maps?.frames ?: return
        if (frames.size < 2 || reduceMotion) return
        animation?.cancel()
        _state.update { it.copy(playing = true) }
        animation = viewModelScope.launch {
            while (isActive) {
                val last = _state.value.index == frames.lastIndex
                delay(if (last) 1500 else 500)
                _state.update { it.copy(index = (it.index + 1) % frames.size) }
            }
        }
    }

    fun pause() {
        animation?.cancel()
        _state.update { it.copy(playing = false) }
    }
}
