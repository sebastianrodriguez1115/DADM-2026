package com.edu.unal.lugarescercanos.domain

/** Orden de los resultados. Enum propio, sin el del SDK de Places. */
enum class RankPreference { DISTANCE }

/**
 * Parámetros de una búsqueda Nearby, sin tipos de Android ni del SDK. El adaptador de datos
 * los traduce a `CircularBounds` y `SearchNearbyRequest`.
 */
data class NearbySearchParams(
    val center: GeoPoint,
    val radiusMeters: Double,
    val includedTypes: List<String>,
    val maxResultCount: Int = MAX_RESULT_COUNT,
    val rankPreference: RankPreference = RankPreference.DISTANCE,
) {
    companion object {
        /** Máximo de resultados que acepta Nearby Search; no se pagina. */
        const val MAX_RESULT_COUNT = 20

        /** Arma los parámetros a partir de la ubicación, el radio en kilómetros y la categoría. */
        fun create(center: GeoPoint, radiusKm: Double, category: PlaceCategory): NearbySearchParams =
            NearbySearchParams(
                center = center,
                radiusMeters = radiusKm * METERS_PER_KM,
                includedTypes = category.placeTypes,
            )

        private const val METERS_PER_KM = 1000.0
    }
}
