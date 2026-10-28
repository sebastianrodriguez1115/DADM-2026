package com.edu.unal.lugarescercanos.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.unal.lugarescercanos.ui.map.MapScreen
import com.edu.unal.lugarescercanos.ui.settings.SettingsScreen
import com.edu.unal.lugarescercanos.ui.settings.SettingsViewModel

/** Dibuja la pantalla actual. El ViewModel de navegación está ligado a la Activity. */
@Composable
fun AppNavigation(navigation: NavigationViewModel = viewModel()) {
    val screen by navigation.screen.collectAsState()

    when (screen) {
        Screen.Map -> MapScreen(onOpenSettings = navigation::openSettings)
        Screen.Settings -> {
            // En el mapa el Back del sistema cierra la app; en Ajustes vuelve al mapa.
            BackHandler { navigation.back() }
            SettingsRoute(onBack = navigation::back)
        }
    }
}

/** Conecta la pantalla de Ajustes con su ViewModel. */
@Composable
private fun SettingsRoute(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory())
    val state by viewModel.state.collectAsState()
    SettingsScreen(
        state = state,
        onRadiusChange = viewModel::onRadiusChanged,
        onRadiusChangeFinished = viewModel::onRadiusChangeFinished,
        onBack = onBack,
    )
}
