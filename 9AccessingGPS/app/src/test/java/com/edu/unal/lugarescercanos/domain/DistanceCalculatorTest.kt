package com.edu.unal.lugarescercanos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Distancias de referencia calculadas con un método distinto de haversine: distancia de cuerda
 * entre vectores unitarios, d = 2R·asin(|u−v|/2), con R = 6 371 008,8 m. Como contraste,
 * Vincenty sobre WGS-84 da 4 511,45 m y 13 888,06 m; la diferencia (≈0,5 %) cae dentro de la
 * tolerancia del 1 %.
 */
class DistanceCalculatorTest {
    private val tolerance = 0.01 // 1 %

    private val plazaDeBolivar = GeoPoint(4.5981, -74.0760)
    private val universidadNacional = GeoPoint(4.6381, -74.0840)
    private val aeropuertoElDorado = GeoPoint(4.7016, -74.1469)

    @Test
    fun plazaDeBolivarToUniversidadNacionalIsAboutReferenceDistance() {
        val expected = 4535.32 // m, referencia esfera
        val actual = DistanceCalculator.haversineMeters(plazaDeBolivar, universidadNacional)
        assertEquals(expected, actual, expected * tolerance)
    }

    @Test
    fun aeropuertoElDoradoToPlazaDeBolivarIsAboutReferenceDistance() {
        val expected = 13935.38 // m, referencia esfera
        val actual = DistanceCalculator.haversineMeters(aeropuertoElDorado, plazaDeBolivar)
        assertEquals(expected, actual, expected * tolerance)
    }

    @Test
    fun samePointIsZero() {
        assertEquals(0.0, DistanceCalculator.haversineMeters(universidadNacional, universidadNacional), 0.0)
    }
}
