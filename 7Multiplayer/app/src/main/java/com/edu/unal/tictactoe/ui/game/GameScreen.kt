package com.edu.unal.tictactoe.ui.game

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.ui.game.models.GameUiState

private enum class GameWindow { Difficulty, About }

/**
 * Main game screen: the board and controls with the options menu next to them, plus the
 * Difficulty and About dialogs.
 *
 * There is no top app bar or app title in any orientation. The [Scaffold] keeps handling window
 * insets (system bars, display cutouts and the keyboard, via [WindowInsets.Companion.safeDrawing])
 * and passes only that padding to [TicTacToeScreen], which chooses the portrait or landscape
 * arrangement from its constraints (see its documentation for the rules and the compact
 * fallback). The compact menu button sits in the controls row beside the "New game" shortcut.
 *
 * The menu has five entries (see [GameMenu]); there is no exit option, so Back keeps the normal
 * system navigation. Only the score and the difficulty are persistent; the game in progress is
 * not permanent storage. [onResetScores] applies immediately and only affects the three counters.
 */
@Composable
fun GameScreen(
    uiState: GameUiState,
    onCellClick: (Int) -> Unit,
    onNewGame: () -> Unit,
    onToggleSound: () -> Unit,
    onDifficultySelected: (DifficultyLevel) -> Boolean,
    onShowToast: (DifficultyLevel) -> Unit,
    onResetScores: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeWindow by remember { mutableStateOf<GameWindow?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        TicTacToeScreen(
            uiState = uiState,
            onCellClick = onCellClick,
            onNewGame = onNewGame,
            modifier = Modifier.padding(innerPadding),
            menu = {
                GameMenu(
                    canChangeDifficulty = uiState.canChangeDifficulty,
                    soundEnabled = uiState.soundEnabled,
                    onNewGame = onNewGame,
                    onToggleSound = onToggleSound,
                    onDifficulty = {
                        if (uiState.canChangeDifficulty) {
                            activeWindow = GameWindow.Difficulty
                        }
                    },
                    onResetScores = onResetScores,
                    onAbout = { activeWindow = GameWindow.About },
                )
            },
        )
    }

    when (activeWindow) {
        GameWindow.Difficulty -> DifficultyDialog(
            currentDifficulty = uiState.difficulty,
            onDifficultySelected = onDifficultySelected,
            onDismiss = { activeWindow = null },
            onShowToast = onShowToast,
        )

        GameWindow.About -> AboutDialog(onDismiss = { activeWindow = null })

        null -> Unit
    }
}
