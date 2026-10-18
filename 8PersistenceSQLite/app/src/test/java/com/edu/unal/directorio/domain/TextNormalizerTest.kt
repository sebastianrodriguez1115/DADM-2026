package com.edu.unal.directorio.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    @Test
    fun trimsSurroundingSpaces() {
        assertEquals("acme software", normalizeKey("  Acme Software  "))
    }

    @Test
    fun foldsUppercaseAcuteA() {
        assertEquals(normalizeKey("á"), normalizeKey("Á"))
        assertEquals("a", normalizeKey("Á"))
    }

    @Test
    fun foldsEnye() {
        assertEquals(normalizeKey("ñ"), normalizeKey("Ñ"))
        assertEquals("n", normalizeKey("Ñ"))
    }

    @Test
    fun matchesAccentedAgainstPlain() {
        assertEquals(normalizeKey("tecnologia"), normalizeKey("Tecnología"))
    }

    @Test
    fun sameKeyForCaseAndAccentVariants() {
        val keys = listOf("Ñandú Ágil", "ñandú ágil", "NANDU AGIL").map(::normalizeKey)
        assertEquals(1, keys.toSet().size)
    }

    @Test
    fun emptyAndBlankGiveEmptyKey() {
        assertEquals("", normalizeKey(""))
        assertEquals("", normalizeKey("   "))
    }

    @Test
    fun escapesLikeSpecialCharactersWithoutLosingAny() {
        assertEquals("100\\%\\_a\\\\b", escapeLike("100%_a\\b"))
    }

    @Test
    fun escapeLeavesPlainTextUntouched() {
        assertEquals("acme", escapeLike("acme"))
        assertEquals("", escapeLike(""))
    }
}
