package com.edu.unal.tictactoe.ui.game.models

import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameUiStateDifficultyTest {
    @Test
    fun defaultsToExpertDifficulty() {
        val state = GameUiState()

        assertEquals(DifficultyLevel.Expert, state.difficulty)
    }

    @Test
    fun explicitDifficultyIsExposedWithoutChangingTheOtherFields() {
        val state = GameUiState(
            turn = Turn.Computer,
            result = GameResult.Tie,
            starter = Player.O,
            nextStarter = Player.X,
            score = Score(ties = 2),
            difficulty = DifficultyLevel.Impossible,
        )

        assertEquals(DifficultyLevel.Impossible, state.difficulty)
        assertEquals(Turn.Computer, state.turn)
        assertEquals(GameResult.Tie, state.result)
        assertEquals(Player.O, state.starter)
        assertEquals(Player.X, state.nextStarter)
        assertEquals(Score(ties = 2), state.score)
    }

    @Test
    fun difficultyIsEditableOnAnInitialEmptyInProgressBoard() {
        val state = GameUiState()

        assertTrue(state.canChangeDifficulty)
    }

    @Test
    fun difficultyIsEditableAfterATerminalResultEvenWithAFullBoard() {
        val wonState = GameUiState(
            board = listOf(Mark.X, Mark.O, Mark.Empty, Mark.X, Mark.X, Mark.O, Mark.O, Mark.Empty, Mark.X),
            result = GameResult.Won(Player.X),
        )
        val tieState = GameUiState(
            board = listOf(Mark.X, Mark.O, Mark.X, Mark.X, Mark.O, Mark.O, Mark.O, Mark.X, Mark.X),
            result = GameResult.Tie,
        )

        assertTrue(wonState.canChangeDifficulty)
        assertTrue(tieState.canChangeDifficulty)
    }

    @Test
    fun difficultyIsLockedOnAnActiveBoardWithAtLeastOneMark() {
        val board = MutableList(9) { Mark.Empty }
        board[4] = Mark.X
        val state = GameUiState(board = board, result = GameResult.InProgress)

        assertFalse(state.canChangeDifficulty)
    }

    @Test
    fun addingDifficultyKeepsTheBoardSnapshotDefensiveAndImmutable() {
        val original = MutableList(9) { Mark.Empty }
        val state = GameUiState(board = original, difficulty = DifficultyLevel.Easy)

        original[0] = Mark.X

        assertEquals(Mark.Empty, state.board[0])
        assertEquals(DifficultyLevel.Easy, state.difficulty)
    }
}
