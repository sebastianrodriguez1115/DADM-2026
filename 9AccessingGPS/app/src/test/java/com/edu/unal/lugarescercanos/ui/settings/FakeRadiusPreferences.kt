package com.edu.unal.lugarescercanos.ui.settings

import com.edu.unal.lugarescercanos.data.RadiusPreferences
import com.edu.unal.lugarescercanos.domain.RadiusRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Repositorio en memoria: [initial] = null simula que nunca se guardó un radio. */
class FakeRadiusPreferences(initial: Double? = null) : RadiusPreferences {
    private val stored = MutableStateFlow(initial)

    /** Cada llamada a [setRadiusKm], en orden. */
    val setCalls = mutableListOf<Double>()

    /** Valor guardado, o null si nunca se guardó. */
    val saved: Double? get() = stored.value

    override val radiusKm: Flow<Double> = stored.map { it ?: RadiusRules.DEFAULT_KM }

    override suspend fun setRadiusKm(km: Double) {
        setCalls += km
        stored.value = km
    }
}
