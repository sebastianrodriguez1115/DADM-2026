package com.edu.unal.directorio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.directorio.data.db.CompanyEntity
import com.edu.unal.directorio.data.db.DirectorioDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke test (task 1.3): in-memory database, insert, read and check that the Flow emits. */
@RunWith(AndroidJUnit4::class)
class RoomSmokeTest {
    private lateinit var db: DirectorioDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = DirectorioDatabase.createInMemory(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndReadEmitsThroughFlow() = runBlocking {
        val dao = db.companyDao()
        assertEquals(emptyList<Any>(), dao.observeFiltered("", 1, emptyList()).first())

        // Start observing before inserting: the Flow must emit the new row.
        val observed = async { dao.observeFiltered("", 1, emptyList()).first { it.isNotEmpty() } }
        dao.insert(
            CompanyEntity(
                name = "Empresa de prueba",
                nameKey = "empresa de prueba",
                email = "a@example.com",
                emailKey = "a@example.com",
            ),
        )

        val emitted = withTimeout(5_000) { observed.await() }
        assertEquals(listOf("Empresa de prueba"), emitted.map { it.company.name })
    }
}
