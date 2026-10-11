package com.edu.unal.tictactoe.ui.navigation

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationViewModelTest {
    private fun handleOf(value: Any?) = SavedStateHandle().apply {
        if (value != null) set(NavigationViewModel.SAVED_STATE_KEY, value)
    }

    @Test
    fun freshLaunchStartsAtStart() {
        assertEquals(AppScreen.Start, NavigationViewModel(SavedStateHandle()).currentScreen.value)
    }

    @Test
    fun savedLocalIsRestored() {
        assertEquals(AppScreen.Local, NavigationViewModel(handleOf("Local")).currentScreen.value)
    }

    @Test
    fun savedLobbyIsRestored() {
        assertEquals(AppScreen.Lobby, NavigationViewModel(handleOf("Lobby")).currentScreen.value)
    }

    @Test
    fun savedMatchRestoresLobby() {
        val handle = handleOf("Match")

        assertEquals(AppScreen.Lobby, NavigationViewModel(handle).currentScreen.value)
        assertEquals("Lobby", handle.get<String>(NavigationViewModel.SAVED_STATE_KEY))
    }

    @Test
    fun unknownSavedValueStartsAtStart() {
        assertEquals(AppScreen.Start, NavigationViewModel(handleOf("Settings")).currentScreen.value)
    }

    @Test
    fun nonStringSavedValueStartsAtStart() {
        assertEquals(AppScreen.Start, NavigationViewModel(handleOf(42)).currentScreen.value)
    }

    @Test
    fun navigatingUpdatesFlowAndSavedValue() {
        val handle = SavedStateHandle()
        val viewModel = NavigationViewModel(handle)

        AppScreen.entries.forEach { screen ->
            viewModel.navigateTo(screen)

            assertEquals(screen, viewModel.currentScreen.value)
            assertEquals(screen.name, handle.get<String>(NavigationViewModel.SAVED_STATE_KEY))
        }
    }

    @Test
    fun newInstanceOnSameHandleKeepsTheScreen() {
        val handle = SavedStateHandle()
        NavigationViewModel(handle).navigateTo(AppScreen.Local)

        assertEquals(AppScreen.Local, NavigationViewModel(handle).currentScreen.value)
    }

    @Test
    fun newInstanceOnSameHandleAfterMatchGoesToLobby() {
        val handle = SavedStateHandle()
        NavigationViewModel(handle).navigateTo(AppScreen.Match)

        assertEquals(AppScreen.Lobby, NavigationViewModel(handle).currentScreen.value)
    }
}
