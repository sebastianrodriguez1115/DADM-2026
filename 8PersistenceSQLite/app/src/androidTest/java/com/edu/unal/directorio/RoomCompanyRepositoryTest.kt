package com.edu.unal.directorio

import android.database.sqlite.SQLiteConstraintException
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.directorio.data.RoomCompanyRepository
import com.edu.unal.directorio.data.SaveResult
import com.edu.unal.directorio.data.db.CompanyEntity
import com.edu.unal.directorio.data.db.DirectorioDatabase
import com.edu.unal.directorio.data.toSaveResult
import com.edu.unal.directorio.domain.Classification.Consulting
import com.edu.unal.directorio.domain.Classification.CustomDevelopment
import com.edu.unal.directorio.domain.Classification.SoftwareFactory
import com.edu.unal.directorio.domain.CompanyDraft
import com.edu.unal.directorio.domain.CompanyFilter
import com.edu.unal.directorio.domain.normalizeKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Repository tests on an empty in-memory database, without the sample-data callback (task 3.5). */
@RunWith(AndroidJUnit4::class)
class RoomCompanyRepositoryTest {
    private lateinit var db: DirectorioDatabase
    private lateinit var repo: RoomCompanyRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = DirectorioDatabase.createInMemory(context)
        repo = RoomCompanyRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun draft(
        name: String = "Acme Software",
        email: String = "contacto@acme.example.com",
        vararg classifications: com.edu.unal.directorio.domain.Classification = arrayOf(Consulting),
        phone: String = "",
        products: String = "",
    ) = CompanyDraft(
        name = name,
        email = email,
        phone = phone,
        products = products,
        classifications = classifications.toSet(),
    )

    private suspend fun saved(d: CompanyDraft): Long =
        (repo.save(d, null) as SaveResult.Saved).id

    private suspend fun all() = repo.observeCompanies(CompanyFilter()).first()

    private suspend fun count(table: String): Long = db.useReaderConnection { c ->
        c.usePrepared("SELECT COUNT(*) FROM $table") { it.step(); it.getLong(0) }
    }

    // --- Data of a company ---

    @Test
    fun companyWithAllFieldsIsReadBack() = runBlocking {
        val id = saved(
            CompanyDraft(
                name = "Acme Software",
                email = "contacto@acme.example.com",
                url = "https://acme.example.com",
                phone = "+57 601 555 0100",
                products = "ERP",
                classifications = setOf(Consulting, SoftwareFactory),
            ),
        )

        val company = repo.observeCompany(id).first()!!
        assertEquals("Acme Software", company.name)
        assertEquals("https://acme.example.com", company.url)
        assertEquals("+57 601 555 0100", company.phone)
        assertEquals("ERP", company.products)
        assertEquals(setOf(Consulting, SoftwareFactory), company.classifications)
    }

    @Test
    fun onlyRequiredFieldsLeavesTheOthersEmpty() = runBlocking {
        val id = saved(draft())

        val company = repo.observeCompany(id).first()!!
        assertEquals("", company.url)
        assertEquals("", company.phone)
        assertEquals("", company.products)
    }

    @Test
    fun surroundingSpacesAreTrimmedButProductLineBreaksKept() = runBlocking {
        val id = saved(
            draft(
                name = "  Acme Software  ",
                email = " contacto@acme.example.com ",
                products = "\n  ERP\nCRM  \n",
            ),
        )

        val company = repo.observeCompany(id).first()!!
        assertEquals("Acme Software", company.name)
        assertEquals("contacto@acme.example.com", company.email)
        assertEquals("ERP\nCRM", company.products)
    }

    @Test
    fun companyWithoutClassificationIsRejectedAndNothingIsStored() = runBlocking {
        try {
            repo.save(CompanyDraft(name = "Acme", email = "a@acme.example.com"), null)
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
        assertEquals(0L, count("company"))
        assertEquals(0L, count("company_classification"))
    }

    // --- Uniqueness on create ---

    @Test
    fun duplicateNameWithOtherCapitalizationIsNameTaken() = runBlocking {
        saved(draft())

        val result = repo.save(draft(name = "ACME software", email = "otro@acme.example.com"), null)

        assertEquals(SaveResult.NameTaken, result)
        assertEquals(1, all().size)
    }

    @Test
    fun duplicateNameWithSurroundingSpacesIsNameTaken() = runBlocking {
        saved(draft())

        val result =
            repo.save(draft(name = "  Acme Software   ", email = "otro@acme.example.com"), null)

        assertEquals(SaveResult.NameTaken, result)
        assertEquals(1, all().size)
    }

    @Test
    fun duplicateEmailWithOtherCapitalizationIsEmailTaken() = runBlocking {
        saved(draft())

        val result =
            repo.save(draft(name = "Beta Labs", email = " Contacto@ACME.example.com "), null)

        assertEquals(SaveResult.EmailTaken, result)
        assertEquals(1, all().size)
    }

    @Test
    fun duplicateNameAndEmailIsNameAndEmailTaken() = runBlocking {
        saved(draft())

        val result = repo.save(draft(name = "acme software", email = "CONTACTO@acme.example.com"), null)

        assertEquals(SaveResult.NameAndEmailTaken, result)
        assertEquals(1, all().size)
    }

    // --- Editing ---

    @Test
    fun editingOnlyThePhoneKeepingNameAndEmailSucceeds() = runBlocking {
        val id = saved(draft())

        val result = repo.save(draft(phone = "+57 555 0100"), id)

        assertEquals(SaveResult.Saved(id), result)
        assertEquals("+57 555 0100", repo.observeCompany(id).first()!!.phone)
        assertEquals(1, all().size)
    }

    @Test
    fun changingOnlyTheCapitalizationOfTheNameSucceeds() = runBlocking {
        val id = saved(draft(name = "Acme software"))

        val result = repo.save(draft(name = "Acme Software"), id)

        assertEquals(SaveResult.Saved(id), result)
        assertEquals("Acme Software", repo.observeCompany(id).first()!!.name)
    }

    @Test
    fun editingTowardsAnotherCompanysNameIsNameTakenAndChangesNothing() = runBlocking {
        saved(draft())
        val beta = saved(draft(name = "Beta Labs", email = "hola@beta.example.com"))

        val result = repo.save(draft(name = "acme software", email = "hola@beta.example.com"), beta)

        assertEquals(SaveResult.NameTaken, result)
        assertEquals("Beta Labs", repo.observeCompany(beta).first()!!.name)
    }

    @Test
    fun editReplacesClassificationsWithoutCreatingAnotherCompany() = runBlocking {
        val id = saved(draft(classifications = arrayOf(Consulting, SoftwareFactory)))

        repo.save(draft(classifications = arrayOf(CustomDevelopment)), id)

        assertEquals(setOf(CustomDevelopment), repo.observeCompany(id).first()!!.classifications)
        assertEquals(1, all().size)
        assertEquals(1L, count("company_classification"))
    }

    @Test
    fun editingAMissingIdThrowsAndStoresNothing() = runBlocking {
        try {
            repo.save(draft(), 12345L)
            fail("Expected NoSuchElementException")
        } catch (_: NoSuchElementException) {
        }
        assertEquals(0L, count("company"))
        assertEquals(0L, count("company_classification"))
    }

    // --- Violation raised by the database ---

    @Test
    fun uniqueViolationFromTheDatabaseIsTranslatedByColumn() = runBlocking {
        saved(draft())
        val dao = db.companyDao()

        val nameDup = captureViolation {
            dao.insert(entity("ACME software", "otro@acme.example.com"))
        }
        val emailDup = captureViolation {
            dao.insert(entity("Beta Labs", "contacto@acme.example.com"))
        }
        val bothDup = captureViolation {
            dao.insert(entity("Acme Software", "contacto@acme.example.com"))
        }

        assertEquals(SaveResult.NameTaken, toSaveResult(nameDup))
        assertEquals(SaveResult.EmailTaken, toSaveResult(emailDup))
        assertTrue(toSaveResult(bothDup) in setOf(SaveResult.NameTaken, SaveResult.EmailTaken))
        assertNull(toSaveResult(SQLiteConstraintException("CHECK constraint failed: x")))
        assertEquals(1L, count("company"))
    }

    private fun entity(name: String, email: String) = CompanyEntity(
        name = name,
        nameKey = normalizeKey(name),
        email = email,
        emailKey = normalizeKey(email),
    )

    private suspend fun captureViolation(block: suspend () -> Unit): SQLiteConstraintException {
        try {
            block()
        } catch (e: SQLiteConstraintException) {
            return e
        }
        fail("Expected SQLiteConstraintException")
        throw IllegalStateException()
    }

    // --- Atomicity ---

    /** Makes any insert of a SOFTWARE_FACTORY classification row fail, after the company row. */
    private suspend fun installFailingTrigger() = db.useWriterConnection { c ->
        c.usePrepared(
            "CREATE TRIGGER fail_factory BEFORE INSERT ON company_classification " +
                "WHEN NEW.classification = 'SOFTWARE_FACTORY' " +
                "BEGIN SELECT RAISE(ABORT, 'simulated failure'); END",
        ) { it.step() }
    }

    @Test
    fun failureWhileWritingClassificationsRollsBackANewCompany() = runBlocking {
        installFailingTrigger()

        try {
            repo.save(draft(classifications = arrayOf(Consulting, SoftwareFactory)), null)
            fail("Expected an exception")
        } catch (e: Exception) {
            // Not a uniqueness violation, so it must propagate untranslated.
            assertTrue(e.message.orEmpty().contains("simulated failure"))
        }

        assertEquals(0L, count("company"))
        assertEquals(0L, count("company_classification"))
    }

    @Test
    fun failureWhileEditingRollsBackBothTheRowAndTheClassifications() = runBlocking {
        val id = saved(draft(phone = "111", classifications = arrayOf(Consulting)))
        installFailingTrigger()

        try {
            repo.save(draft(phone = "222", classifications = arrayOf(SoftwareFactory)), id)
            fail("Expected an exception")
        } catch (_: Exception) {
        }

        val company = repo.observeCompany(id).first()!!
        assertEquals("111", company.phone)
        assertEquals(setOf(Consulting), company.classifications)
    }

    // --- Delete and filter ---

    @Test
    fun deleteRemovesCompanyAndClassifications() = runBlocking {
        val id = saved(draft(classifications = arrayOf(Consulting, SoftwareFactory)))
        val other = saved(draft(name = "Beta Labs", email = "b@beta.example.com"))

        repo.delete(id)
        repo.delete(98765L) // missing id: no effect

        assertNull(repo.observeCompany(id).first())
        assertNotNull(repo.observeCompany(other).first())
        assertEquals(1, all().size)
        assertEquals(1L, count("company_classification"))
    }

    @Test
    fun observeCompaniesAppliesTheFilterAndOrdersByNormalizedName() = runBlocking {
        saved(draft(name = "Ñandú Ágil", email = "n@nandu.example.com", classifications = arrayOf(Consulting)))
        saved(draft(name = "Acme Software", email = "a@acme.example.com", classifications = arrayOf(SoftwareFactory)))
        saved(draft(name = "Zeta Tecnología", email = "z@zeta.example.com", classifications = arrayOf(Consulting, CustomDevelopment)))

        assertEquals(
            listOf("Acme Software", "Ñandú Ágil", "Zeta Tecnología"),
            all().map { it.name },
        )
        assertEquals(
            listOf("Zeta Tecnología"),
            repo.observeCompanies(CompanyFilter(query = "TECNOLOGIA")).first().map { it.name },
        )
        assertEquals(
            listOf("Ñandú Ágil", "Zeta Tecnología"),
            repo.observeCompanies(CompanyFilter(classifications = setOf(Consulting, CustomDevelopment)))
                .first().map { it.name },
        )
        assertEquals(
            listOf("Ñandú Ágil"),
            repo.observeCompanies(CompanyFilter(query = "nandu", classifications = setOf(Consulting)))
                .first().map { it.name },
        )
        assertTrue(repo.observeCompanies(CompanyFilter(query = "nada")).first().isEmpty())
    }
}
