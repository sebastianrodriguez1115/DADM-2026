package com.edu.unal.lugarescercanos.ui.map

import com.edu.unal.lugarescercanos.domain.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraBoundsTest {
    private val bogota = GeoPoint(4.6381, -74.0840)

    @Test
    fun boundsAreSymmetricAroundCenterInDegrees() {
        val b = cameraBoundsAround(bogota, 1000.0)

        assertEquals(bogota.latitude, (b.north + b.south) / 2, 1e-9)
        assertEquals(bogota.longitude, (b.east + b.west) / 2, 1e-9)
    }

    @Test
    fun oneKilometerIsAboutNineThousandthsOfLatitudeDegree() {
        // 1000 m ≈ 0,008993° de latitud (111,19 km por grado).
        val b = cameraBoundsAround(bogota, 1000.0)

        assertEquals(0.008993 * 2, b.north - b.south, 0.008993 * 2 * 0.01)
    }

    @Test
    fun longitudeDegreesGrowWithLatitude() {
        val near = cameraBoundsAround(GeoPoint(0.0, 0.0), 1000.0)
        val far = cameraBoundsAround(GeoPoint(60.0, 0.0), 1000.0)

        // A 60° de latitud un grado de longitud mide la mitad, así que el intervalo es el doble.
        assertEquals((near.east - near.west) * 2, far.east - far.west, (near.east - near.west) * 2 * 0.01)
        assertTrue(far.east - far.west > near.east - near.west)
    }

    @Test
    fun longitudeSpanAtBogotaIsAboutTheSameAsLatitudeSpan() {
        val b = cameraBoundsAround(bogota, 1000.0)

        // cos(4,6°) ≈ 0,9966: el intervalo de longitud es un poco mayor que el de latitud.
        assertEquals(b.north - b.south, (b.east - b.west) * 0.99656, 0.00001)
    }

    @Test
    fun radiusIsProportionalToMeters() {
        val one = cameraBoundsAround(bogota, 1000.0)
        val three = cameraBoundsAround(bogota, 3000.0)

        assertEquals((one.north - one.south) * 3, three.north - three.south, 1e-9)
    }
}
