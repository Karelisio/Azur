package com.karelisio.azur.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karelisio.azur.AppContainer
import com.karelisio.azur.data.AppSettings
import com.karelisio.azur.data.WidgetBackground
import com.karelisio.azur.domain.Units
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private fun edit(block: suspend () -> Unit) = viewModelScope.launch {
        block()
        c.onSettingsChanged()
    }

    fun setUnits(u: Units) = edit { c.settings.setUnits(u) }
    fun setMountain(enabled: Boolean) = edit { c.settings.setMountain(enabled) }
    fun setMountainAltitude(m: Int) = edit { c.settings.setMountainAltitude(m) }
    fun setRainAlerts(enabled: Boolean) = edit { c.settings.setRainAlerts(enabled) }
    fun setMorning(enabled: Boolean, hour: Int, minute: Int) = edit { c.settings.setMorning(enabled, hour, minute) }
    fun setWidgetBackground(bg: WidgetBackground) = edit { c.settings.setWidgetBackground(bg) }
    fun setVigilanceKey(key: String) = edit { c.settings.setVigilanceKey(key) }
}
