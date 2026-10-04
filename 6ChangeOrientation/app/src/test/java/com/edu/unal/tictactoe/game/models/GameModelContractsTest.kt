package com.edu.unal.tictactoe.game.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class GameModelContractsTest {
    @Test
    fun playerAndMarkSymbolsMatchTheGameContract() {
        assertEquals('X', Player.X.symbol)
        assertEquals('O', Player.O.symbol)
        assertEquals(' ', Mark.Empty.symbol)
        assertEquals('X', Mark.X.symbol)
        assertEquals('O', Mark.O.symbol)
    }

    @Test
    fun resultsKeepTheirAssociatedValuesAndRemainDistinct() {
        assertEquals(Player.O, GameResult.Won(Player.O).player)
        assertEquals(4, ComputerMoveResult.Selected(4).index)
        assertNotSame(GameResult.InProgress, GameResult.Tie)
        assertNotSame(MoveResult.GameAlreadyFinished, ComputerMoveResult.GameAlreadyFinished)
        assertNotSame(MoveResult.Applied, MoveResult.InvalidLocation)
        assertNotSame(MoveResult.Occupied, MoveResult.GameAlreadyFinished)
        assertNotSame(ComputerMoveResult.NoMoves, ComputerMoveResult.GameAlreadyFinished)
    }

    @Test
    fun scoreDefaultsToZeroAndCopyDoesNotMutateTheOriginal() {
        val defaultScore = Score()
        assertEquals(0, defaultScore.xWins)
        assertEquals(0, defaultScore.oWins)
        assertEquals(0, defaultScore.ties)

        val original = Score(xWins = 1, oWins = 2, ties = 3)
        val copy = original.copy(xWins = 4)
        assertEquals(1, original.xWins)
        assertEquals(4, copy.xWins)
        assertEquals(original.oWins, copy.oWins)
        assertEquals(original.ties, copy.ties)
    }

    @Test
    fun difficultyLevelsKeepTheirExactNamesAndStableOrder() {
        assertEquals(
            listOf("Easy", "Hard", "Expert", "Impossible"),
            DifficultyLevel.values().map { it.name },
        )
        assertEquals(
            listOf(
                DifficultyLevel.Easy,
                DifficultyLevel.Hard,
                DifficultyLevel.Expert,
                DifficultyLevel.Impossible,
            ),
            DifficultyLevel.values().toList(),
        )
    }
}
