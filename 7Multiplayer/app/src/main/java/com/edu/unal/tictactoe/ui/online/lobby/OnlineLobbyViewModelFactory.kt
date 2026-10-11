package com.edu.unal.tictactoe.ui.online.lobby

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.edu.unal.tictactoe.data.online.NicknameStore
import com.edu.unal.tictactoe.data.online.OnlineGameService

/** Production wiring: the only place that gives the lobby ViewModel its service and nickname store. */
fun onlineLobbyViewModelFactory(
    service: OnlineGameService,
    nicknameStore: NicknameStore,
): ViewModelProvider.Factory = viewModelFactory {
    initializer { OnlineLobbyViewModel(service, nicknameStore) }
}
