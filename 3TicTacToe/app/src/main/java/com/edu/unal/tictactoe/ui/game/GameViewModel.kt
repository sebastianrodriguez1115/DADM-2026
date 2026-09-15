package com.edu.unal.tictactoe.ui.game

import androidx.lifecycle.ViewModel
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.ComputerMoveResult
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.MoveResult
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(
    private val game: TicTacToeGame = TicTacToeGame(),
) : ViewModel() {
    private val _uiState: MutableStateFlow<GameUiState>

    val uiState: StateFlow<GameUiState>

    init {
        game.clearBoard()
        _uiState = MutableStateFlow(
            GameUiState(
                board = game.board,
                turn = Turn.Human,
                result = GameResult.InProgress,
                starter = Player.X,
                nextStarter = Player.O,
                score = Score(),
            ),
        )
        uiState = _uiState.asStateFlow()
    }

    fun onHumanMove(location: Int) {
        val currentState = _uiState.value
        if (currentState.result != GameResult.InProgress || currentState.turn != Turn.Human) {
            return
        }

        if (game.setMove(Player.X, location) != MoveResult.Applied) {
            return
        }

        val resultAfterHuman = game.checkForWinner()
        if (resultAfterHuman != GameResult.InProgress) {
            publishState(turn = Turn.Human, result = resultAfterHuman)
            return
        }

        publishState(turn = Turn.Computer, result = GameResult.InProgress)
        playComputerTurn()
    }

    fun newGame() {
        val currentState = _uiState.value
        val starter = currentState.nextStarter
        val nextStarter = otherPlayer(starter)
        game.clearBoard()

        if (starter == Player.O) {
            publishState(
                turn = Turn.Computer,
                result = GameResult.InProgress,
                starter = starter,
                nextStarter = nextStarter,
            )
            playComputerTurn()
        } else {
            publishState(
                turn = Turn.Human,
                result = GameResult.InProgress,
                starter = starter,
                nextStarter = nextStarter,
            )
        }
    }

    private fun playComputerTurn() {
        when (val computerMove = game.getComputerMove()) {
            is ComputerMoveResult.Selected -> {
                game.setMove(Player.O, computerMove.index)
            }

            ComputerMoveResult.NoMoves,
            ComputerMoveResult.GameAlreadyFinished,
            -> Unit
        }

        val resultAfterComputer = game.checkForWinner()
        val nextTurn = if (resultAfterComputer == GameResult.InProgress) {
            Turn.Human
        } else {
            Turn.Computer
        }
        publishState(turn = nextTurn, result = resultAfterComputer)
    }

    private fun otherPlayer(player: Player): Player = when (player) {
        Player.X -> Player.O
        Player.O -> Player.X
    }

    private fun publishState(turn: Turn, result: GameResult) {
        val currentState = _uiState.value
        publishState(
            turn = turn,
            result = result,
            starter = currentState.starter,
            nextStarter = currentState.nextStarter,
        )
    }

    private fun publishState(
        turn: Turn,
        result: GameResult,
        starter: Player,
        nextStarter: Player,
    ) {
        val currentState = _uiState.value
        _uiState.value = GameUiState(
            board = game.board,
            turn = turn,
            result = result,
            starter = starter,
            nextStarter = nextStarter,
            score = scoreAfterTransition(currentState, result),
        )
    }

    private fun scoreAfterTransition(currentState: GameUiState, result: GameResult): Score {
        if (currentState.result != GameResult.InProgress || result == GameResult.InProgress) {
            return currentState.score
        }

        return when (result) {
            is GameResult.Won -> when (result.player) {
                Player.X -> currentState.score.copy(xWins = currentState.score.xWins + 1)
                Player.O -> currentState.score.copy(oWins = currentState.score.oWins + 1)
            }

            GameResult.Tie -> currentState.score.copy(ties = currentState.score.ties + 1)
            GameResult.InProgress -> currentState.score
        }
    }
}
