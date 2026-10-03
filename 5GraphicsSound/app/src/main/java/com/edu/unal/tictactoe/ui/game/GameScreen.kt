package com.edu.unal.tictactoe.ui.game

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.edu.unal.tictactoe.R
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.ui.game.models.GameUiState

private enum class GameWindow { Difficulty, Exit, About }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    uiState: GameUiState,
    onCellClick: (Int) -> Unit,
    onNewGame: () -> Unit,
    onToggleSound: () -> Unit,
    onDifficultySelected: (DifficultyLevel) -> Boolean,
    onShowToast: (DifficultyLevel) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeWindow by remember { mutableStateOf<GameWindow?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
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
                        onAbout = { activeWindow = GameWindow.About },
                        onExit = { activeWindow = GameWindow.Exit },
                    )
                },
            )
        },
    ) { innerPadding ->
        TicTacToeScreen(
            uiState = uiState,
            onCellClick = onCellClick,
            onNewGame = onNewGame,
            modifier = Modifier.padding(innerPadding),
        )
    }

    when (activeWindow) {
        GameWindow.Difficulty -> DifficultyDialog(
            currentDifficulty = uiState.difficulty,
            onDifficultySelected = onDifficultySelected,
            onDismiss = { activeWindow = null },
            onShowToast = onShowToast,
        )

        GameWindow.Exit -> ExitConfirmationDialog(
            onCancel = { activeWindow = null },
            onConfirm = {
                activeWindow = null
                onExit()
            },
        )

        GameWindow.About -> AboutDialog(onDismiss = { activeWindow = null })

        null -> Unit
    }
}
