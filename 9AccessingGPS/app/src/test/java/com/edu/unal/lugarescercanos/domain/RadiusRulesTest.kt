package com.edu.unal.lugarescercanos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RadiusRulesTest {
    @Test
    fun defaultIsOneKilometer() {
        assertEquals(1.0, RadiusRules.DEFAULT_KM, 0.0)
    }

    @Test
    fun valueAboveRangeIsClampedToMax() {
        assertEquals(10.0, RadiusRules.clamp(25.0), 0.0)
    }

    @Test
    fun valueBelowRangeIsClampedToMin() {
        assertEquals(0.5, RadiusRules.clamp(0.2), 0.0)
    }

    @Test
    fun valueIsRoundedToNearestStep() {
        assertEquals(1.5, RadiusRules.clamp(1.3), 0.0)
        assertEquals(1.0, RadiusRules.clamp(1.2), 0.0)
    }

    @Test
    fun notANumberFallsBackToDefault() {
        assertEquals(RadiusRules.DEFAULT_KM, RadiusRules.clamp(Double.NaN), 0.0)
    }

    @Test
    fun formatsWithoutTrailingZerosInEsCo() {
        assertEquals("1,5 km", RadiusRules.format(1.5))
        assertEquals("1 km", RadiusRules.format(1.0))
        assertEquals("10 km", RadiusRules.format(10.0))
        assertEquals("0,5 km", RadiusRules.format(0.5))
    }
}
