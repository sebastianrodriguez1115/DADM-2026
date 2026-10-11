package com.edu.unal.tictactoe.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the current [AppScreen] and mirrors its enum name into [savedStateHandle] on every change.
 *
 * Restore (at construction): no saved value or an unknown one starts at [AppScreen.Start];
 * [AppScreen.Local] and [AppScreen.Lobby] are restored as saved; a saved [AppScreen.Match] starts
 * at [AppScreen.Lobby], because an online match cannot be rebuilt after process death.
 */
class NavigationViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _currentScreen = MutableStateFlow(restoreScreen())
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    init {
        savedStateHandle[SAVED_STATE_KEY] = _currentScreen.value.name
    }

    fun navigateTo(screen: AppScreen) {
        savedStateHandle[SAVED_STATE_KEY] = screen.name
        _currentScreen.value = screen
    }

    private fun restoreScreen(): AppScreen {
        val saved = AppScreen.entries.firstOrNull { it.name == savedStateHandle.get<String>(SAVED_STATE_KEY) }
        return when (saved) {
            null -> AppScreen.Start
            AppScreen.Match -> AppScreen.Lobby
            else -> saved
        }
    }

    companion object {
        const val SAVED_STATE_KEY = "app_screen"
    }
}
