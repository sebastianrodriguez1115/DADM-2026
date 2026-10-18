package com.edu.unal.directorio

import android.database.sqlite.SQLiteConstraintException
import androidx.room3.useWriterConnection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.directorio.data.db.CompanyDao
import com.edu.unal.directorio.data.db.CompanyEntity
import com.edu.unal.directorio.data.db.DirectorioDatabase
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.domain.Classification.Consulting
import com.edu.unal.directorio.domain.Classification.CustomDevelopment
import com.edu.unal.directorio.domain.Classification.SoftwareFactory
import com.edu.unal.directorio.domain.escapeLike
import com.edu.unal.directorio.domain.normalizeKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DAO tests on an in-memory database with the classification catalog and no companies, without
 * the sample data (tasks 3.3 and 9.2).
 */
@RunWith(AndroidJUnit4::class)
class CompanyDaoTest {
    private lateinit var db: DirectorioDatabase
    private lateinit var dao: CompanyDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = DirectorioDatabase.createInMemory(context)
        dao = db.companyDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Inserts a company the way the repository will: keys computed with [normalizeKey]. */
    private suspend fun add(
        name: String,
        classification: Classification = Consulting,
        email: String = "contact@${normalizeKey(name).filter { it.isLetterOrDigit() }}.example.com",
    ): Long = dao.insert(
        CompanyEntity(
            name = name,
            nameKey = normalizeKey(name),
            email = email,
            emailKey = normalizeKey(email),
            classificationId = classification.id,
        ),
    )

    /** Runs the filter the way a caller must: normalized, escaped text and optional chips. */
    private suspend fun list(text: String = "", vararg chips: Classification): List<String> =
        dao.observeFiltered(
            query = escapeLike(normalizeKey(text)),
            noClassFilter = if (chips.isEmpty()) 1 else 0,
            classificationIds = chips.map { it.id },
        ).first().map { it.name }

    // --- Catalog ---

    @Test
    fun newDatabaseHasTheThreeCatalogRowsAndNoCompanies() = runBlocking {
        val rows = dao.getClassifications()

        assertEquals(
            listOf(1 to "CONSULTING", 2 to "CUSTOM_DEVELOPMENT", 3 to "SOFTWARE_FACTORY"),
            rows.map { it.id to it.code },
        )
        assertEquals(Classification.entries.map { it.id to it.code }, rows.map { it.id to it.code })
        assertTrue(list().isEmpty())
    }

    @Test
    fun foreignKeyRejectsAnUnknownClassification() = runBlocking {
        try {
            dao.insert(
                CompanyEntity(
                    name = "Acme Software",
                    nameKey = "acme software",
                    email = "a@example.com",
                    emailKey = "a@example.com",
                    classificationId = 99,
                ),
            )
            fail("Expected a foreign key violation")
        } catch (e: SQLiteConstraintException) {
            assertTrue(e.message.orEmpty(), e.message.orEmpty().contains("FOREIGN KEY", ignoreCase = true))
        }
        assertTrue(list().isEmpty())
    }

    @Test
    fun classificationCodeIsUnique() = runBlocking {
        try {
            db.useWriterConnection { transactor ->
                transactor.usePrepared("INSERT INTO classification (id, code) VALUES (4, 'CONSULTING')") {
                    it.step()
                }
            }
            fail("Expected a unique constraint violation on code")
        } catch (e: SQLiteConstraintException) {
            assertTrue(e.message.orEmpty(), e.message.orEmpty().contains("code"))
        }
        assertEquals(3, dao.getClassifications().size)
    }

    // --- Basic CRUD ---

    @Test
    fun insertedCompanyIsReadBackById() = runBlocking {
        val id = add("Acme Software", SoftwareFactory)

        val found = dao.observeById(id).first()
        assertNotNull(found)
        assertEquals("Acme Software", found!!.name)
        assertEquals("acme software", found.nameKey)
        assertEquals(SoftwareFactory, Classification.fromId(found.classificationId))
    }

    @Test
    fun observeByIdOfMissingCompanyEmitsNull() = runBlocking {
        assertNull(dao.observeById(999).first())
    }

    @Test
    fun updateKeepsIdentityAndChangesTheClassification() = runBlocking {
        val id = add("Acme Software", SoftwareFactory)
        val before = dao.observeById(id).first()!!

        val changed = dao.update(before.copy(phone = "+57 555-0100", classificationId = CustomDevelopment.id))

        assertEquals(1, changed)
        val after = dao.observeById(id).first()!!
        assertEquals(id, after.id)
        assertEquals("+57 555-0100", after.phone)
        assertEquals(CustomDevelopment.id, after.classificationId)
        assertEquals(1, list().size)
    }

    @Test
    fun findIdByKeysReturnsTheOwnerOrNull() = runBlocking {
        val id = add("Acme Software", email = "Hola@Acme.example.com")

        assertEquals(id, dao.findIdByNameKey(normalizeKey("  ACME software ")))
        assertEquals(id, dao.findIdByEmailKey(normalizeKey("hola@acme.example.com")))
        assertNull(dao.findIdByNameKey("otra"))
        assertNull(dao.findIdByEmailKey("otro@example.com"))
    }

    // --- Delete ---

    @Test
    fun deleteRemovesTheCompanyAndLeavesTheCatalogIntact() = runBlocking {
        val doomed = add("Acme Software", Consulting)
        add("Beta Labs", CustomDevelopment)

        assertEquals(1, dao.deleteById(doomed))

        assertNull(dao.observeById(doomed).first())
        assertEquals(listOf("Beta Labs"), list())
        assertEquals(listOf("Beta Labs"), list("", Consulting, SoftwareFactory, CustomDevelopment))
        assertTrue(list("", Consulting).isEmpty())
        assertEquals(3, dao.getClassifications().size)
        assertEquals(0, dao.deleteById(doomed))
    }

    // --- Unique indices ---

    @Test
    fun duplicateNameKeyIsRejectedByTheDatabase() = runBlocking {
        add("Ñandú Ágil", email = "uno@example.com")
        try {
            dao.insert(
                CompanyEntity(
                    name = "NANDU AGIL",
                    nameKey = normalizeKey("NANDU AGIL"),
                    email = "dos@example.com",
                    emailKey = "dos@example.com",
                    classificationId = Consulting.id,
                ),
            )
            fail("Expected a unique constraint violation on name_key")
        } catch (e: SQLiteConstraintException) {
            assertTrue(e.message.orEmpty().contains("name_key"))
        }
        assertEquals(1, list().size)
    }

    @Test
    fun duplicateEmailKeyIsRejectedByTheDatabase() = runBlocking {
        add("Acme Software", email = "info@example.com")
        try {
            dao.insert(
                CompanyEntity(
                    name = "Beta Labs",
                    nameKey = normalizeKey("Beta Labs"),
                    email = " INFO@example.com",
                    emailKey = normalizeKey(" INFO@example.com"),
                    classificationId = Consulting.id,
                ),
            )
            fail("Expected a unique constraint violation on email_key")
        } catch (e: SQLiteConstraintException) {
            assertTrue(e.message.orEmpty().contains("email_key"))
        }
        assertEquals(listOf("Acme Software"), list())
    }

    // --- Filters ---

    @Test
    fun searchInTheMiddleOfTheName() = runBlocking {
        add("Acme Software")
        add("Beta Labs")

        assertEquals(listOf("Acme Software"), list("me sof"))
    }

    @Test
    fun searchIgnoresCaseAndAccents() = runBlocking {
        add("Tecnología Ñandú")

        assertEquals(listOf("Tecnología Ñandú"), list("TECNOLOGIA"))
        assertEquals(listOf("Tecnología Ñandú"), list("ñandu"))
        assertEquals(listOf("Tecnología Ñandú"), list("ÑANDÚ"))
    }

    @Test
    fun accentedAndEnyeNamesMatchTheirPlainForms() = runBlocking {
        add("Ñandú Ágil")

        assertEquals(listOf("Ñandú Ágil"), list("ñandú ágil"))
        assertEquals(listOf("Ñandú Ágil"), list("NANDU AGIL"))
        assertEquals(listOf("Ñandú Ágil"), list("ágil"))
    }

    @Test
    fun percentIsALiteralInTheSearch() = runBlocking {
        add("Acme Software")
        add("Cien%Digital")

        assertEquals(listOf("Cien%Digital"), list("%"))
    }

    @Test
    fun underscoreAndBackslashAreLiteralsInTheSearch() = runBlocking {
        add("Acme Software")
        add("Mi_Empresa")
        add("Ruta\\Norte")

        assertEquals(listOf("Mi_Empresa"), list("_"))
        assertEquals(listOf("Ruta\\Norte"), list("\\"))
        assertEquals(listOf("Mi_Empresa"), list("i_e"))
    }

    @Test
    fun emptyOrBlankSearchDoesNotFilter() = runBlocking {
        add("Beta Labs")
        add("Acme Software")

        assertEquals(listOf("Acme Software", "Beta Labs"), list(""))
        assertEquals(listOf("Acme Software", "Beta Labs"), list("   "))
    }

    @Test
    fun singleChipFilter() = runBlocking {
        add("A", Consulting)
        add("B", CustomDevelopment)
        add("C", SoftwareFactory)

        assertEquals(listOf("A"), list("", Consulting))
        assertEquals(listOf("B"), list("", CustomDevelopment))
        assertEquals(listOf("C"), list("", SoftwareFactory))
    }

    @Test
    fun severalChipsAreTheUnionOfTheirCompanies() = runBlocking {
        add("A", Consulting)
        add("B", CustomDevelopment)
        add("C", SoftwareFactory)

        assertEquals(listOf("A", "C"), list("", Consulting, SoftwareFactory))
        assertEquals(listOf("A", "B"), list("", Consulting, CustomDevelopment))
        assertEquals(listOf("A", "B", "C"), list("", Consulting, CustomDevelopment, SoftwareFactory))
    }

    @Test
    fun noChipsDoesNotFilterByClassification() = runBlocking {
        add("A", Consulting)
        add("B", CustomDevelopment)
        add("C", SoftwareFactory)

        assertEquals(listOf("A", "B", "C"), list(""))
    }

    @Test
    fun searchAndChipCombine() = runBlocking {
        add("Acme Software", Consulting)
        add("Acme Labs", CustomDevelopment)
        add("Beta", Consulting)

        assertEquals(listOf("Acme Software"), list("acme", Consulting))
    }

    @Test
    fun noMatchesReturnsAnEmptyList() = runBlocking {
        add("Acme Software", Consulting)
        add("Beta", Consulting)

        assertTrue(list("zzz").isEmpty())
        assertTrue(list("beta", CustomDevelopment).isEmpty())
    }

    @Test
    fun resultsAreOrderedByNameKeyIgnoringCaseAndAccents() = runBlocking {
        add("zeta")
        add("Ñandú Ágil")
        add("Álamo")
        add("beta")
        add("Acme")

        assertEquals(listOf("Acme", "Álamo", "beta", "Ñandú Ágil", "zeta"), list())
    }

    @Test
    fun emptyChipListWithNoClassFilterReturnsEverything() = runBlocking {
        add("A", Consulting)
        add("B", SoftwareFactory)

        val result = dao.observeFiltered("", 1, emptyList()).first()
        assertEquals(listOf("A", "B"), result.map { it.name })
        assertFalse(result.isEmpty())
    }

    @Test
    fun filteredFlowReemitsWhenDataChanges() = runBlocking {
        val flow = dao.observeFiltered("", 1, emptyList())
        assertTrue(flow.first().isEmpty())
        add("Acme Software", Consulting)
        assertEquals(listOf("Acme Software"), flow.first().map { it.name })
    }
}
