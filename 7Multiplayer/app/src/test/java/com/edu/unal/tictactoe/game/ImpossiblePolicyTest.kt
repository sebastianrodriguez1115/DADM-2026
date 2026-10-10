package com.edu.unal.tictactoe.game

import com.edu.unal.tictactoe.game.models.ComputerMoveResult
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.MoveResult
import com.edu.unal.tictactoe.game.models.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class ImpossiblePolicyTest {
    @Test
    fun impossibleEmptyBoardOpensAtTheLowestIndexForADraw() {
        val randomizer = FailFastRandomizer()
        val game = TicTacToeGame(randomizer)

        assertMoveAndUnchanged(game, randomizer, expectedLocation = 0)
    }

    @Test
    fun impossibleAgainstCenterOpeningTakesTheLowestCornerToHoldTheDraw() {
        val randomizer = FailFastRandomizer()
        val game = TicTacToeGame(randomizer)
        place(game, Player.X, 4)

        assertMoveAndUnchanged(game, randomizer, expectedLocation = 0)
    }

    @Test
    fun impossibleAgainstCornerOpeningTakesTheCenter() {
        val randomizer = FailFastRandomizer()
        val game = TicTacToeGame(randomizer)
        place(game, Player.X, 0)

        assertMoveAndUnchanged(game, randomizer, expectedLocation = 4)
    }

    @Test
    fun impossibleDefendsTheForkWithAnEdge() {
        val randomizer = FailFastRandomizer()
        val game = TicTacToeGame(randomizer)
        place(game, Player.X, 0)
        place(game, Player.O, 4)
        place(game, Player.X, 8)

        assertMoveAndUnchanged(game, randomizer, expectedLocation = 1)
    }

    @Test
    fun impossibleCompletesItsOwnWinningLine() {
        val randomizer = FailFastRandomizer()
        val game = TicTacToeGame(randomizer)
        place(game, Player.O, 0, 1)
        place(game, Player.X, 3, 4)

        assertMoveAndUnchanged(game, randomizer, expectedLocation = 2)
    }

    @Test
    fun impossibleOnAnAlreadyLostBoardReturnsTheOptimalMoveWithoutPromisingARescue() {
        val randomizer = FailFastRandomizer()
        val game = TicTacToeGame(randomizer)
        place(game, Player.X, 0)
        place(game, Player.O, 4)
        place(game, Player.X, 8)
        place(game, Player.O, 2)
        place(game, Player.X, 6)

        assertMoveAndUnchanged(game, randomizer, expectedLocation = 1)
    }

    private fun assertMoveAndUnchanged(
        game: TicTacToeGame,
        randomizer: FailFastRandomizer,
        expectedLocation: Int,
    ) {
        val snapshot = game.board
        val expectedBoard = snapshot.toList()

        repeat(3) { attempt ->
            val result = game.getComputerMove(DifficultyLevel.Impossible)

            assertEquals("attempt $attempt", ComputerMoveResult.Selected(expectedLocation), result)
            assertEquals("attempt $attempt", expectedBoard, game.board)
            assertEquals("attempt $attempt", expectedBoard, snapshot)
        }

        assertEquals(0, randomizer.calls)
        assertEquals(Mark.Empty, snapshot[expectedLocation])
    }

    private fun place(game: TicTacToeGame, player: Player, vararg locations: Int) {
        for (location in locations) {
            assertEquals(MoveResult.Applied, game.setMove(player, location))
        }
    }

    private class FailFastRandomizer : MoveRandomizer {
        var calls: Int = 0
            private set

        override fun nextInt(bound: Int): Int {
            calls++
            throw AssertionError("Impossible must not consult MoveRandomizer (bound=$bound)")
        }
    }
}
