package com.edu.unal.tictactoe

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.unal.tictactoe.ui.game.DifficultyLabels
import com.edu.unal.tictactoe.ui.game.GameScreen
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val gameViewModel: GameViewModel = viewModel()
            val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current.applicationContext

            TicTacToeTheme {
                GameScreen(
                    uiState = uiState,
                    onCellClick = gameViewModel::onHumanMove,
                    onNewGame = gameViewModel::newGame,
                    onDifficultySelected = gameViewModel::setDifficulty,
                    onShowToast = { level ->
                        Toast.makeText(
                            context,
                            DifficultyLabels.labelRes(level),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    onExit = { finish() },
                )
            }
        }
    }
}
