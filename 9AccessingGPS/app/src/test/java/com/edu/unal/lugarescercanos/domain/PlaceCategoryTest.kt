package com.edu.unal.lugarescercanos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceCategoryTest {
    @Test
    fun hasFiveCategoriesInDisplayOrder() {
        assertEquals(
            listOf(
                PlaceCategory.HospitalsAndClinics,
                PlaceCategory.Pharmacies,
                PlaceCategory.Restaurants,
                PlaceCategory.TouristSites,
                PlaceCategory.Parks,
            ),
            PlaceCategory.entries,
        )
    }

    @Test
    fun defaultIsHospitalsAndClinics() {
        assertEquals(PlaceCategory.HospitalsAndClinics, PlaceCategory.DEFAULT)
    }

    @Test
    fun hospitalsAndClinicsUsesHospitalAndMedicalClinic() {
        assertEquals(listOf("hospital", "medical_clinic"), PlaceCategory.HospitalsAndClinics.placeTypes)
    }

    @Test
    fun pharmaciesUsesPharmacy() {
        assertEquals(listOf("pharmacy"), PlaceCategory.Pharmacies.placeTypes)
    }

    @Test
    fun restaurantsUsesRestaurant() {
        assertEquals(listOf("restaurant"), PlaceCategory.Restaurants.placeTypes)
    }

    @Test
    fun touristSitesUsesTouristAttractionMuseumAndArtGallery() {
        assertEquals(
            listOf("tourist_attraction", "museum", "art_gallery"),
            PlaceCategory.TouristSites.placeTypes,
        )
    }

    @Test
    fun parksUsesPark() {
        assertEquals(listOf("park"), PlaceCategory.Parks.placeTypes)
    }
}
