package com.edu.unal.lugarescercanos.location

/** Estado del permiso de ubicación que ve el usuario (`current-location`, D3). */
enum class LocationPermissionState {
    /** Permiso de ubicación precisa concedido. */
    Precise,

    /** Solo la ubicación aproximada fue concedida: la app sigue y lo indica. */
    Approximate,

    /** Denegado; el sistema todavía mostraría el diálogo. */
    Denied,

    /** Denegado de forma permanente; el botón pasa a «Abrir ajustes». */
    PermanentlyDenied,
    ;

    /** True cuando hay alguna ubicación concedida. */
    val isGranted: Boolean get() = this == Precise || this == Approximate
}

/**
 * Traduce el resultado de pedir los permisos a un estado.
 * La denegación permanente es: ya se pidió, no se concedió y `shouldShowRequestPermissionRationale` es falso.
 */
fun locationPermissionStateOf(
    fineGranted: Boolean,
    coarseGranted: Boolean,
    alreadyRequested: Boolean,
    shouldShowRationale: Boolean,
): LocationPermissionState = when {
    fineGranted -> LocationPermissionState.Precise
    coarseGranted -> LocationPermissionState.Approximate
    alreadyRequested && !shouldShowRationale -> LocationPermissionState.PermanentlyDenied
    else -> LocationPermissionState.Denied
}
