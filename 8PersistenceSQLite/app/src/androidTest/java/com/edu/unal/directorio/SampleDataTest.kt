package com.edu.unal.directorio

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.directorio.data.RoomCompanyRepository
import com.edu.unal.directorio.data.db.DirectorioDatabase
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

    @Test
    fun firstCreation_loadsEightCompanies() {
        assertEquals(8, open().all().size)
    }

    @Test
    fun afterDeletingAllAndReopening_thereAreNone() {
        var repo = open()
        runBlocking { repo.all().forEach { repo.delete(it.id) } }
        assertEquals(0, repo.all().size)
        repo = open()
        assertEquals(0, repo.all().size)
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
                    classifications = setOf(Consulting),
                ),
                editingId = null,
            )
        }
        repo = open()
        assertEquals(listOf("Mi Empresa"), repo.all().map { it.name })
    }
}
