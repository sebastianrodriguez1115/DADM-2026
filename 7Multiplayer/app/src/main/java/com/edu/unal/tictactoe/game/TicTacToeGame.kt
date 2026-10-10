package com.edu.unal.tictactoe.game

import java.util.Collections
import kotlin.math.abs
import com.edu.unal.tictactoe.game.models.ComputerMoveResult
import com.edu.unal.tictactoe.game.models.DifficultyLevel
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

private data class BoardKey(val board: List<Mark>, val nextPlayer: Player)

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

    fun getComputerMove(difficulty: DifficultyLevel = DifficultyLevel.Expert): ComputerMoveResult {
        when (checkForWinner()) {
            is GameResult.Won -> return ComputerMoveResult.GameAlreadyFinished
            GameResult.Tie -> return ComputerMoveResult.NoMoves
            GameResult.InProgress -> Unit
        }

        val freeLocations = cells.indices.filter { cells[it] == Mark.Empty }

        return when (difficulty) {
            DifficultyLevel.Easy -> ComputerMoveResult.Selected(randomLocation(freeLocations))

            DifficultyLevel.Hard -> ComputerMoveResult.Selected(
                immediateWinningLocation(freeLocations) ?: randomLocation(freeLocations),
            )

            DifficultyLevel.Expert -> ComputerMoveResult.Selected(
                immediateWinningLocation(freeLocations)
                    ?: blockingLocation(freeLocations)
                    ?: randomLocation(freeLocations),
            )

            DifficultyLevel.Impossible -> ComputerMoveResult.Selected(impossibleLocation(freeLocations))
        }
    }

    private fun immediateWinningLocation(freeLocations: List<Int>): Int? {
        for (location in freeLocations) {
            val probe = cells.toMutableList()
            probe[location] = Mark.O
            if (resultFor(probe) == GameResult.Won(Player.O)) {
                return location
            }
        }
        return null
    }

    private fun blockingLocation(freeLocations: List<Int>): Int? {
        for (location in freeLocations) {
            val probe = cells.toMutableList()
            probe[location] = Mark.X
            if (resultFor(probe) == GameResult.Won(Player.X)) {
                return location
            }
        }
        return null
    }

    private fun impossibleLocation(freeLocations: List<Int>): Int {
        val memo = HashMap<BoardKey, Int>()
        val probe = cells.toMutableList()
        var bestScore = Int.MIN_VALUE
        var bestLocation = freeLocations.first()
        for (location in freeLocations) {
            probe[location] = Mark.O
            val score = minimax(probe, Player.X, memo)
            probe[location] = Mark.Empty
            if (score > bestScore) {
                bestScore = score
                bestLocation = location
            }
        }
        return bestLocation
    }

    private fun minimax(
        board: MutableList<Mark>,
        nextPlayer: Player,
        memo: MutableMap<BoardKey, Int>,
    ): Int {
        when (val result = resultFor(board)) {
            is GameResult.Won -> return if (result.player == Player.O) 1 else -1
            GameResult.Tie -> return 0
            GameResult.InProgress -> Unit
        }

        val key = BoardKey(board.toList(), nextPlayer)
        memo[key]?.let { return it }

        var best = if (nextPlayer == Player.O) Int.MIN_VALUE else Int.MAX_VALUE
        for (location in board.indices) {
            if (board[location] != Mark.Empty) continue
            board[location] = if (nextPlayer == Player.O) Mark.O else Mark.X
            val score = minimax(board, if (nextPlayer == Player.O) Player.X else Player.O, memo)
            board[location] = Mark.Empty
            best = if (nextPlayer == Player.O) maxOf(best, score) else minOf(best, score)
        }
        memo[key] = best
        return best
    }

    private fun randomLocation(freeLocations: List<Int>): Int {
        return freeLocations[moveRandomizer.nextInt(freeLocations.size)]
    }

    fun clearBoard() {
        cells.fill(Mark.Empty)
    }

    /**
     * Replaces the whole board with [marks] when it is an intrinsically legal position,
     * without going through the move path (no computer move, no randomizer use).
     * Returns false and leaves the board untouched otherwise. Turn and starter
     * consistency are the caller's responsibility.
     */
    fun restoreBoard(marks: List<Mark>): Boolean {
        if (marks.size != BOARD_SIZE) {
            return false
        }
        val candidate = ArrayList(marks)
        if (!isLegalPosition(candidate)) {
            return false
        }
        for (index in candidate.indices) {
            cells[index] = candidate[index]
        }
        return true
    }

    private fun isLegalPosition(board: List<Mark>): Boolean {
        val xCount = board.count { it == Mark.X }
        val oCount = board.count { it == Mark.O }
        if (abs(xCount - oCount) > 1) {
            return false
        }
        val xWins = hasWinningLine(board, Mark.X)
        val oWins = hasWinningLine(board, Mark.O)
        return when {
            xWins && oWins -> false
            xWins -> xCount >= oCount
            oWins -> oCount >= xCount
            else -> true
        }
    }

    private fun hasWinningLine(board: List<Mark>, mark: Mark): Boolean {
        return WINNING_LINES.any { line -> line.all { board[it] == mark } }
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
