package com.edu.unal.lugarescercanos.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Distancia de gran círculo entre dos puntos con la fórmula de haversine, en metros. */
object DistanceCalculator {
    /** Radio terrestre medio, en metros. */
    const val EARTH_RADIUS_METERS = 6_371_008.8

    fun haversineMeters(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = from.latitude * RADIANS_PER_DEGREE
        val lat2 = to.latitude * RADIANS_PER_DEGREE
        val dLat = lat2 - lat1
        val dLng = (to.longitude - from.longitude) * RADIANS_PER_DEGREE
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
        // min evita que errores de redondeo pasen de 1 dentro de asin.
        return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(a)))
    }

    private const val RADIANS_PER_DEGREE = Math.PI / 180.0
}
