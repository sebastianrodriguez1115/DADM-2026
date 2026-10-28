package com.edu.unal.lugarescercanos.domain

/** Punto geográfico en grados decimales (WGS-84). Es un tipo propio para no depender de `LatLng` del SDK. */
data class GeoPoint(val latitude: Double, val longitude: Double)
