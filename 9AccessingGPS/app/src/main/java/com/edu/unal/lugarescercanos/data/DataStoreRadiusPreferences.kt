package com.edu.unal.lugarescercanos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.edu.unal.lugarescercanos.domain.RadiusRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * [RadiusPreferences] sobre DataStore Preferences. Guarda un `Float` con la clave
 * `search_radius_km` y aplica `RadiusRules.clamp` al leer y al escribir.
 */
class DataStoreRadiusPreferences(private val dataStore: DataStore<Preferences>) : RadiusPreferences {

    override val radiusKm: Flow<Double> = dataStore.data.map { prefs -> readRadius(prefs) }

    override suspend fun setRadiusKm(km: Double) {
        val clamped = RadiusRules.clamp(km).toFloat()
        dataStore.edit { prefs -> prefs[RADIUS_KEY] = clamped }
    }

    private fun readRadius(prefs: Preferences): Double {
        val saved = prefs[RADIUS_KEY] ?: return RadiusRules.DEFAULT_KM
        return RadiusRules.clamp(saved.toDouble())
    }

    companion object {
        private val RADIUS_KEY = floatPreferencesKey("search_radius_km")

        /** Repositorio sobre el almacén de preferencias de la aplicación. */
        fun fromContext(context: Context): DataStoreRadiusPreferences =
            DataStoreRadiusPreferences(context.settingsDataStore)
    }
}
