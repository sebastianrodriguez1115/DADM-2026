package com.edu.unal.lugarescercanos.data.places

import com.edu.unal.lugarescercanos.domain.NearbySearchParams
import com.edu.unal.lugarescercanos.domain.Place
import com.edu.unal.lugarescercanos.domain.PlacesError

/** Resultado de una búsqueda Nearby: lugares ya filtrados u ordenados, o la causa del fallo. */
sealed interface PlacesSearchResult {
    data class Success(val places: List<Place>) : PlacesSearchResult

    data class Failure(val error: PlacesError) : PlacesSearchResult
}

/**
 * Fuente de lugares cercanos (D5). La app usa [PlacesSdkPlacesRepository]; las pruebas JVM usan un fake.
 * Si la corrutina se cancela, la búsqueda se cancela también en el SDK.
 */
interface PlacesRepository {
    suspend fun searchNearby(params: NearbySearchParams): PlacesSearchResult
}
