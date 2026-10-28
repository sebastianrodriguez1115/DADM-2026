package com.edu.unal.lugarescercanos.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.edu.unal.lugarescercanos.LugaresApplication
import com.edu.unal.lugarescercanos.data.RadiusPreferences
import com.edu.unal.lugarescercanos.domain.RadiusRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de la pantalla de Ajustes: el radio elegido y su texto con formato es-CO. */
data class SettingsUiState(val radiusKm: Double, val radiusText: String) {
    companion object {
        fun forRadius(km: Double): SettingsUiState = SettingsUiState(km, RadiusRules.format(km))
    }
}

/**
 * Ajustes del radio. Mientras el slider se mueve solo cambia [state]; al soltarlo
 * ([onRadiusChangeFinished]) guarda el valor ajustado (D8, `specs/search-radius-settings`).
 */
class SettingsViewModel(private val preferences: RadiusPreferences) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState.forRadius(RadiusRules.DEFAULT_KM))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.radiusKm.collect { saved ->
                _state.value = SettingsUiState.forRadius(RadiusRules.clamp(saved))
            }
        }
    }

    /** Solo cambia el estado de la UI, sin guardar. */
    fun onRadiusChanged(km: Double) {
        _state.value = SettingsUiState.forRadius(RadiusRules.clamp(km))
    }

    /** Guarda el radio actual al soltar el slider. */
    fun onRadiusChangeFinished() {
        val km = _state.value.radiusKm
        viewModelScope.launch { preferences.setRadiusKm(km) }
    }

    companion object {
        /** Fábrica manual, sin framework de inyección (como en el taller 8). */
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(radiusPreferencesFrom(this)) }
        }

        private fun radiusPreferencesFrom(extras: CreationExtras): RadiusPreferences =
            (extras[APPLICATION_KEY] as LugaresApplication).container.radiusPreferences
    }
}
