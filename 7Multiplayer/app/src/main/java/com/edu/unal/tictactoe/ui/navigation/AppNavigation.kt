package com.edu.unal.tictactoe.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edu.unal.tictactoe.R
import com.edu.unal.tictactoe.ui.game.DifficultyLabels
import com.edu.unal.tictactoe.ui.game.GameScreen
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.start.StartScreen

/**
 * Root composable: draws the screen held by [navigationViewModel] and declares the Back handlers.
 *
 * Back goes from [AppScreen.Local] and [AppScreen.Lobby] to [AppScreen.Start]; [AppScreen.Start]
 * declares no handler, so the system applies its normal Back behavior. A menu or dialog is its own
 * window and consumes Back before these handlers.
 *
 * Every screen's state holder is a parameter, so a test can mount this with fakes and without the
 * real Activity.
 */
@Composable
fun AppNavigation(
    navigationViewModel: NavigationViewModel,
    gameViewModel: GameViewModel,
    modifier: Modifier = Modifier,
) {
    val currentScreen by navigationViewModel.currentScreen.collectAsStateWithLifecycle()

    when (currentScreen) {
        AppScreen.Start -> StartScreen(
            onPlayLocal = { navigationViewModel.navigateTo(AppScreen.Local) },
            onPlayOnline = { navigationViewModel.navigateTo(AppScreen.Lobby) },
            modifier = modifier,
        )

        AppScreen.Local -> {
            BackHandler { navigationViewModel.navigateTo(AppScreen.Start) }
            val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current.applicationContext
            GameScreen(
                uiState = uiState,
                onCellClick = gameViewModel::onHumanMove,
                onNewGame = gameViewModel::newGame,
                onToggleSound = gameViewModel::toggleSound,
                onDifficultySelected = gameViewModel::setDifficulty,
                onShowToast = { level ->
                    Toast.makeText(
                        context,
                        DifficultyLabels.labelRes(level),
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                onResetScores = gameViewModel::resetScores,
                modifier = modifier,
            )
        }

        AppScreen.Lobby -> {
            BackHandler { navigationViewModel.navigateTo(AppScreen.Start) }
            ProvisionalOnlineScreen(modifier)
        }

        AppScreen.Match -> ProvisionalOnlineScreen(modifier)
    }
}

/** Provisional stand-in for the online screens; the lobby and match tasks replace and remove it. */
@Composable
private fun ProvisionalOnlineScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.online_provisional_placeholder))
    }
}
