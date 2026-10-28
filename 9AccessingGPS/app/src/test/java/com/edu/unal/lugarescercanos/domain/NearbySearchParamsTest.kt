package com.edu.unal.lugarescercanos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NearbySearchParamsTest {
    private val center = GeoPoint(4.6381, -74.0840)

    @Test
    fun oneKilometerBecomesOneThousandMeters() {
        val params = NearbySearchParams.create(center, 1.0, PlaceCategory.Pharmacies)
        assertEquals(1000.0, params.radiusMeters, 0.0)
    }

    @Test
    fun halfKilometerBecomesFiveHundredMeters() {
        val params = NearbySearchParams.create(center, 0.5, PlaceCategory.Pharmacies)
        assertEquals(500.0, params.radiusMeters, 0.0)
    }

    @Test
    fun keepsCenterAndCategoryTypes() {
        val params = NearbySearchParams.create(center, 1.0, PlaceCategory.TouristSites)
        assertEquals(center, params.center)
        assertEquals(listOf("tourist_attraction", "museum", "art_gallery"), params.includedTypes)
    }

    @Test
    fun maxResultCountIsTwenty() {
        val params = NearbySearchParams.create(center, 1.0, PlaceCategory.Parks)
        assertEquals(20, params.maxResultCount)
    }

    @Test
    fun rankPreferenceIsDistance() {
        val params = NearbySearchParams.create(center, 1.0, PlaceCategory.Parks)
        assertEquals(RankPreference.DISTANCE, params.rankPreference)
    }
}
