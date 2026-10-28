package com.edu.unal.lugarescercanos.domain

/** Causa de un fallo de la búsqueda de lugares. Los mensajes visibles van en `strings.xml` (grupo 6). */
enum class PlacesError {
    /** Sin conexión o tiempo de espera agotado. */
    NoConnection,

    /** Cuota o límite de consultas alcanzado. */
    OverQueryLimit,

    /** Llave inválida o sin permisos para Places API (New). */
    RequestDenied,

    /** Cualquier otro error del servicio. */
    ServiceUnavailable,
}

/**
 * Convierte `ApiException.statusCode` en un [PlacesError]. Los valores enteros se confirmaron
 * con `javap -constants` sobre los jars del SDK 6.0.2 (ver `qa/build-probe.md`).
 */
object PlacesErrorMapper {
    /** CommonStatusCodes.NETWORK_ERROR del SDK de Play services 18.9.0. */
    const val NETWORK_ERROR = 7

    /** CommonStatusCodes.TIMEOUT del SDK de Play services 18.9.0. */
    const val TIMEOUT = 15

    /** PlacesStatusCodes.OVER_QUERY_LIMIT del SDK de Places 6.0.2. */
    const val OVER_QUERY_LIMIT = 9010

    /** PlacesStatusCodes.REQUEST_DENIED del SDK de Places 6.0.2. */
    const val REQUEST_DENIED = 9011

    fun map(statusCode: Int): PlacesError = when (statusCode) {
        NETWORK_ERROR, TIMEOUT -> PlacesError.NoConnection
        OVER_QUERY_LIMIT -> PlacesError.OverQueryLimit
        REQUEST_DENIED -> PlacesError.RequestDenied
        else -> PlacesError.ServiceUnavailable
    }
}
