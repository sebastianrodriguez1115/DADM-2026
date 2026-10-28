package com.edu.unal.lugarescercanos.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceFormatterTest {
    @Test
    fun underAKilometerShowsRoundedMeters() {
        assertEquals("a 350 m", formatDistance(350.0))
        assertEquals("a 350 m", formatDistance(349.6))
        assertEquals("a 0 m", formatDistance(0.0))
    }

    @Test
    fun fromAKilometerShowsOneDecimalWithComma() {
        assertEquals("a 1,2 km", formatDistance(1200.0))
        assertEquals("a 1,0 km", formatDistance(1000.0))
    }

    @Test
    fun meterRoundingNeverShowsOneThousandMeters() {
        assertEquals("a 1,0 km", formatDistance(999.6))
    }
}
