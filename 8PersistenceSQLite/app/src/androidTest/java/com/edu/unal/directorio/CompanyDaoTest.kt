package com.edu.unal.directorio

import android.database.sqlite.SQLiteConstraintException
import androidx.room3.useReaderConnection
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

/** DAO tests on an empty in-memory database, without the sample-data callback (task 3.3). */
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
        vararg classifications: Classification,
        email: String = "contact@${normalizeKey(name).filter { it.isLetterOrDigit() }}.example.com",
    ): Long {
        val id = dao.insert(
            CompanyEntity(
                name = name,
                nameKey = normalizeKey(name),
                email = email,
                emailKey = normalizeKey(email),
            ),
        )
        dao.replaceClassifications(id, classifications.map { it.code }.toSet())
        return id
    }

    /** Runs the filter the way a caller must: normalized, escaped text and optional chips. */
    private suspend fun list(text: String = "", vararg chips: Classification): List<String> =
        dao.observeFiltered(
            query = escapeLike(normalizeKey(text)),
            noClassFilter = if (chips.isEmpty()) 1 else 0,
            classifications = chips.map { it.code },
        ).first().map { it.company.name }

    /** Counts rows of `company_classification` directly, to detect orphans. */
    private suspend fun countClassificationRows(): Long =
        db.useReaderConnection { connection ->
            connection.usePrepared("SELECT COUNT(*) FROM company_classification") { statement ->
                statement.step()
                statement.getLong(0)
            }
        }

    // --- Basic CRUD ---

    @Test
    fun insertedCompanyIsReadBackById() = runBlocking {
        val id = add("Acme Software", Consulting, SoftwareFactory)

        val found = dao.observeById(id).first()
        assertNotNull(found)
        assertEquals("Acme Software", found!!.company.name)
        assertEquals("acme software", found.company.nameKey)
        assertEquals(
            setOf("CONSULTING", "SOFTWARE_FACTORY"),
            found.classifications.map { it.classification }.toSet(),
        )
    }

    @Test
    fun observeByIdOfMissingCompanyEmitsNull() = runBlocking {
        assertNull(dao.observeById(999).first())
    }

    @Test
    fun updateKeepsIdentityAndReplacesClassifications() = runBlocking {
        val id = add("Acme Software", Consulting, SoftwareFactory)
        val before = dao.observeById(id).first()!!.company

        val changed = dao.update(before.copy(phone = "+57 555-0100"))
        dao.replaceClassifications(id, setOf(CustomDevelopment.code))

        assertEquals(1, changed)
        val after = dao.observeById(id).first()!!
        assertEquals(id, after.company.id)
        assertEquals("+57 555-0100", after.company.phone)
        assertEquals(listOf("CUSTOM_DEVELOPMENT"), after.classifications.map { it.classification })
        assertEquals(1, list().size)
    }

    @Test
    fun findIdByKeysReturnsTheOwnerOrNull() = runBlocking {
        val id = add("Acme Software", Consulting, email = "Hola@Acme.example.com")

        assertEquals(id, dao.findIdByNameKey(normalizeKey("  ACME software ")))
        assertEquals(id, dao.findIdByEmailKey(normalizeKey("hola@acme.example.com")))
        assertNull(dao.findIdByNameKey("otra"))
        assertNull(dao.findIdByEmailKey("otro@example.com"))
    }

    // --- Cascade ---

    @Test
    fun deleteRemovesCompanyAndItsClassificationsWithoutOrphans() = runBlocking {
        val doomed = add("Acme Software", Consulting, SoftwareFactory)
        val kept = add("Beta Labs", CustomDevelopment)

        assertEquals(1, dao.deleteById(doomed))

        assertNull(dao.observeById(doomed).first())
        assertEquals(listOf("Beta Labs"), list())
        assertEquals(listOf("Beta Labs"), list("", Consulting, SoftwareFactory, CustomDevelopment))
        assertTrue(list("", Consulting).isEmpty())
        // Orphan check against the raw table: only the kept company's single row remains.
        assertEquals(1, dao.observeById(kept).first()!!.classifications.size)
        assertEquals(1L, countClassificationRows())
        assertEquals(0, dao.deleteById(doomed))
    }

    // --- Unique indices ---

    @Test
    fun duplicateNameKeyIsRejectedByTheDatabase() = runBlocking {
        add("Ñandú Ágil", Consulting, email = "uno@example.com")
        try {
            dao.insert(
                CompanyEntity(
                    name = "NANDU AGIL",
                    nameKey = normalizeKey("NANDU AGIL"),
                    email = "dos@example.com",
                    emailKey = "dos@example.com",
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
        add("Acme Software", Consulting, email = "info@example.com")
        try {
            dao.insert(
                CompanyEntity(
                    name = "Beta Labs",
                    nameKey = normalizeKey("Beta Labs"),
                    email = " INFO@example.com",
                    emailKey = normalizeKey(" INFO@example.com"),
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
        add("C", Consulting, SoftwareFactory)

        assertEquals(listOf("A", "C"), list("", Consulting))
    }

    @Test
    fun severalChipsAreAUnionWithoutRepeatedCompanies() = runBlocking {
        add("A", Consulting)
        add("B", CustomDevelopment)
        add("C", Consulting, SoftwareFactory)

        assertEquals(listOf("A", "B", "C"), list("", Consulting, CustomDevelopment))
        assertEquals(listOf("A", "B", "C"), list("", Consulting, CustomDevelopment, SoftwareFactory))
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
        add("B")

        val result = dao.observeFiltered("", 1, emptyList()).first()
        assertEquals(listOf("A", "B"), result.map { it.company.name })
        assertFalse(result.isEmpty())
    }

    @Test
    fun filteredFlowReemitsWhenDataChanges() = runBlocking {
        val flow = dao.observeFiltered("", 1, emptyList())
        assertTrue(flow.first().isEmpty())
        add("Acme Software", Consulting)
        assertEquals(listOf("Acme Software"), flow.first().map { it.company.name })
    }
}
