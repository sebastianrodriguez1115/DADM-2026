package com.edu.unal.lugarescercanos.data

import kotlinx.coroutines.flow.Flow

/**
 * Radio de búsqueda guardado entre aperturas de la app (U4, D8, `specs/search-radius-settings`).
 *
 * [radiusKm] emite el radio ya ajustado al rango (1 km si nunca se guardó). [setRadiusKm] guarda
 * el valor ajustado con `RadiusRules.clamp`.
 */
interface RadiusPreferences {
    val radiusKm: Flow<Double>

    suspend fun setRadiusKm(km: Double)
}
