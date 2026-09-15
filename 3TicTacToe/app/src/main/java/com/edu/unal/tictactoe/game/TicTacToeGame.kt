package com.edu.unal.tictactoe.game

import java.util.Collections
import com.edu.unal.tictactoe.game.models.ComputerMoveResult
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.MoveResult
import com.edu.unal.tictactoe.game.models.Player

private const val BOARD_SIZE = 9

private val WINNING_LINES = listOf(
    intArrayOf(0, 1, 2),
    intArrayOf(3, 4, 5),
    intArrayOf(6, 7, 8),
    intArrayOf(0, 3, 6),
    intArrayOf(1, 4, 7),
    intArrayOf(2, 5, 8),
    intArrayOf(0, 4, 8),
    intArrayOf(2, 4, 6),
)

class TicTacToeGame(
    private val moveRandomizer: MoveRandomizer = MoveRandomizer.Default,
) {
    private val cells = MutableList(BOARD_SIZE) { Mark.Empty }

    val board: List<Mark>
        get() = Collections.unmodifiableList(ArrayList(cells))

    fun setMove(player: Player, location: Int): MoveResult {
        // Location is checked first, then terminal state, then occupancy.
        if (location !in cells.indices) {
            return MoveResult.InvalidLocation
        }
        if (checkForWinner() != GameResult.InProgress) {
            return MoveResult.GameAlreadyFinished
        }
        if (cells[location] != Mark.Empty) {
            return MoveResult.Occupied
        }

        cells[location] = when (player) {
            Player.X -> Mark.X
            Player.O -> Mark.O
        }
        return MoveResult.Applied
    }

    fun checkForWinner(): GameResult {
        return resultFor(cells)
    }

    fun getComputerMove(): ComputerMoveResult {
        when (checkForWinner()) {
            is GameResult.Won -> return ComputerMoveResult.GameAlreadyFinished
            GameResult.Tie -> return ComputerMoveResult.NoMoves
            GameResult.InProgress -> Unit
        }

        val freeLocations = cells.indices.filter { cells[it] == Mark.Empty }

        for (location in freeLocations) {
            val probe = cells.toMutableList()
            probe[location] = Mark.O
            if (resultFor(probe) == GameResult.Won(Player.O)) {
                return ComputerMoveResult.Selected(location)
            }
        }

        for (location in freeLocations) {
            val probe = cells.toMutableList()
            probe[location] = Mark.X
            if (resultFor(probe) == GameResult.Won(Player.X)) {
                return ComputerMoveResult.Selected(location)
            }
        }

        val randomIndex = moveRandomizer.nextInt(freeLocations.size)
        return ComputerMoveResult.Selected(freeLocations[randomIndex])
    }

    fun clearBoard() {
        cells.fill(Mark.Empty)
    }

    private fun resultFor(board: List<Mark>): GameResult {
        for (line in WINNING_LINES) {
            val mark = board[line[0]]
            if (mark != Mark.Empty && mark == board[line[1]] && mark == board[line[2]]) {
                return when (mark) {
                    Mark.X -> GameResult.Won(Player.X)
                    Mark.O -> GameResult.Won(Player.O)
                    Mark.Empty -> error("An empty mark cannot win")
                }
            }
        }

        return if (board.none { it == Mark.Empty }) {
            GameResult.Tie
        } else {
            GameResult.InProgress
        }
    }
}
