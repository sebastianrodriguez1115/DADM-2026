package com.edu.unal.lugarescercanos

import android.Manifest
import android.location.Location
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.lugarescercanos.location.FusedLocationProvider
import com.edu.unal.lugarescercanos.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * Lectura real con el Fused Location Provider y ubicación simulada (`current-location`, D2).
 *
 * La prueba es autocontenida: concede los permisos, habilita la ubicación simulada de la app,
 * lanza [MainActivity] (la lectura solo recibe posiciones con la app en primer plano) y envía
 * posiciones simuladas mientras espera la lectura. Al final desactiva el modo simulado.
 */
@RunWith(AndroidJUnit4::class)
class FusedLocationProviderTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val uiAutomation = instrumentation.uiAutomation
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private val feeding = AtomicBoolean(false)
    private var feeder: Thread? = null
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        uiAutomation.grantRuntimePermission(PACKAGE, Manifest.permission.ACCESS_FINE_LOCATION)
        uiAutomation.grantRuntimePermission(PACKAGE, Manifest.permission.ACCESS_COARSE_LOCATION)
        shell("appops set $PACKAGE android:mock_location allow")
        Tasks.await(client.setMockMode(true))
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        feeding.set(false)
        feeder?.join()
        runCatching { Tasks.await(client.setMockMode(false)) }
        scenario?.close()
        shell("appops set $PACKAGE android:mock_location deny")
    }

    @Test
    fun currentLocationReturnsTheSimulatedPosition() = runBlocking {
        startFeedingMockLocations()

        val result = withTimeout(TIMEOUT_MS) { FusedLocationProvider(context).currentLocation() }

        assertTrue("resultado inesperado: $result", result is LocationResult.Success)
        val point = (result as LocationResult.Success).point
        assertTrue("latitud fuera de 0,01: ${point.latitude}", abs(point.latitude - LAT) < 0.01)
        assertTrue("longitud fuera de 0,01: ${point.longitude}", abs(point.longitude - LON) < 0.01)
    }

    /** Envía una posición simulada cada 500 ms, variando el último decimal, hasta que termina la prueba. */
    private fun startFeedingMockLocations() {
        feeding.set(true)
        feeder = Thread {
            var step = 0
            while (feeding.get()) {
                runCatching { Tasks.await(client.setMockLocation(mockLocation(step))) }
                step++
                Thread.sleep(FEED_INTERVAL_MS)
            }
        }.also { it.start() }
    }

    private fun mockLocation(step: Int): Location = Location(PROVIDER).apply {
        latitude = LAT + (step % 10) * 0.000001
        longitude = LON
        accuracy = 5f
        time = System.currentTimeMillis()
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
    }

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }

    private companion object {
        const val PACKAGE = "com.edu.unal.lugarescercanos"
        const val PROVIDER = "fused"
        const val LAT = 4.6381
        const val LON = -74.0840
        const val FEED_INTERVAL_MS = 500L
        const val TIMEOUT_MS = 30_000L
    }
}
