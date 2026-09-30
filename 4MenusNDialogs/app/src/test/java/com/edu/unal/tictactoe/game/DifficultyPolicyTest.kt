package com.edu.unal.tictactoe.game

import com.edu.unal.tictactoe.game.models.ComputerMoveResult
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.MoveResult
import com.edu.unal.tictactoe.game.models.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class DifficultyPolicyTest {
    @Test
    fun easyUsesTheRandomizerEvenWhenTacticsAreAvailable() {
        for (randomIndex in 0 until 5) {
            val randomizer = RecordingRandomizer(randomIndex)
            val game = TicTacToeGame(randomizer)
            place(game, Player.O, 0, 1)
            place(game, Player.X, 3, 4)
            val freeLocations = freeLocations(game)
            val before = game.board

            val result = game.getComputerMove(DifficultyLevel.Easy)

            assertEquals(ComputerMoveResult.Selected(freeLocations[randomIndex]), result)
            assertEquals(1, randomizer.calls)
            assertEquals(listOf(freeLocations.size), randomizer.bounds)
            assertEquals(before, game.board)
        }
    }

    @Test
    fun hardTakesItsImmediateWinBeforeConsultingTheRandomizer() {
        val randomizer = RecordingRandomizer()
        val game = TicTacToeGame(randomizer)
        place(game, Player.O, 0, 1)
        place(game, Player.X, 3, 4)
        val before = game.board

        assertEquals(ComputerMoveResult.Selected(2), game.getComputerMove(DifficultyLevel.Hard))
        assertEquals(0, randomizer.calls)
        assertEquals(before, game.board)
    }

    @Test
    fun hardDoesNotDeliberatelyBlockAndUsesTheRandomizerWhenItCannotWin() {
        for (randomIndex in 0 until 6) {
            val randomizer = RecordingRandomizer(randomIndex)
            val game = TicTacToeGame(randomizer)
            place(game, Player.O, 4)
            place(game, Player.X, 1, 2)
            val freeLocations = freeLocations(game)
            val before = game.board

            val result = game.getComputerMove(DifficultyLevel.Hard)

            assertEquals(ComputerMoveResult.Selected(freeLocations[randomIndex]), result)
            assertEquals(1, randomizer.calls)
            assertEquals(listOf(freeLocations.size), randomizer.bounds)
            assertEquals(before, game.board)
        }
    }

    @Test
    fun easyAndHardUseTheRandomizerWhenNoTacticsExist() {
        for (difficulty in listOf(DifficultyLevel.Easy, DifficultyLevel.Hard)) {
            for (randomIndex in 0 until 7) {
                val randomizer = RecordingRandomizer(randomIndex)
                val game = TicTacToeGame(randomizer)
                place(game, Player.O, 0)
                place(game, Player.X, 4)
                val freeLocations = freeLocations(game)
                val before = game.board

                val result = game.getComputerMove(difficulty)

                assertEquals(ComputerMoveResult.Selected(freeLocations[randomIndex]), result)
                assertEquals(1, randomizer.calls)
                assertEquals(listOf(freeLocations.size), randomizer.bounds)
                assertEquals(before, game.board)
            }
        }
    }

    @Test
    fun everyDifficultyKeepsTheTerminalGuardsWithoutUsingTheRandomizer() {
        for (difficulty in DifficultyLevel.entries) {
            assertTerminalGuard(difficulty, ComputerMoveResult.GameAlreadyFinished) { game ->
                place(game, Player.O, 3, 4)
                place(game, Player.X, 0, 1, 2)
            }
            assertTerminalGuard(difficulty, ComputerMoveResult.GameAlreadyFinished) { game ->
                val firstEightMoves = listOf(
                    Player.X, Player.O, Player.O,
                    Player.O, Player.X, Player.X,
                    Player.X, Player.O,
                )
                for ((location, player) in firstEightMoves.withIndex()) {
                    place(game, player, location)
                }
                place(game, Player.X, 8)
            }
            assertTerminalGuard(difficulty, ComputerMoveResult.NoMoves) { game ->
                val tieMoves = listOf(
                    Player.X, Player.O, Player.X,
                    Player.X, Player.O, Player.O,
                    Player.O, Player.X, Player.X,
                )
                for ((location, player) in tieMoves.withIndex()) {
                    place(game, player, location)
                }
            }
        }
    }

    private fun assertTerminalGuard(
        difficulty: DifficultyLevel,
        expected: ComputerMoveResult,
        setup: (TicTacToeGame) -> Unit,
    ) {
        val randomizer = RecordingRandomizer()
        val game = TicTacToeGame(randomizer)
        setup(game)
        val before = game.board

        val result = game.getComputerMove(difficulty)

        assertEquals(expected, result)
        assertEquals(0, randomizer.calls)
        assertEquals(before, game.board)
    }

    private fun freeLocations(game: TicTacToeGame): List<Int> =
        game.board.indices.filter { game.board[it] == Mark.Empty }

    private fun place(game: TicTacToeGame, player: Player, vararg locations: Int) {
        for (location in locations) {
            assertEquals(MoveResult.Applied, game.setMove(player, location))
        }
    }

    private class RecordingRandomizer(
        private val selectedIndex: Int = 0,
    ) : MoveRandomizer {
        var calls: Int = 0
            private set
        val bounds = mutableListOf<Int>()

        override fun nextInt(bound: Int): Int {
            calls++
            bounds += bound
            return selectedIndex
        }
    }
}
