package com.edu.unal.tictactoe.ui.game.models

import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class GameUiStateTest {
    @Test
    fun defaultsRepresentTheFirstHumanStartedSession() {
        val state = GameUiState()

        assertEquals(List(9) { Mark.Empty }, state.board)
        assertEquals(Turn.Human, state.turn)
        assertSame(GameResult.InProgress, state.result)
        assertEquals(Player.X, state.starter)
        assertEquals(Player.O, state.nextStarter)
        assertEquals(Score(), state.score)
        assertEquals(true, state.soundEnabled)
    }

    @Test
    fun snapshotComposesTheDomainModels() {
        val board = listOf(Mark.X, Mark.O, Mark.Empty, Mark.Empty, Mark.X, Mark.O, Mark.Empty, Mark.Empty, Mark.Empty)
        val score = Score(xWins = 2, oWins = 1, ties = 3)
        val state = GameUiState(
            board = board,
            turn = Turn.Computer,
            result = GameResult.Won(Player.X),
            starter = Player.O,
            nextStarter = Player.X,
            score = score,
        )

        assertEquals(board, state.board)
        assertEquals(Turn.Computer, state.turn)
        assertEquals(GameResult.Won(Player.X), state.result)
        assertEquals(Player.O, state.starter)
        assertEquals(Player.X, state.nextStarter)
        assertEquals(score, state.score)
    }

    @Test
    fun originalBoardMutationDoesNotChangeTheSnapshot() {
        val original = MutableList(9) { Mark.Empty }
        val state = GameUiState(board = original)

        original[0] = Mark.X

        assertEquals(Mark.Empty, state.board[0])
    }

    @Test
    fun soundPreferenceCanBeInitializedDisabledAsAnImmutableValue() {
        val state = GameUiState(soundEnabled = false)

        assertEquals(false, state.soundEnabled)
        assertEquals(true, GameUiState().soundEnabled)
    }

    @Test
    fun exposedBoardCannotBeMutated() {
        val state = GameUiState()
        @Suppress("UNCHECKED_CAST")
        val exposed = state.board as MutableList<Mark>

        assertThrows(UnsupportedOperationException::class.java) {
            exposed[0] = Mark.X
        }
        assertEquals(Mark.Empty, state.board[0])
    }

    @Test
    fun boardMustContainExactlyNineMarks() {
        assertThrows(IllegalArgumentException::class.java) {
            GameUiState(board = List(8) { Mark.Empty })
        }
    }
}
