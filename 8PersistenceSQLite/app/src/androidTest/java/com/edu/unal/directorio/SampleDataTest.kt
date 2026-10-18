package com.edu.unal.directorio

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.directorio.data.RoomCompanyRepository
import com.edu.unal.directorio.data.db.DirectorioDatabase
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.domain.Classification.Consulting
import com.edu.unal.directorio.domain.CompanyDraft
import com.edu.unal.directorio.domain.CompanyFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sample data loading on a real database file of its own, never the app's (design D8, D10).
 * "Reopening" closes the database and builds it again with the same file name.
 */
@RunWith(AndroidJUnit4::class)
class SampleDataTest {
    private val fileName = "directorio-sampledata-test.db"
    private lateinit var context: Context
    private var db: DirectorioDatabase? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(fileName)
    }

    @After
    fun tearDown() {
        db?.close()
        context.deleteDatabase(fileName)
    }

    private fun open(): RoomCompanyRepository {
        db?.close()
        val database = DirectorioDatabase.create(context, name = fileName)
        db = database
        return RoomCompanyRepository(database)
    }

    private fun RoomCompanyRepository.all() = runBlocking { observeCompanies(CompanyFilter()).first() }

    private fun catalog() = runBlocking { db!!.companyDao().getClassifications().map { it.id to it.code } }

    private val expectedCatalog =
        listOf(1 to "CONSULTING", 2 to "CUSTOM_DEVELOPMENT", 3 to "SOFTWARE_FACTORY")

    @Test
    fun firstCreation_loadsEightCompaniesAndTheThreeRowCatalog() {
        val companies = open().all()
        assertEquals(8, companies.size)
        assertEquals(expectedCatalog, catalog())
        // Each sample has exactly one classification, and the three are all used.
        assertEquals(Classification.entries.toSet(), companies.map { it.classification }.toSet())
    }

    @Test
    fun afterDeletingAllAndReopening_thereAreNone() {
        var repo = open()
        runBlocking { repo.all().forEach { repo.delete(it.id) } }
        assertEquals(0, repo.all().size)
        repo = open()
        assertEquals(0, repo.all().size)
        assertEquals(expectedCatalog, catalog())
    }

    @Test
    fun existingDatabaseWithOwnCompany_doesNotReceiveSamples() {
        var repo = open()
        runBlocking {
            repo.all().forEach { repo.delete(it.id) }
            repo.save(
                CompanyDraft(
                    name = "Mi Empresa",
                    email = "mia@example.com",
                    classification = Consulting,
                ),
                editingId = null,
            )
        }
        repo = open()
        assertEquals(listOf("Mi Empresa"), repo.all().map { it.name })
        assertEquals(expectedCatalog, catalog())
    }
}
