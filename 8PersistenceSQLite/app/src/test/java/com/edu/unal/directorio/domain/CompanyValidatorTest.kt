package com.edu.unal.directorio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanyValidatorTest {
    private val validDraft = CompanyDraft(
        name = "Acme Software",
        email = "ventas@acme.example.com",
        classifications = setOf(Classification.Consulting),
    )

    private fun validate(block: CompanyDraft.() -> CompanyDraft) =
        CompanyValidator.validate(validDraft.block())

    // Name

    @Test
    fun validDraftHasNoErrors() {
        val result = CompanyValidator.validate(validDraft)
        assertTrue(result.isValid)
        assertEquals(ValidationErrors(), result.errors)
    }

    @Test
    fun emptyNameIsRejected() {
        assertEquals(NameError.Empty, validate { copy(name = "") }.errors.name)
    }

    @Test
    fun blankNameIsRejected() {
        assertEquals(NameError.Empty, validate { copy(name = "   ") }.errors.name)
    }

    @Test
    fun nameOf100CharactersIsValid() {
        assertNull(validate { copy(name = "a".repeat(100)) }.errors.name)
    }

    @Test
    fun nameOf101CharactersIsTooLong() {
        assertEquals(NameError.TooLong, validate { copy(name = "a".repeat(101)) }.errors.name)
    }

    @Test
    fun nameLengthIsMeasuredAfterTrimming() {
        assertNull(validate { copy(name = " " + "a".repeat(100) + " ") }.errors.name)
    }

    // Email

    @Test
    fun validEmailsAreAccepted() {
        listOf("ventas@acme.example.com", "a.b+c@sub.example.co").forEach { email ->
            assertNull(email, validate { copy(email = email) }.errors.email)
        }
    }

    @Test
    fun invalidEmailsAreRejected() {
        listOf("acme", "acme@", "@acme.com", "a b@acme.com", "a@acme", "a@acme.c").forEach { email ->
            assertEquals(email, EmailError.Invalid, validate { copy(email = email) }.errors.email)
        }
    }

    @Test
    fun emptyEmailIsRejected() {
        assertEquals(EmailError.Empty, validate { copy(email = "") }.errors.email)
        assertEquals(EmailError.Empty, validate { copy(email = "  ") }.errors.email)
    }

    @Test
    fun emailLongerThan254IsInvalid() {
        val long = "a".repeat(250) + "@b.co"
        assertEquals(EmailError.Invalid, validate { copy(email = long) }.errors.email)
    }

    @Test
    fun emailOf254CharactersIsValid() {
        val email = "a".repeat(254 - "@b.co".length) + "@b.co"
        assertEquals(254, email.length)
        assertNull(validate { copy(email = email) }.errors.email)
    }

    // URL

    @Test
    fun validUrlsWithAndWithoutSchemeAreAccepted() {
        listOf("https://www.acme.example.com/productos", "acme.example.com", "HTTP://Acme.com:8080")
            .forEach { url -> assertNull(url, validate { copy(url = url) }.errors.url) }
    }

    @Test
    fun invalidUrlsAreRejected() {
        listOf("ftp://acme.com", "acme", "acme .com", "http://", "http://localhost").forEach { url ->
            assertEquals(url, UrlError.Invalid, validate { copy(url = url) }.errors.url)
        }
    }

    @Test
    fun emptyOrBlankUrlIsValidAndStoredEmpty() {
        assertNull(validate { copy(url = "") }.errors.url)
        val result = validate { copy(url = "   ") }
        assertNull(result.errors.url)
        assertEquals("", result.draft.url)
    }

    @Test
    fun urlOf200CharactersIsValidAnd201IsNot() {
        val base = "https://acme.com/"
        val ok = base + "a".repeat(200 - base.length)
        assertEquals(200, ok.length)
        assertNull(validate { copy(url = ok) }.errors.url)
        assertEquals(UrlError.Invalid, validate { copy(url = ok + "a") }.errors.url)
    }

    @Test
    fun urlIsKeptAsTypedWithoutPrefix() {
        assertEquals("acme.example.com", validate { copy(url = " acme.example.com ") }.draft.url)
    }

    // Phone

    @Test
    fun validPhonesAreAccepted() {
        listOf("601 555 0123", "+57 (601) 555-0123", "3105550123").forEach { phone ->
            assertNull(phone, validate { copy(phone = phone) }.errors.phone)
        }
    }

    @Test
    fun invalidPhonesAreRejected() {
        listOf("12345", "555-CALL-NOW", "1234567890123456", "57+6015550123").forEach { phone ->
            assertEquals(phone, PhoneError.Invalid, validate { copy(phone = phone) }.errors.phone)
        }
    }

    @Test
    fun phoneDigitLimits() {
        assertEquals(PhoneError.Invalid, validate { copy(phone = "12345") }.errors.phone)
        assertNull(validate { copy(phone = "1234567") }.errors.phone)
        assertNull(validate { copy(phone = "123456789012345") }.errors.phone)
        assertEquals(PhoneError.Invalid, validate { copy(phone = "1234567890123456") }.errors.phone)
    }

    @Test
    fun plusOnlyAllowedAtStart() {
        assertNull(validate { copy(phone = "+576015550123") }.errors.phone)
        assertEquals(PhoneError.Invalid, validate { copy(phone = "5760+15550123") }.errors.phone)
        assertEquals(PhoneError.Invalid, validate { copy(phone = "++576015550123") }.errors.phone)
    }

    @Test
    fun emptyPhoneIsValid() {
        assertNull(validate { copy(phone = "") }.errors.phone)
        assertNull(validate { copy(phone = "  ") }.errors.phone)
    }

    // Products

    @Test
    fun productsOf1000CharactersAreValid() {
        assertNull(validate { copy(products = "a".repeat(1000)) }.errors.products)
    }

    @Test
    fun productsOf1001CharactersAreTooLong() {
        assertEquals(
            ProductsError.TooLong,
            validate { copy(products = "a".repeat(1001)) }.errors.products,
        )
    }

    @Test
    fun productsKeepInternalLineBreaks() {
        val result = validate { copy(products = "\n Consulting\nDevelopment\nSupport \n") }
        assertNull(result.errors.products)
        assertEquals("Consulting\nDevelopment\nSupport", result.draft.products)
    }

    @Test
    fun emptyProductsAreValid() {
        assertNull(validate { copy(products = "") }.errors.products)
    }

    // Classification

    @Test
    fun noClassificationIsRejected() {
        val result = validate { copy(classifications = emptySet()) }
        assertEquals(ClassificationError.NoneSelected, result.errors.classification)
        assertFalse(result.isValid)
    }

    @Test
    fun anyCombinationOfClassificationsIsValid() {
        val all = Classification.entries.toSet()
        assertNull(validate { copy(classifications = all) }.errors.classification)
        assertNull(
            validate { copy(classifications = setOf(Classification.SoftwareFactory)) }
                .errors.classification,
        )
    }

    // Combined behavior

    @Test
    fun emptyDraftReportsAllRequiredErrorsAtOnce() {
        val result = CompanyValidator.validate(CompanyDraft())
        assertFalse(result.isValid)
        assertEquals(
            ValidationErrors(
                name = NameError.Empty,
                email = EmailError.Empty,
                classification = ClassificationError.NoneSelected,
            ),
            result.errors,
        )
    }

    @Test
    fun reportsErrorsOfEveryFieldAtOnce() {
        val result = CompanyValidator.validate(
            CompanyDraft(
                name = "a".repeat(101),
                email = "acme",
                url = "ftp://acme.com",
                phone = "12345",
                products = "a".repeat(1001),
            ),
        )
        assertEquals(
            ValidationErrors(
                NameError.TooLong,
                EmailError.Invalid,
                UrlError.Invalid,
                PhoneError.Invalid,
                ProductsError.TooLong,
                ClassificationError.NoneSelected,
            ),
            result.errors,
        )
    }

    @Test
    fun trimsAllTextFields() {
        val result = CompanyValidator.validate(
            CompanyDraft(
                name = "  Acme Software  ",
                email = " contacto@acme.example.com ",
                url = " acme.example.com ",
                phone = " 601 555 0123 ",
                products = "  Consulting  ",
                classifications = setOf(Classification.Consulting),
            ),
        )
        assertTrue(result.isValid)
        assertEquals("Acme Software", result.draft.name)
        assertEquals("contacto@acme.example.com", result.draft.email)
        assertEquals("acme.example.com", result.draft.url)
        assertEquals("601 555 0123", result.draft.phone)
        assertEquals("Consulting", result.draft.products)
    }

    @Test
    fun classificationCodesAreStable() {
        assertEquals(
            listOf("CONSULTING", "CUSTOM_DEVELOPMENT", "SOFTWARE_FACTORY"),
            Classification.entries.map { it.code },
        )
        assertEquals(Classification.CustomDevelopment, Classification.fromCode("CUSTOM_DEVELOPMENT"))
        assertNull(Classification.fromCode("UNKNOWN"))
    }
}
