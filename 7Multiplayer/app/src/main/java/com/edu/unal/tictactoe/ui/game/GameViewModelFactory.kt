package com.edu.unal.tictactoe.ui.game

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.edu.unal.tictactoe.data.preferences.SharedPreferencesGamePreferences

/** Production wiring: the only place that gives the ViewModel the real, application-scoped store. */
fun gameViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        GameViewModel(
            preferences = SharedPreferencesGamePreferences(application()),
            savedStateHandle = createSavedStateHandle(),
        )
    }
}

private fun CreationExtras.application(): Application =
    checkNotNull(this[APPLICATION_KEY]) { "Application is missing from CreationExtras" }
