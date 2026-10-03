package com.edu.unal.tictactoe.ui.game

import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelSoundTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    private fun playMove(viewModel: GameViewModel, location: Int) {
        viewModel.onHumanMove(location)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
    }

    @Test
    fun mutedPreferenceSurvivesAcceptedComputerAndTerminalHumanMoves() {
        val randomizer = CountingRandomizer()
        val viewModel = harness.createViewModelResumed(TicTacToeGame(randomizer))
        viewModel.toggleSound()

        listOf(0, 3, 4, 8).forEach { playMove(viewModel, it) }

        val terminal = viewModel.uiState.value
        assertEquals(com.edu.unal.tictactoe.game.models.GameResult.Won(Player.X), terminal.result)
        assertEquals(1, terminal.score.xWins)
        assertFalse(terminal.soundEnabled)
        assertEquals(1, randomizer.calls)
    }

    @Test
    fun soundTogglesDuringOpeningWaitPreserveStateAndOriginalDeadline() {
        val randomizer = CountingRandomizer()
        val viewModel = harness.createViewModelResumed(TicTacToeGame(randomizer))
        viewModel.newGame()
        val opening = viewModel.uiState.value
        harness.scheduler.advanceTimeBy(400)
        harness.scheduler.runCurrent()

        viewModel.toggleSound()
        val muted = viewModel.uiState.value
        assertFalse(muted.soundEnabled)
        assertEquals(opening.board, muted.board)
        assertEquals(opening.turn, muted.turn)
        assertEquals(opening.result, muted.result)
        assertEquals(opening.score, muted.score)
        viewModel.toggleSound()
        assertTrue(viewModel.uiState.value.soundEnabled)
        assertEquals(muted.board, viewModel.uiState.value.board)
        assertEquals(muted.score, viewModel.uiState.value.score)

        harness.scheduler.advanceTimeBy(599)
        harness.scheduler.runCurrent()
        assertEquals(0, viewModel.uiState.value.board.count { it == Mark.O })
        assertEquals(0, randomizer.calls)
        harness.scheduler.advanceTimeBy(1)
        harness.scheduler.runCurrent()
        assertEquals(1_000, harness.scheduler.currentTime)
        assertEquals(1, viewModel.uiState.value.board.count { it == Mark.O })
        assertTrue(viewModel.uiState.value.soundEnabled)
        assertEquals(opening.score, viewModel.uiState.value.score)
        assertEquals(1, randomizer.calls)
    }

    @Test
    fun mutedPreferenceSurvivesPauseAndResumeDuringPendingResponseAndNewGame() {
        val viewModel = harness.createViewModelResumed(TicTacToeGame(CountingRandomizer()))
        viewModel.toggleSound()
        viewModel.onHumanMove(0)
        harness.scheduler.advanceTimeBy(400)
        harness.scheduler.runCurrent()
        viewModel.onHostPaused()
        val paused = viewModel.uiState.value
        assertFalse(paused.soundEnabled)

        harness.scheduler.advanceTimeBy(5_000)
        assertSame(paused, viewModel.uiState.value)
        viewModel.onHostResumed()
        harness.scheduler.advanceTimeBy(599)
        harness.scheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.board.count { it != Mark.Empty })
        assertFalse(viewModel.uiState.value.soundEnabled)
        harness.scheduler.advanceTimeBy(1)
        harness.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.soundEnabled)
        assertEquals(1, viewModel.uiState.value.board.count { it == Mark.O })
        assertEquals(1, viewModel.uiState.value.board.count { it == Mark.X })
        assertEquals(Turn.Human, viewModel.uiState.value.turn)

        viewModel.newGame()
        assertFalse(viewModel.uiState.value.soundEnabled)
        assertEquals(0, viewModel.uiState.value.board.count { it != Mark.Empty })
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.soundEnabled)
        assertEquals(1, viewModel.uiState.value.board.count { it == Mark.O })
    }

    @Test
    fun mutePersistsAcrossNewGamesMovesAndDifficultyChangesWithoutChangingOtherState() {
        val randomizer = CountingRandomizer()
        val viewModel = harness.createViewModelResumed(TicTacToeGame(randomizer))
        viewModel.toggleSound()
        val muted = viewModel.uiState.value
        assertFalse(muted.soundEnabled)

        // Difficulty is editable on the empty board; rejected during an active game.
        assertTrue(viewModel.setDifficulty(DifficultyLevel.Hard))
        assertFalse(viewModel.uiState.value.soundEnabled)
        playMove(viewModel, 0)
        val active = viewModel.uiState.value
        assertFalse(active.soundEnabled)
        assertEquals(Mark.X, active.board[0])
        assertEquals(1, randomizer.calls)
        assertFalse(viewModel.setDifficulty(DifficultyLevel.Expert))
        assertFalse(viewModel.uiState.value.soundEnabled)

        viewModel.newGame()
        assertFalse(viewModel.uiState.value.soundEnabled)
        assertEquals(Player.O, viewModel.uiState.value.starter)
        assertEquals(0, viewModel.uiState.value.board.count { it != Mark.Empty })
        assertEquals(1, randomizer.calls)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(2, randomizer.calls)
        viewModel.newGame()
        assertFalse(viewModel.uiState.value.soundEnabled)
        assertTrue(viewModel.setDifficulty(DifficultyLevel.Expert))
        assertFalse(viewModel.uiState.value.soundEnabled)

        val beforeUnmute = viewModel.uiState.value
        val callsBeforeUnmute = randomizer.calls
        viewModel.toggleSound()
        val unmuted = viewModel.uiState.value
        assertTrue(unmuted.soundEnabled)
        assertEquals(beforeUnmute.board, unmuted.board)
        assertEquals(beforeUnmute.result, unmuted.result)
        assertEquals(beforeUnmute.turn, unmuted.turn)
        assertEquals(beforeUnmute.score, unmuted.score)
        assertEquals(beforeUnmute.starter, unmuted.starter)
        assertEquals(beforeUnmute.nextStarter, unmuted.nextStarter)
        assertEquals(beforeUnmute.difficulty, unmuted.difficulty)
        assertEquals(callsBeforeUnmute, randomizer.calls)

        // Published snapshots are independent and a new ViewModel starts enabled.
        assertFalse(muted.soundEnabled)
        assertFalse(active.soundEnabled)
        assertFalse(beforeUnmute.soundEnabled)
        assertTrue(harness.createViewModelResumed().uiState.value.soundEnabled)
    }

    private class CountingRandomizer : MoveRandomizer {
        var calls = 0
            private set

        override fun nextInt(bound: Int): Int {
            calls++
            return (0 until bound).first()
        }
    }
}
