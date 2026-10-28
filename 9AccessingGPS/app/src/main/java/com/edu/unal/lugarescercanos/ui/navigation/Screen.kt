package com.edu.unal.lugarescercanos.ui.navigation

/** Las pantallas de la app. La navegación es manual: una de estas está activa en todo momento. */
sealed interface Screen {
    /** Mapa de lugares, la pantalla inicial. */
    data object Map : Screen

    /** Ajustes del radio de búsqueda. */
    data object Settings : Screen
}
