package com.edu.unal.lugarescercanos.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import com.edu.unal.lugarescercanos.domain.GeoPoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Lectura puntual con el Fused Location Provider (D2). La pantalla pide el permiso antes de
 * llamar; si el permiso falta, la llamada lanza SecurityException y se devuelve [LocationResult.Unavailable].
 */
class FusedLocationProvider(context: Context) : LocationProvider {
    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    override suspend fun currentLocation(): LocationResult {
        if (!locationManager.isLocationEnabled) return LocationResult.LocationDisabled
        return readOnce()
    }

    @SuppressLint("MissingPermission")
    private suspend fun readOnce(): LocationResult = suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationTokenSource()
        continuation.invokeOnCancellation { cancellation.cancel() }
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                .addOnSuccessListener { location ->
                    val result = if (location == null) {
                        LocationResult.Unavailable
                    } else {
                        LocationResult.Success(
                            point = GeoPoint(location.latitude, location.longitude),
                            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                        )
                    }
                    if (continuation.isActive) continuation.resume(result)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(LocationResult.Unavailable)
                }
        } catch (_: SecurityException) {
            if (continuation.isActive) continuation.resume(LocationResult.Unavailable)
        }
    }
}
