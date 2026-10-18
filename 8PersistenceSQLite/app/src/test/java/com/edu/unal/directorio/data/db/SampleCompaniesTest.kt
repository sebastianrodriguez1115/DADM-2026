package com.edu.unal.directorio.data.db

import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.domain.CompanyValidator
import com.edu.unal.directorio.domain.normalizeKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleCompaniesTest {
    @Test
    fun hasEightCompanies() {
        assertEquals(8, SAMPLE_COMPANIES.size)
    }

    @Test
    fun allPassTheValidator() {
        SAMPLE_COMPANIES.forEach {
            assertTrue("${it.name}: ${CompanyValidator.validate(it)}", CompanyValidator.validate(it).isValid)
        }
    }

    @Test
    fun nameAndEmailKeysAreUnique() {
        assertEquals(8, SAMPLE_COMPANIES.map { normalizeKey(it.name) }.toSet().size)
        assertEquals(8, SAMPLE_COMPANIES.map { normalizeKey(it.email) }.toSet().size)
    }

    @Test
    fun emailsAndUrlsUseTheExampleDomain() {
        SAMPLE_COMPANIES.forEach {
            assertTrue(it.email, it.email.endsWith(".example.com"))
            assertTrue(it.url, it.url.isEmpty() || it.url.endsWith(".example.com"))
        }
    }

    @Test
    fun allThreeClassificationsArePresent() {
        assertEquals(Classification.entries.toSet(), SAMPLE_COMPANIES.flatMap { it.classifications }.toSet())
    }

    @Test
    fun hasOneTwoAndThreeClassificationCompanies() {
        val sizes = SAMPLE_COMPANIES.map { it.classifications.size }.toSet()
        assertTrue(sizes.containsAll(setOf(1, 2, 3)))
    }

    @Test
    fun hasMultilineProductsAndEmptyUrlAndPhone() {
        assertTrue(SAMPLE_COMPANIES.any { "\n" in it.products })
        assertTrue(SAMPLE_COMPANIES.any { it.url.isEmpty() && it.phone.isEmpty() })
    }
}
