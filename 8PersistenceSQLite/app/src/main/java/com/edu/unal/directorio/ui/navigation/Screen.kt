package com.edu.unal.directorio.ui.navigation

/** The screens of the app. Navigation is manual: one of these is current at any time. */
sealed interface Screen {
    /** Company list, the start screen. */
    data object List : Screen

    /** Detail of the company with the given [id]. */
    data class Detail(val id: Long) : Screen

    /** Company form. [id] is `null` when creating and the company id when editing. */
    data class Form(val id: Long?) : Screen
}
