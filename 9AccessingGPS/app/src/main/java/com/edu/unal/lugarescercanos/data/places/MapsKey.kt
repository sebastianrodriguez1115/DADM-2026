package com.edu.unal.lugarescercanos.data.places

/** Llave de Google Maps (D11): el valor por defecto indica que no hay llave configurada. */
object MapsKey {
    const val DEFAULT_API_KEY = "DEFAULT_API_KEY"

    /** True cuando la llave existe y no es el valor por defecto de `local.defaults.properties`. */
    fun isConfigured(key: String): Boolean = key.isNotBlank() && key != DEFAULT_API_KEY
}
