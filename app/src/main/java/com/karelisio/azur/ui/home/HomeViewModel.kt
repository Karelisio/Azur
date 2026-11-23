package com.karelisio.azur.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karelisio.azur.data.AppSettings
import com.karelisio.azur.data.CurrentLocationUpdater
import com.karelisio.azur.data.PlaceRepository
import com.karelisio.azur.data.PlaceWeather
import com.karelisio.azur.data.SettingsRepository
import com.karelisio.azur.data.WeatherRepository
import com.karelisio.azur.data.db.PlaceEntity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val locationUpdater: CurrentLocationUpdater,
    private val placeRepo: PlaceRepository,
    private val weatherRepo: WeatherRepository,
    settingsRepo: SettingsRepository,
) : ViewModel() {
    /** null tant que la base n'a pas répondu. */
    val places: StateFlow<List<PlaceEntity>?> = placeRepo.places.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val weather: StateFlow<Map<Long, PlaceWeather>> =
        weatherRepo.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val settings: StateFlow<AppSettings> = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _refreshing = MutableStateFlow<Set<Long>>(emptySet())
    val refreshing: StateFlow<Set<Long>> = _refreshing

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun refresh(place: PlaceEntity, force: Boolean = false) {
        if (place.id in _refreshing.value) return
        val cached = weather.value[place.id]
        val s = settings.value
        val altitudeMissing = s.mountainEnabled && cached?.altitudeMeters != s.mountainAltitude
        if (!force && cached != null && !weatherRepo.isStale(cached.fetchedAt) && !altitudeMissing) return
        viewModelScope.launch {
            _refreshing.update { it + place.id }
            weatherRepo.refresh(place).onFailure {
                if (force || cached == null) _messages.send("Impossible d'actualiser ${place.name} : hors ligne ?")
            }
            _refreshing.update { it - place.id }
        }
    }

    private var locating = false

    /** Met à jour la position actuelle (si autorisée) puis sa météo. */
    fun updateLocation(force: Boolean = false) {
        if (locating) return
        locating = true
        viewModelScope.launch {
            runCatching { locationUpdater.update() }.getOrNull()?.let { refresh(it, force) }
            locating = false
        }
    }

    fun refreshAllStale() {
        places.value?.forEach { refresh(it) }
    }
}
