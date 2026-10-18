package com.edu.unal.directorio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ClassificationTest {
    @Test
    fun idAndCodePairsAreFixed() {
        assertEquals(
            listOf(1 to "CONSULTING", 2 to "CUSTOM_DEVELOPMENT", 3 to "SOFTWARE_FACTORY"),
            Classification.entries.map { it.id to it.code },
        )
        assertEquals(1, Classification.Consulting.id)
        assertEquals(2, Classification.CustomDevelopment.id)
        assertEquals(3, Classification.SoftwareFactory.id)
    }

    @Test
    fun fromIdResolvesEachId() {
        assertEquals(Classification.Consulting, Classification.fromId(1))
        assertEquals(Classification.CustomDevelopment, Classification.fromId(2))
        assertEquals(Classification.SoftwareFactory, Classification.fromId(3))
        Classification.entries.forEach { assertEquals(it, Classification.fromId(it.id)) }
    }

    @Test
    fun fromIdFailsForAnUnknownId() {
        assertThrows(IllegalArgumentException::class.java) { Classification.fromId(0) }
        assertThrows(IllegalArgumentException::class.java) { Classification.fromId(4) }
        assertThrows(IllegalArgumentException::class.java) { Classification.fromId(-1) }
    }
}
