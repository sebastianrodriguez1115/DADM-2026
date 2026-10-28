package com.edu.unal.lugarescercanos.ui.map

import com.edu.unal.lugarescercanos.data.places.PlacesRepository
import com.edu.unal.lugarescercanos.data.places.PlacesSearchResult
import com.edu.unal.lugarescercanos.domain.NearbySearchParams

/**
 * Repositorio en memoria. [responder] decide el resultado de cada llamada y puede suspenderse
 * para simular búsquedas lentas. Guarda los parámetros de cada petición en [requests].
 */
class FakePlacesRepository(
    var responder: suspend (NearbySearchParams) -> PlacesSearchResult = {
        PlacesSearchResult.Success(emptyList())
    },
) : PlacesRepository {
    val requests = mutableListOf<NearbySearchParams>()

    override suspend fun searchNearby(params: NearbySearchParams): PlacesSearchResult {
        requests += params
        return responder(params)
    }
}
