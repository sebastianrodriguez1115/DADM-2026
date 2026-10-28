package com.edu.unal.lugarescercanos.ui.map

import com.edu.unal.lugarescercanos.domain.GeoPoint
import com.edu.unal.lugarescercanos.location.AddressResolver

/** Resolutor en memoria: devuelve [address] y guarda los puntos consultados. */
class FakeAddressResolver(var address: String?) : AddressResolver {
    val requested = mutableListOf<GeoPoint>()

    override suspend fun addressFor(point: GeoPoint): String? {
        requested += point
        return address
    }
}
