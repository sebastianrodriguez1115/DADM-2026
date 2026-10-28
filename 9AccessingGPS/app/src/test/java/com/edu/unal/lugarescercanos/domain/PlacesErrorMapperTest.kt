package com.edu.unal.lugarescercanos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Los valores de los códigos se escriben literales a propósito: así la prueba comprueba los
 * números reales de los jars del SDK (ver `qa/build-probe.md`), no solo las constantes del mapper.
 */
class PlacesErrorMapperTest {
    @Test
    fun networkErrorIsNoConnection() {
        // CommonStatusCodes.NETWORK_ERROR = 7
        assertEquals(PlacesError.NoConnection, PlacesErrorMapper.map(7))
    }

    @Test
    fun timeoutIsNoConnection() {
        // CommonStatusCodes.TIMEOUT = 15
        assertEquals(PlacesError.NoConnection, PlacesErrorMapper.map(15))
    }

    @Test
    fun overQueryLimitIsOverQueryLimit() {
        // PlacesStatusCodes.OVER_QUERY_LIMIT = 9010
        assertEquals(PlacesError.OverQueryLimit, PlacesErrorMapper.map(9010))
    }

    @Test
    fun requestDeniedIsRequestDenied() {
        // PlacesStatusCodes.REQUEST_DENIED = 9011
        assertEquals(PlacesError.RequestDenied, PlacesErrorMapper.map(9011))
    }

    @Test
    fun otherPlacesCodeIsServiceUnavailable() {
        // PlacesStatusCodes.INVALID_REQUEST = 9012
        assertEquals(PlacesError.ServiceUnavailable, PlacesErrorMapper.map(9012))
    }

    @Test
    fun unknownCodeIsServiceUnavailable() {
        assertEquals(PlacesError.ServiceUnavailable, PlacesErrorMapper.map(424242))
    }
}
