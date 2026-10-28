package com.edu.unal.lugarescercanos

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.lugarescercanos.data.DataStoreRadiusPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * Persistencia del radio con un archivo DataStore de prueba, aislado y borrado al terminar.
 * "Reiniciar" es cancelar el ámbito del almacén y abrir otro sobre el mismo archivo.
 */
@RunWith(AndroidJUnit4::class)
class RadiusPreferencesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val file = File(context.cacheDir, "radius-test-${UUID.randomUUID()}.preferences_pb")
    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        scopes.clear()
        file.delete()
    }

    /** Abre un almacén sobre el archivo de prueba; su ámbito se cancela en [restart] o al final. */
    private fun openStore(): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    /**
     * Simula cerrar y reabrir: cancela los almacenes abiertos, espera a que terminen (DataStore
     * libera el archivo al terminar su ámbito) y abre uno nuevo sobre el mismo archivo.
     */
    private suspend fun restart(): DataStoreRadiusPreferences {
        val jobs = scopes.mapNotNull { it.coroutineContext[Job] }
        jobs.forEach { it.cancelAndJoin() }
        scopes.clear()
        return DataStoreRadiusPreferences(openStore())
    }

    @Test
    fun valueWrittenByOneInstanceIsReadByAnother() = runBlocking {
        DataStoreRadiusPreferences(openStore()).setRadiusKm(3.0)

        val reader = restart()

        assertEquals(3.0, reader.radiusKm.first(), 0.0)
    }

    @Test
    fun withoutSavedValueTheDefaultIsOneKm() = runBlocking {
        val preferences = DataStoreRadiusPreferences(openStore())

        assertEquals(1.0, preferences.radiusKm.first(), 0.0)
    }

    @Test
    fun valueOutOfRangeIsClampedWhenSaved() = runBlocking {
        DataStoreRadiusPreferences(openStore()).setRadiusKm(25.0)

        val reader = restart()

        assertEquals(10.0, reader.radiusKm.first(), 0.0)
    }
}
