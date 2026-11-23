package com.karelisio.azur.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.karelisio.azur.BuildConfig
import com.karelisio.azur.ui.update.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(updateVm: UpdateViewModel, onBack: () -> Unit) {
    Scaffold(topBar = {
        LargeTopAppBar(
            title = { Text("Réglages") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Retour") } },
        )
    }) { padding ->
        Column(Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text("Rechercher une mise à jour") },
                supportingContent = { Text("Version ${BuildConfig.VERSION_NAME}") },
                modifier = Modifier,
            )
        }
    }
}
