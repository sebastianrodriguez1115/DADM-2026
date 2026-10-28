package com.edu.unal.lugarescercanos.location

import com.edu.unal.lugarescercanos.domain.GeoPoint
import java.util.Locale

/**
 * Formato de coordenadas de la tarjeta «Tu ubicación»: 5 decimales con punto decimal (`Locale.ROOT`),
 * p. ej. «4.63810, -74.08400». La spec no fija separador decimal; se asume punto.
 */
object CoordinateFormatter {
    fun formatValue(value: Double): String = String.format(Locale.ROOT, "%.5f", value)

    fun formatPoint(point: GeoPoint): String =
        "${formatValue(point.latitude)}, ${formatValue(point.longitude)}"
}
