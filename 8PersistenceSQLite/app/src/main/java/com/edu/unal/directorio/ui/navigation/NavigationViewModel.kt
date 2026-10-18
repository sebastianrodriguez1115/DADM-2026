package com.edu.unal.directorio.ui.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the current [Screen]. It starts on the list and does not depend on the repository.
 *
 * Destinations: detail and create go back to the list; edit goes back to that company's detail.
 */
class NavigationViewModel : ViewModel() {
    private val _screen = MutableStateFlow<Screen>(Screen.List)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    fun openDetail(id: Long) {
        _screen.value = Screen.Detail(id)
    }

    /** Opens the form to create a company. */
    fun openCreate() {
        _screen.value = Screen.Form(null)
    }

    /** Opens the form to edit the company with the given [id]. */
    fun openEdit(id: Long) {
        _screen.value = Screen.Form(id)
    }

    /** Goes to the origin screen. Used by Back and Cancel. Does nothing on the list. */
    fun back() {
        _screen.value = when (val current = _screen.value) {
            Screen.List -> Screen.List
            is Screen.Detail -> Screen.List
            is Screen.Form -> current.id?.let { Screen.Detail(it) } ?: Screen.List
        }
    }

    /** After saving: the list when creating, that company's detail when editing. */
    fun onSaved() = back()

    /** After deleting a company (or when it no longer exists): the list. */
    fun onDeleted() {
        _screen.value = Screen.List
    }
}
