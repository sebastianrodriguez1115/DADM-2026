package com.edu.unal.tictactoe

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edu.unal.tictactoe.ui.game.DifficultyLabels
import com.edu.unal.tictactoe.ui.game.GameScreen
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.audio.AndroidMoveSoundPlayerFactory
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundHost
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme

class MainActivity : ComponentActivity() {
    private lateinit var gameViewModel: GameViewModel
    private lateinit var moveSoundHost: MoveSoundHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Resolve from the Activity's store once so lifecycle callbacks and Compose share it.
        gameViewModel = ViewModelProvider(this)[GameViewModel::class.java]
        moveSoundHost = MoveSoundHost(
            gameViewModel,
            AndroidMoveSoundPlayerFactory(applicationContext),
        )
        setContent {
            val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current.applicationContext

            TicTacToeTheme {
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
                    onExit = { finish() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        moveSoundHost.onResume()
    }

    override fun onPause() {
        moveSoundHost.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        moveSoundHost.onDestroy()
        super.onDestroy()
    }
}
