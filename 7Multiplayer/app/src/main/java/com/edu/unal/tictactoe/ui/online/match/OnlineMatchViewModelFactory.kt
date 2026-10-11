package com.edu.unal.tictactoe.ui.online.match

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.edu.unal.tictactoe.data.online.OnlineGameService

/** Production wiring: the only place that gives the match ViewModel its service. */
fun onlineMatchViewModelFactory(service: OnlineGameService): ViewModelProvider.Factory = viewModelFactory {
    initializer { OnlineMatchViewModel(service) }
}
