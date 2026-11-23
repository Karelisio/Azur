package com.karelisio.azur.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.karelisio.azur.data.db.PlaceEntity
import com.karelisio.azur.data.net.departmentFromPostcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** Position via LocationManager (précision approximative suffisante), sans Play Services. */
class LocationProvider(private val context: Context) {
    private val lm = context.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun lastKnown(): Location? {
        if (!hasPermission()) return null
        return lm.allProviders.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    suspend fun current(timeoutMs: Long = 10_000): Location? {
        if (!hasPermission()) return null
        val provider = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && lm.hasProvider(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return lastKnown()
        }
        val fresh = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                LocationManagerCompat.getCurrentLocation(lm, provider, signal, ContextCompat.getMainExecutor(context)) { loc ->
                    if (cont.isActive) cont.resume(loc)
                }
            }
        }
        return fresh ?: lastKnown()
    }

    data class PlaceName(val name: String?, val region: String?, val department: String?)

    /** Géocodage inverse local (Geocoder Android) : nom de commune et département. */
    suspend fun reverseGeocode(lat: Double, lon: Double): PlaceName? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.FRANCE)
        val address: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(8_000) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) { if (cont.isActive) cont.resume(addresses.firstOrNull()) }
                        override fun onError(errorMessage: String?) { if (cont.isActive) cont.resume(null) }
                    })
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(lat, lon, 1)?.firstOrNull() }.getOrNull()
            }
        }
        address ?: return null
        val dept = if (address.countryCode == "FR") address.postalCode?.let { departmentFromPostcode(it) } else null
        return PlaceName(
            name = address.locality ?: address.subAdminArea ?: address.adminArea,
            region = listOfNotNull(address.subAdminArea, address.adminArea).distinct().joinToString(", ").ifBlank { null },
            department = dept,
        )
    }
}

/** Met à jour la ligne « position actuelle » si l'utilisateur a bougé (> 1 km) ou si elle n'existe pas. */
class CurrentLocationUpdater(
    private val location: LocationProvider,
    private val places: PlaceRepository,
) {
    suspend fun update(useFreshFix: Boolean = true): PlaceEntity? {
        if (!location.hasPermission()) return null
        val loc = (if (useFreshFix) location.current() else location.lastKnown()) ?: return places.current()
        val existing = places.current()
        if (existing != null) {
            val d = FloatArray(1)
            Location.distanceBetween(existing.latitude, existing.longitude, loc.latitude, loc.longitude, d)
            if (d[0] < 1000 && existing.name.isNotBlank()) return existing
        }
        val name = runCatching { location.reverseGeocode(loc.latitude, loc.longitude) }.getOrNull()
        return places.updateCurrent(loc.latitude, loc.longitude, name?.name, name?.region, name?.department)
    }
}
