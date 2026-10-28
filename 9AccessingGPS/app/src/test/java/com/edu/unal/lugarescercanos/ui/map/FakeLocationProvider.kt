package com.edu.unal.lugarescercanos.ui.map

import com.edu.unal.lugarescercanos.location.LocationProvider
import com.edu.unal.lugarescercanos.location.LocationResult

/** Proveedor en memoria: devuelve siempre [result] y cuenta las lecturas. */
class FakeLocationProvider(var result: LocationResult) : LocationProvider {
    var readCount = 0
        private set

    override suspend fun currentLocation(): LocationResult {
        readCount++
        return result
    }
}
