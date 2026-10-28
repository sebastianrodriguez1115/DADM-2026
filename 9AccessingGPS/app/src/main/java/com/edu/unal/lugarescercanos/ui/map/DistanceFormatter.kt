package com.edu.unal.lugarescercanos.ui.map

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val localeCo: Locale = Locale.Builder().setLanguage("es").setRegion("CO").build()

/**
 * Distancia de la burbuja y de la etiqueta (`places-map`, P7): «a 350 m» por debajo de 1000 m,
 * con metros redondeados, y «a 1,2 km» desde 1000 m, con un decimal (es-CO).
 */
fun formatDistance(meters: Double): String {
    val rounded = meters.roundToInt()
    if (rounded < METERS_PER_KM) return "a $rounded m"
    val km = NumberFormat.getNumberInstance(localeCo).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(meters / METERS_PER_KM)
    return "a $km km"
}

private const val METERS_PER_KM = 1000
