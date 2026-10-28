package com.edu.unal.lugarescercanos.location

import com.edu.unal.lugarescercanos.domain.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinateFormatterTest {
    @Test
    fun formatsEachValueWithFiveDecimals() {
        assertEquals("4.63810", CoordinateFormatter.formatValue(4.6381))
        assertEquals("-74.08400", CoordinateFormatter.formatValue(-74.0840))
    }

    @Test
    fun formatsPointAsLatitudeCommaLongitude() {
        assertEquals("4.63810, -74.08400", CoordinateFormatter.formatPoint(GeoPoint(4.6381, -74.0840)))
    }

    @Test
    fun roundsToFiveDecimals() {
        assertEquals("4.59810", CoordinateFormatter.formatValue(4.598104))
    }
}
