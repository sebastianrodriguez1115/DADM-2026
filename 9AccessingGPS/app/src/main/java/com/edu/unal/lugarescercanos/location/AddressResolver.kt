package com.edu.unal.lugarescercanos.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.edu.unal.lugarescercanos.domain.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Dirección de una posición. Devuelve null si no hay dirección (`current-location`, D4). */
interface AddressResolver {
    suspend fun addressFor(point: GeoPoint): String?
}

/**
 * Elige la línea de dirección a mostrar a partir de los resultados del geocodificador.
 * Lista vacía, línea nula o en blanco dan null. Es pura para probarla en la JVM.
 */
fun chooseAddressLine(lines: List<String?>): String? =
    lines.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }

/** Geocodificador del sistema, en su variante asíncrona (API 33+; minSdk es 35). */
class GeocoderAddressResolver(context: Context) : AddressResolver {
    private val appContext = context.applicationContext

    override suspend fun addressFor(point: GeoPoint): String? {
        if (!Geocoder.isPresent()) return null
        val lines = try {
            requestLines(point)
        } catch (_: Exception) {
            return null
        }
        return chooseAddressLine(lines)
    }

    private suspend fun requestLines(point: GeoPoint): List<String?> =
        suspendCancellableCoroutine { continuation ->
            val geocoder = Geocoder(appContext)
            geocoder.getFromLocation(
                point.latitude,
                point.longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val lines = addresses.map { address ->
                            address.takeIf { it.maxAddressLineIndex >= 0 }?.getAddressLine(0)
                        }
                        if (continuation.isActive) continuation.resume(lines)
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resume(emptyList())
                    }
                },
            )
        }
}
