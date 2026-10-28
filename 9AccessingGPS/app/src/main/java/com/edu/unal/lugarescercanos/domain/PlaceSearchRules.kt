package com.edu.unal.lugarescercanos.domain

/**
 * Candidato crudo que llega de Places, antes de validarlo. El nombre y las coordenadas pueden
 * venir nulos.
 */
data class RawPlaceCandidate(
    val id: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
)

/**
 * Filtra, ordena y limita los candidatos de una búsqueda (D5). Descarta los que no tienen nombre
 * o ubicación, y los que quedan a más del radio. Ordena por distancia de menor a mayor y devuelve
 * como máximo [NearbySearchParams.MAX_RESULT_COUNT] lugares.
 */
object PlaceSearchRules {

    fun process(
        candidates: List<RawPlaceCandidate>,
        center: GeoPoint,
        radiusMeters: Double,
        maxResults: Int = NearbySearchParams.MAX_RESULT_COUNT,
    ): List<Place> =
        candidates
            .mapNotNull { candidate -> toPlace(candidate, center) }
            .filter { it.distanceMeters <= radiusMeters }
            .sortedBy { it.distanceMeters }
            .take(maxResults)

    private fun toPlace(candidate: RawPlaceCandidate, center: GeoPoint): Place? {
        val name = candidate.name?.trim()
        val latitude = candidate.latitude
        val longitude = candidate.longitude
        if (name.isNullOrBlank() || latitude == null || longitude == null) return null
        val distance = DistanceCalculator.haversineMeters(center, GeoPoint(latitude, longitude))
        return Place(
            id = candidate.id,
            name = name,
            latitude = latitude,
            longitude = longitude,
            distanceMeters = distance,
        )
    }
}
