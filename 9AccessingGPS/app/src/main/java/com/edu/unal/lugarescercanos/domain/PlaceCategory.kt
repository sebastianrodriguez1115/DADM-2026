package com.edu.unal.lugarescercanos.domain

/**
 * Categorías de lugares que se pueden buscar, en el orden en que se muestran. Cada una lleva
 * los tipos de lugar de Google (Table A, ver `qa/place-types.md`). Los nombres visibles van en
 * `strings.xml`, no aquí.
 */
enum class PlaceCategory(val placeTypes: List<String>) {
    HospitalsAndClinics(listOf("hospital", "medical_clinic")),
    Pharmacies(listOf("pharmacy")),
    Restaurants(listOf("restaurant")),
    TouristSites(listOf("tourist_attraction", "museum", "art_gallery")),
    Parks(listOf("park"));

    companion object {
        /** Categoría seleccionada al abrir la app. */
        val DEFAULT: PlaceCategory = HospitalsAndClinics
    }
}
