package com.karelisio.azur.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karelisio.azur.data.CurrentLocationUpdater
import com.karelisio.azur.data.SettingsRepository
import com.karelisio.azur.data.WeatherRepository
import com.karelisio.azur.domain.Units
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val settings: SettingsRepository,
    private val locationUpdater: CurrentLocationUpdater,
    private val weather: WeatherRepository,
) : ViewModel() {
    val units = settings.settings.map { it.units }.stateIn(viewModelScope, SharingStarted.Eagerly, Units())

    fun setUnits(units: Units) = viewModelScope.launch { settings.setUnits(units) }

    /** Dès que la localisation est accordée : on crée le lieu « position actuelle » et on charge sa météo. */
    fun onLocationGranted() = viewModelScope.launch {
        locationUpdater.update()?.let { weather.refresh(it) }
    }

    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        settings.setOnboardingDone(true)
        onDone()
    }
}
