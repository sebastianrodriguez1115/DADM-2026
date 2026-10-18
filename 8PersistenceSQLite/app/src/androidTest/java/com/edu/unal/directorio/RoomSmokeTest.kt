package com.edu.unal.directorio

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
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

/** Prueba de humo de la tarea 1.3: base en memoria, inserta, lee y comprueba que el Flow emite. */
@RunWith(AndroidJUnit4::class)
class RoomSmokeTest {
    private lateinit var db: DirectorioDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, DirectorioDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndReadEmitsThroughFlow() = runBlocking {
        val dao = db.companyDao()
        assertEquals(0, dao.count())

        // Se empieza a observar antes de insertar: el Flow debe emitir la fila nueva.
        val observed = async { dao.observeAll().first { it.isNotEmpty() } }
        dao.insert(CompanyEntity(name = "Empresa de prueba"))

        val emitted = withTimeout(5_000) { observed.await() }
        assertEquals(1, dao.count())
        assertEquals(listOf("Empresa de prueba"), emitted.map { it.name })
    }
}
