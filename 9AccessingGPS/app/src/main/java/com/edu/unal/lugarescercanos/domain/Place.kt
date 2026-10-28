package com.edu.unal.lugarescercanos.domain

/** Lugar con nombre y ubicación, ya filtrado, con su distancia en metros a la ubicación del usuario. */
data class Place(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
)
