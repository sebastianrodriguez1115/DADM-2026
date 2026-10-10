package com.edu.unal.tictactoe.game

import com.edu.unal.tictactoe.game.models.ComputerMoveResult
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.MoveResult
import com.edu.unal.tictactoe.game.models.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImpossibleExhaustiveTest {
    @Test
    fun impossibleNeverLosesWhenHumanXStarts() {
        assertImpossibleNeverLosesFrom(Player.X)
    }

    @Test
    fun impossibleNeverLosesWhenComputerOStarts() {
        assertImpossibleNeverLosesFrom(Player.O)
    }

    private fun assertImpossibleNeverLosesFrom(starter: Player) {
        val randomizer = FailFastRandomizer()
        val coverage = Coverage()
        val game = gameFrom(emptyList(), randomizer)

        explore(game, emptyList(), starter, randomizer, coverage)

        assertEquals(0, randomizer.calls)
        assertTrue("no terminal state reached for $starter", coverage.terminals > 0)
        assertTrue("no drawn terminal reached for $starter", coverage.draws > 0)
    }

    private fun explore(
        game: TicTacToeGame,
        history: List<Pair<Player, Int>>,
        turn: Player,
        randomizer: FailFastRandomizer,
        coverage: Coverage,
    ) {
        val terminal = game.checkForWinner()
        if (terminal != GameResult.InProgress) {
            coverage.terminals++
            if (terminal == GameResult.Tie) coverage.draws++
            assertNotEquals(
                "X must never win against Impossible",
                GameResult.Won(Player.X),
                terminal,
            )
            return
        }

        when (turn) {
            // Human is X: branch over every legal free cell.
            Player.X -> {
                val board = game.board
                for (location in board.indices.filter { board[it] == Mark.Empty }) {
                    val childHistory = history + (Player.X to location)
                    explore(gameFrom(childHistory, randomizer), childHistory, Player.O, randomizer, coverage)
                }
            }

            // Computer is O: follow Impossible's single deterministic reply.
            Player.O -> {
                val before = game.board
                val selected = selectedIndex(game.getComputerMove(DifficultyLevel.Impossible), before)
                assertEquals("query must not mutate the board", before, game.board)
                val childHistory = history + (Player.O to selected)
                explore(gameFrom(childHistory, randomizer), childHistory, Player.X, randomizer, coverage)
            }
        }
    }

    private fun gameFrom(
        history: List<Pair<Player, Int>>,
        randomizer: FailFastRandomizer,
    ): TicTacToeGame {
        val game = TicTacToeGame(randomizer)
        for ((player, location) in history) {
            assertEquals("illegal replay move $player@$location", MoveResult.Applied, game.setMove(player, location))
        }
        return game
    }

    private fun selectedIndex(move: ComputerMoveResult, board: List<Mark>): Int {
        val selected = move as? ComputerMoveResult.Selected
            ?: throw AssertionError("Impossible returned $move instead of a Selected move")
        assertTrue("selected index out of range: ${selected.index}", selected.index in board.indices)
        assertEquals("selected cell must be empty", Mark.Empty, board[selected.index])
        return selected.index
    }

    private class FailFastRandomizer : MoveRandomizer {
        var calls = 0
            private set

        override fun nextInt(bound: Int): Int {
            calls++
            throw AssertionError("Impossible must not consult MoveRandomizer (bound=$bound)")
        }
    }

    private class Coverage {
        var terminals = 0
        var draws = 0
    }
}
