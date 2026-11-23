package com.karelisio.azur.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/** Autorisations demandées dans l'onboarding et listées dans Réglages → Autorisations. */
enum class AzurPermission(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val optional: Boolean,
) {
    LOCATION(
        "Localisation approximative",
        "Pour afficher la météo là où vous êtes. Une position approximative suffit, elle ne quitte pas votre téléphone.",
        Icons.Rounded.MyLocation, optional = false,
    ),
    BACKGROUND_LOCATION(
        "Localisation en arrière-plan",
        "Facultatif : garde widgets et alertes de pluie à jour quand vous vous déplacez. Choisissez « Toujours autoriser ».",
        Icons.Rounded.LocationOn, optional = true,
    ),
    NOTIFICATIONS(
        "Notifications",
        "Pour la pluie dans l'heure, le résumé du matin et les vigilances Météo-France.",
        Icons.Rounded.NotificationsActive, optional = true,
    ),
    BATTERY(
        "Optimisation de la batterie",
        "Évite que le système (MagicOS sur Honor notamment) arrête Azur en arrière-plan : widgets et alertes restent à jour.",
        Icons.Rounded.BatteryChargingFull, optional = true,
    ),
    INSTALL(
        "Installation des mises à jour",
        "Permet à Azur d'installer ses propres mises à jour, publiées sur GitHub.",
        Icons.Rounded.InstallMobile, optional = true,
    ),
    ;

    fun isGranted(context: Context): Boolean = when (this) {
        LOCATION -> granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        BACKGROUND_LOCATION -> granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        BATTERY -> context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
        INSTALL -> context.packageManager.canRequestPackageInstalls()
    }

    /** Permission runtime à demander via ActivityResult, ou null si on passe par un écran système. */
    val runtimePermission: String?
        get() = when (this) {
            LOCATION -> Manifest.permission.ACCESS_COARSE_LOCATION
            BACKGROUND_LOCATION -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
            NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
            else -> null
        }

    /** Écran système de repli (ou principal pour batterie / installation). */
    fun settingsIntent(context: Context): Intent {
        val pkg = "package:${context.packageName}".toUri()
        return when (this) {
            BATTERY -> {
                @Suppress("BatteryLife")
                val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
                if (direct.resolveActivity(context.packageManager) != null) direct
                else Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            }
            INSTALL -> Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, pkg)
            NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
        }
    }

    companion object {
        private fun granted(context: Context, permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
