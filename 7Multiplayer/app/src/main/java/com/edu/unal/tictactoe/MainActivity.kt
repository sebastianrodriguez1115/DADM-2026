package com.edu.unal.tictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.edu.unal.tictactoe.data.online.OnlineServices
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.audio.AndroidMoveSoundPlayerFactory
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundController
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundHost
import com.edu.unal.tictactoe.ui.game.gameViewModelFactory
import com.edu.unal.tictactoe.ui.navigation.AppNavigation
import com.edu.unal.tictactoe.ui.navigation.AppScreen
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import com.edu.unal.tictactoe.ui.navigation.navigationViewModelFactory
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyViewModel
import com.edu.unal.tictactoe.ui.online.lobby.onlineLobbyViewModelFactory
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchViewModel
import com.edu.unal.tictactoe.ui.online.match.onlineMatchViewModelFactory
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var gameViewModel: GameViewModel
    private lateinit var navigationViewModel: NavigationViewModel
    private lateinit var lobbyViewModel: OnlineLobbyViewModel
    private lateinit var matchViewModel: OnlineMatchViewModel
    private lateinit var moveSoundHost: MoveSoundHost
    private lateinit var onlineMoveSound: MoveSoundController

    // The local mode runs only while the first and second hold, the online sound while the first
    // and third do; see syncSoundControllers.
    private var activityResumed = false
    private var localScreenShown = false
    private var matchScreenShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Resolve from the Activity's store once so lifecycle callbacks and Compose share it.
        gameViewModel = ViewModelProvider(this, gameViewModelFactory())[GameViewModel::class.java]
        navigationViewModel = ViewModelProvider(this, navigationViewModelFactory())[NavigationViewModel::class.java]
        lobbyViewModel = ViewModelProvider(
            this,
            onlineLobbyViewModelFactory(
                service = OnlineServices.gameService,
                nicknameStore = OnlineServices.nicknameStore(applicationContext),
            ),
        )[OnlineLobbyViewModel::class.java]
        matchViewModel = ViewModelProvider(
            this,
            onlineMatchViewModelFactory(OnlineServices.gameService),
        )[OnlineMatchViewModel::class.java]
        moveSoundHost = MoveSoundHost(
            gameViewModel,
            AndroidMoveSoundPlayerFactory(applicationContext),
        )
        onlineMoveSound = MoveSoundController(
            playerFactory = AndroidMoveSoundPlayerFactory(applicationContext),
            soundEnabled = { gameViewModel.uiState.value.soundEnabled },
        )
        lifecycleScope.launch {
            navigationViewModel.currentScreen.collect { screen ->
                localScreenShown = screen == AppScreen.Local
                matchScreenShown = screen == AppScreen.Match
                syncSoundControllers()
            }
        }
        setContent {
            TicTacToeTheme {
                AppNavigation(
                    navigationViewModel = navigationViewModel,
                    gameViewModel = gameViewModel,
                    lobbyViewModel = lobbyViewModel,
                    matchViewModel = matchViewModel,
                    onlineMoveSound = onlineMoveSound,
                    shareLink = BuildConfig.SHARE_URL,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        syncSoundControllers()
    }

    override fun onPause() {
        activityResumed = false
        syncSoundControllers()
        super.onPause()
    }

    override fun onDestroy() {
        moveSoundHost.onDestroy()
        onlineMoveSound.release()
        super.onDestroy()
    }

    /**
     * Keeps at most one of the two sound controllers prepared. The screens are exclusive, so at
     * most one of the local and match conditions holds, and whichever controller is not current is
     * paused first, so they are never both prepared, not even within one call.
     */
    private fun syncSoundControllers() {
        if (activityResumed && matchScreenShown) {
            syncLocalModeHost()
            syncOnlineSound()
        } else {
            syncOnlineSound()
            syncLocalModeHost()
        }
    }

    /** Prepares the online match sound only while the Activity is RESUMED and the match is current. */
    private fun syncOnlineSound() {
        if (activityResumed && matchScreenShown) onlineMoveSound.prepare() else onlineMoveSound.pause()
    }

    /**
     * Resumes the local mode (pending computer turn and sound) only while the Activity is RESUMED
     * and the local screen is current, and pauses it otherwise. Safe to call repeatedly: the host,
     * the sound controller and the ViewModel's foreground callbacks are idempotent.
     */
    private fun syncLocalModeHost() {
        if (activityResumed && localScreenShown) moveSoundHost.onResume() else moveSoundHost.onPause()
    }
}
