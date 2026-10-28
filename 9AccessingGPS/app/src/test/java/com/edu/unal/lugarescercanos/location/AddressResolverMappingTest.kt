package com.edu.unal.lugarescercanos.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddressResolverMappingTest {
    @Test
    fun emptyListGivesNoAddress() {
        assertNull(chooseAddressLine(emptyList()))
    }

    @Test
    fun nullLineGivesNoAddress() {
        assertNull(chooseAddressLine(listOf(null)))
    }

    @Test
    fun blankLineGivesNoAddress() {
        assertNull(chooseAddressLine(listOf("   ")))
    }

    @Test
    fun validFirstLineIsReturnedTrimmed() {
        assertEquals(
            "Cra. 30 #45-03, Bogotá",
            chooseAddressLine(listOf("  Cra. 30 #45-03, Bogotá  ", "Otra dirección")),
        )
    }
}
