package com.edu.unal.lugarescercanos.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

class PlaceSearchRulesTest {
    private val center = GeoPoint(4.6381, -74.0840)

    /** Punto al norte del centro, a [meters] metros: la distancia es exacta con haversine. */
    private fun northOf(meters: Double): GeoPoint {
        val degrees = meters / DistanceCalculator.EARTH_RADIUS_METERS * 180.0 / PI
        return GeoPoint(center.latitude + degrees, center.longitude)
    }

    private fun candidate(id: String, name: String?, at: GeoPoint?) =
        RawPlaceCandidate(id, name, at?.latitude, at?.longitude)

    private fun process(vararg candidates: RawPlaceCandidate, radiusMeters: Double = 1000.0) =
        PlaceSearchRules.process(candidates.toList(), center, radiusMeters)

    @Test
    fun discardsCandidatesWithoutName() {
        val result = process(
            candidate("nulo", null, northOf(100.0)),
            candidate("vacio", "", northOf(110.0)),
            candidate("espacios", "   ", northOf(120.0)),
            candidate("valido", "Farmacia Central", northOf(130.0)),
        )
        assertEquals(listOf("valido"), result.map { it.id })
    }

    @Test
    fun discardsCandidatesWithoutCoordinates() {
        val result = process(
            RawPlaceCandidate("sin-lat", "Clínica A", null, -74.0840),
            RawPlaceCandidate("sin-lng", "Clínica B", 4.6381, null),
            RawPlaceCandidate("sin-ambas", "Clínica C", null, null),
            candidate("valido", "Clínica D", northOf(100.0)),
        )
        assertEquals(listOf("valido"), result.map { it.id })
    }

    @Test
    fun discardsCandidatesOutsideRadius() {
        val result = process(
            candidate("lejos", "Lejos", northOf(1400.0)),
            candidate("cerca", "Cerca", northOf(900.0)),
        )
        assertEquals(listOf("cerca"), result.map { it.id })
    }

    @Test
    fun ordersByDistanceAscending() {
        val result = process(
            candidate("900", "A", northOf(900.0)),
            candidate("200", "B", northOf(200.0)),
            candidate("500", "C", northOf(500.0)),
        )
        assertEquals(listOf("200", "500", "900"), result.map { it.id })
    }

    @Test
    fun keepsAtMostTwentyNearestOfTwentyFiveValid() {
        val candidates = (1..25).map { i -> candidate("p$i", "Lugar $i", northOf(i * 10.0)) }
        val result = PlaceSearchRules.process(candidates, center, 1000.0)
        assertEquals(20, result.size)
        assertEquals((1..20).map { "p$it" }, result.map { it.id })
    }

    @Test
    fun computesDistanceInMeters() {
        val result = process(candidate("a", "A", northOf(350.0)))
        assertEquals(350.0, result.single().distanceMeters, 0.01)
    }

    @Test
    fun trimsNameAndKeepsCoordinates() {
        val result = process(candidate("a", "  Parque Nacional  ", northOf(100.0)))
        val place = result.single()
        assertEquals("Parque Nacional", place.name)
        assertEquals(northOf(100.0).latitude, place.latitude, 0.0)
        assertEquals(center.longitude, place.longitude, 0.0)
    }
}
