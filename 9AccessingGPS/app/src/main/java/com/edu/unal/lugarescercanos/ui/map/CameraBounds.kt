package com.edu.unal.lugarescercanos.ui.map

import com.edu.unal.lugarescercanos.domain.GeoPoint
import kotlin.math.cos

/** Límites de la cámara en grados: sur, oeste, norte y este (D1, `places-map` «Encuadre»). */
data class CameraBounds(val south: Double, val west: Double, val north: Double, val east: Double)

private const val EARTH_RADIUS_M = 6_371_000.0

/** Coseno mínimo de la latitud, para no dividir entre cero cerca de los polos. */
private const val MIN_COS_LAT = 1e-6

/**
 * Límites que contienen el círculo de [radiusMeters] alrededor de [center]: centro ± radio,
 * convertido a grados de latitud y, según la latitud, a grados de longitud.
 */
fun cameraBoundsAround(center: GeoPoint, radiusMeters: Double): CameraBounds {
    val dLat = Math.toDegrees(radiusMeters / EARTH_RADIUS_M)
    val cosLat = cos(Math.toRadians(center.latitude)).coerceAtLeast(MIN_COS_LAT)
    val dLon = Math.toDegrees(radiusMeters / (EARTH_RADIUS_M * cosLat))
    return CameraBounds(
        south = (center.latitude - dLat).coerceIn(-90.0, 90.0),
        west = (center.longitude - dLon).coerceIn(-180.0, 180.0),
        north = (center.latitude + dLat).coerceIn(-90.0, 90.0),
        east = (center.longitude + dLon).coerceIn(-180.0, 180.0),
    )
}
