package com.edu.unal.lugarescercanos.location

import com.edu.unal.lugarescercanos.domain.GeoPoint

/** Resultado de una lectura puntual de la ubicación (`current-location`). */
sealed interface LocationResult {
    /** Posición obtenida; [accuracyMeters] es null si el sistema no informa precisión. */
    data class Success(val point: GeoPoint, val accuracyMeters: Float?) : LocationResult

    /** La ubicación del dispositivo está apagada. */
    data object LocationDisabled : LocationResult

    /** El sistema no devolvió posición (nula o con error). */
    data object Unavailable : LocationResult
}

/** Fuente de la ubicación. Las pruebas usan un fake; la app usa [FusedLocationProvider]. */
interface LocationProvider {
    /** Lee la ubicación una sola vez. No hace seguimiento continuo. */
    suspend fun currentLocation(): LocationResult
}
