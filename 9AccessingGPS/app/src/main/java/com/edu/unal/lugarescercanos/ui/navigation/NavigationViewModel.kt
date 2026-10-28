package com.edu.unal.lugarescercanos.ui.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda la [Screen] actual. Empieza en el mapa y no depende de ningún repositorio.
 * Desde Ajustes, volver siempre lleva al mapa.
 */
class NavigationViewModel : ViewModel() {
    private val _screen = MutableStateFlow<Screen>(Screen.Map)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    fun openSettings() {
        _screen.value = Screen.Settings
    }

    /** Vuelve al mapa. Lo usan el botón Volver de Ajustes y el Back del sistema. */
    fun back() {
        _screen.value = Screen.Map
    }
}
