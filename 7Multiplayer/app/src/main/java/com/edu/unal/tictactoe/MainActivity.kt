package com.edu.unal.tictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.audio.AndroidMoveSoundPlayerFactory
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundHost
import com.edu.unal.tictactoe.ui.game.gameViewModelFactory
import com.edu.unal.tictactoe.ui.navigation.AppNavigation
import com.edu.unal.tictactoe.ui.navigation.AppScreen
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import com.edu.unal.tictactoe.ui.navigation.navigationViewModelFactory
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var gameViewModel: GameViewModel
    private lateinit var navigationViewModel: NavigationViewModel
    private lateinit var moveSoundHost: MoveSoundHost

    // The local mode runs only while both hold; see syncLocalModeHost.
    private var activityResumed = false
    private var localScreenShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Resolve from the Activity's store once so lifecycle callbacks and Compose share it.
        gameViewModel = ViewModelProvider(this, gameViewModelFactory())[GameViewModel::class.java]
        navigationViewModel = ViewModelProvider(this, navigationViewModelFactory())[NavigationViewModel::class.java]
        moveSoundHost = MoveSoundHost(
            gameViewModel,
            AndroidMoveSoundPlayerFactory(applicationContext),
        )
        lifecycleScope.launch {
            navigationViewModel.currentScreen.collect { screen ->
                localScreenShown = screen == AppScreen.Local
                syncLocalModeHost()
            }
        }
        setContent {
            TicTacToeTheme {
                AppNavigation(
                    navigationViewModel = navigationViewModel,
                    gameViewModel = gameViewModel,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        syncLocalModeHost()
    }

    override fun onPause() {
        activityResumed = false
        syncLocalModeHost()
        super.onPause()
    }

    override fun onDestroy() {
        moveSoundHost.onDestroy()
        super.onDestroy()
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
