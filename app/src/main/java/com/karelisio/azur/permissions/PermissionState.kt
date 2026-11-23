package com.karelisio.azur.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

class AzurPermissionState(val permission: AzurPermission, val granted: Boolean, val request: () -> Unit)

/** État d'une autorisation, revérifié au retour des réglages système (ON_RESUME). */
@Composable
fun rememberAzurPermission(permission: AzurPermission, onResult: (Boolean) -> Unit = {}): AzurPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(permission.isGranted(context)) }
    var asked by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = permission.isGranted(context)
        if (now != granted) { granted = now; onResult(now) }
    }
    val runtime = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = permission.isGranted(context)
        onResult(granted)
    }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        granted = permission.isGranted(context)
        onResult(granted)
    }
    return AzurPermissionState(permission, granted) {
        val rt = permission.runtimePermission
        if (rt != null && !asked) {
            asked = true
            runtime.launch(rt)
        } else {
            runCatching { settings.launch(permission.settingsIntent(context)) }
        }
    }
}
