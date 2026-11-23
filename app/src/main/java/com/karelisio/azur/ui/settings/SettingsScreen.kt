package com.karelisio.azur.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karelisio.azur.BuildConfig
import com.karelisio.azur.data.WidgetBackground
import com.karelisio.azur.domain.PrecipUnit
import com.karelisio.azur.domain.PressureUnit
import com.karelisio.azur.domain.TempUnit
import com.karelisio.azur.domain.WindUnit
import com.karelisio.azur.permissions.AzurPermission
import com.karelisio.azur.permissions.rememberAzurPermission
import com.karelisio.azur.ui.onboarding.UnitChooser
import com.karelisio.azur.ui.update.UpdateViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    updateVm: UpdateViewModel,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit,
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val update by updateVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showTime by remember { mutableStateOf(false) }
    val notifications = rememberAzurPermission(AzurPermission.NOTIFICATIONS)
    fun ensureNotifications() { if (!notifications.granted) notifications.request() }
    LaunchedEffect(update.message) { update.message?.let { snackbar.showSnackbar(it); updateVm.clearMessage() } }
    fun open(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeTopAppBar(
                title = { Text("Réglages") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Retour") } },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Section("Unités", Icons.Rounded.Straighten) {
                    UnitChooser("Température", TempUnit.entries, s.units.temp, { it.label }) { vm.setUnits(s.units.copy(temp = it)) }
                    UnitChooser("Vent", WindUnit.entries, s.units.wind, { it.label }) { vm.setUnits(s.units.copy(wind = it)) }
                    UnitChooser("Précipitations", PrecipUnit.entries, s.units.precip, { it.label }) { vm.setUnits(s.units.copy(precip = it)) }
                    UnitChooser("Pression", PressureUnit.entries, s.units.pressure, { it.label }) { vm.setUnits(s.units.copy(pressure = it)) }
                }
            }
            item {
                Section("Montagne", Icons.Rounded.Landscape) {
                    SwitchRow("Module montagne", "Isotherme 0 °C, limite pluie-neige, vent en altitude", s.mountainEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn); vm.setMountain(it)
                    }
                    if (s.mountainEnabled) {
                        var alt by remember(s.mountainAltitude) { mutableFloatStateOf(s.mountainAltitude.toFloat()) }
                        Text("Altitude de référence : ${alt.roundToInt()} m", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = alt, onValueChange = { alt = it }, valueRange = 500f..4000f, steps = 34,
                            onValueChangeFinished = { vm.setMountainAltitude(((alt / 100).roundToInt()) * 100) },
                        )
                    }
                }
            }
            item {
                Section("Notifications", Icons.Rounded.Notifications) {
                    SwitchRow("Pluie dans l'heure", "Vérification toutes les 15 min pour votre position", s.rainAlerts) { if (it) ensureNotifications(); vm.setRainAlerts(it) }
                    SwitchRow(
                        "Résumé du matin",
                        "Chaque jour à %d h %02d".format(s.morningHour, s.morningMinute),
                        s.morningEnabled,
                    ) { if (it) ensureNotifications(); vm.setMorning(it, s.morningHour, s.morningMinute) }
                    if (s.morningEnabled) TextButton(onClick = { showTime = true }) { Text("Changer l'heure") }
                }
            }
            item {
                Section("Widgets", Icons.Rounded.Widgets) {
                    UnitChooser(
                        "Fond des widgets", WidgetBackground.entries, s.widgetBackground,
                        { if (it == WidgetBackground.DYNAMIC) "Couleurs dynamiques" else "Suit le ciel" },
                    ) { vm.setWidgetBackground(it) }
                }
            }
            item {
                Section("Vigilances Météo-France", Icons.Rounded.Warning) {
                    var key by remember(s.vigilanceApiKey) { mutableStateOf(s.vigilanceApiKey) }
                    var visible by remember { mutableStateOf(false) }
                    Text(
                        "Facultatif : collez votre clé gratuite du portail API Météo-France (API « Données Publiques Vigilance ») " +
                            "pour afficher la vigilance de votre département et être notifié.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = key, onValueChange = { key = it }, singleLine = true,
                        label = { Text("Clé API") },
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { visible = !visible }) {
                                Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Afficher")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row {
                        TextButton(onClick = { if (key.isNotBlank()) ensureNotifications(); vm.setVigilanceKey(key) }, enabled = key != s.vigilanceApiKey) { Text("Enregistrer") }
                        TextButton(onClick = { open("https://portail-api.meteofrance.fr") }) { Text("Obtenir une clé") }
                    }
                }
            }
            item {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column {
                        LinkRow("Autorisations", "Localisation, notifications, batterie, installation", Icons.Rounded.Security, onOpenPermissions)
                        LinkRow(
                            "Rechercher une mise à jour", "Version ${BuildConfig.VERSION_NAME}", Icons.Rounded.SystemUpdate,
                            onClick = { updateVm.check(force = true) },
                            trailing = { if (update.checking) LoadingIndicator() },
                        )
                    }
                }
            }
            item {
                Section("Sources et crédits", Icons.Rounded.Info) {
                    SourceRow("Open-Meteo", "Prévisions, qualité de l'air, recherche (CC BY 4.0)") { open("https://open-meteo.com") }
                    SourceRow("Météo-France", "Modèles AROME et ARPEGE, vigilances") { open("https://meteofrance.com") }
                    SourceRow("RainViewer", "Images radar de pluie") { open("https://www.rainviewer.com") }
                    SourceRow("OpenStreetMap", "Fond de carte © contributeurs OSM") { open("https://www.openstreetmap.org/copyright") }
                    SourceRow("Copernicus CAMS", "Qualité de l'air et pollens") { open("https://atmosphere.copernicus.eu") }
                }
            }
        }
    }

    if (showTime) {
        val state = rememberTimePickerState(s.morningHour, s.morningMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = { TextButton(onClick = { vm.setMorning(true, state.hour, state.minute); showTime = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Annuler") } },
            text = { TimePicker(state) },
        )
    }
}

@Composable
private fun Section(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Text("  $title", style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier.clickableRow(onClick),
    )
}

@Composable
private fun SourceRow(name: String, detail: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickableRow(onClick).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
