package com.edu.unal.lugarescercanos.domain

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.floor

/** Reglas del radio de búsqueda en kilómetros (D8, `specs/search-radius-settings`). */
object RadiusRules {
    const val DEFAULT_KM = 1.0
    const val MIN_KM = 0.5
    const val MAX_KM = 10.0
    const val STEP_KM = 0.5

    private val localeCo: Locale = Locale.Builder().setLanguage("es").setRegion("CO").build()

    /**
     * Ajusta [km] al rango de [MIN_KM] a [MAX_KM] y luego al paso [STEP_KM] más cercano (en un
     * empate, al valor mayor). Un valor NaN se toma como [DEFAULT_KM].
     */
    fun clamp(km: Double): Double {
        if (km.isNaN()) return DEFAULT_KM
        val inRange = km.coerceIn(MIN_KM, MAX_KM)
        val steps = floor(inRange / STEP_KM + 0.5)
        return (steps * STEP_KM).coerceIn(MIN_KM, MAX_KM)
    }

    /** Formato es-CO sin ceros decimales sobrantes: «1 km», «1,5 km», «10 km». */
    fun format(km: Double): String {
        val number = NumberFormat.getNumberInstance(localeCo).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 1
        }
        return "${number.format(km)} km"
    }
}
