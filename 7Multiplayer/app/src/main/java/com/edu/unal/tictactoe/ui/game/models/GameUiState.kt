package com.edu.unal.tictactoe.ui.game.models

import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
import java.util.Collections

private const val BOARD_SIZE = 9

class GameUiState(
    board: List<Mark> = List(BOARD_SIZE) { Mark.Empty },
    val turn: Turn = Turn.Human,
    val result: GameResult = GameResult.InProgress,
    val starter: Player = Player.X,
    val nextStarter: Player = Player.O,
    val score: Score = Score(),
    val difficulty: DifficultyLevel = DifficultyLevel.Expert,
    val soundEnabled: Boolean = true,
) {
    init {
        require(board.size == BOARD_SIZE)
    }

    private val boardSnapshot: List<Mark> =
        Collections.unmodifiableList(ArrayList(board))

    val board: List<Mark>
        get() = boardSnapshot

    val canChangeDifficulty: Boolean =
        result != GameResult.InProgress || boardSnapshot.all { it == Mark.Empty }
}
