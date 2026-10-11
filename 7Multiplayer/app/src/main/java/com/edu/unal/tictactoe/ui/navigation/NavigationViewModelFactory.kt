package com.edu.unal.tictactoe.ui.navigation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.createSavedStateHandle

/** Production wiring for [NavigationViewModel], backed by the Activity's saved state. */
fun navigationViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        NavigationViewModel(savedStateHandle = createSavedStateHandle())
    }
}
