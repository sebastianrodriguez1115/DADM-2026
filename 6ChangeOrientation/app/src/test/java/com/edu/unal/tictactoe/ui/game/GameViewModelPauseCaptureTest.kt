package com.edu.unal.tictactoe.ui.game

import androidx.lifecycle.SavedStateHandle
import com.edu.unal.tictactoe.data.preferences.FakeGamePreferences
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.session.GameSessionSnapshot
import com.edu.unal.tictactoe.ui.game.session.GameSessionSnapshotCodec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Task 2.8: the pause is the only capture route and freezes the remaining time of O's turn. */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelPauseCaptureTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    private val handle = SavedStateHandle()
    private val viewModel = harness.createViewModelResumed(savedStateHandle = handle)

    private fun advance(milliseconds: Long) {
        harness.scheduler.advanceTimeBy(milliseconds)
        harness.scheduler.runCurrent()
    }

    private fun snapshotIn(from: SavedStateHandle = handle): GameSessionSnapshot {
        val snapshot = GameSessionSnapshotCodec.decode(from.get<Any>(GameSessionSnapshot.SAVED_STATE_KEY))
        assertNotNull("the handle holds a valid snapshot", snapshot)
        return snapshot!!
    }

    private fun remainingMillis(): Long? = snapshotIn().pendingComputerRemainingNanos?.let { it / 1_000_000L }

    private fun marks(vm: GameViewModel = viewModel) = vm.uiState.value.board.count { it != Mark.Empty }

    private fun startOpening() {
        viewModel.newGame() // nextStarter of a fresh session is O
        assertEquals(Turn.Computer, viewModel.uiState.value.turn)
        assertEquals(0, marks())
    }

    private fun startResponse() {
        viewModel.onHumanMove(4)
        assertEquals(Turn.Computer, viewModel.uiState.value.turn)
        assertEquals(1, marks())
    }

    // Criterion 1
    @Test
    fun pausingAResponseStoresTheRemainingAndNothingChangesInBackground() {
        startResponse()
        assertEquals(1_000L, remainingMillis())
        advance(400)

        viewModel.onHostPaused()
        assertEquals(600L, remainingMillis())
        val captured = snapshotIn()

        repeat(3) { viewModel.onHostPaused() }
        advance(5_000)
        assertEquals(captured, snapshotIn())
        assertEquals(600L, remainingMillis())
        assertEquals(1, marks())

        viewModel.onHostResumed()
        advance(599)
        assertEquals(1, marks())
        advance(1)
        assertEquals(2, marks())
        advance(10_000)
        assertEquals(2, marks())
    }

    // Criterion 2
    @Test
    fun twoSuccessivePausesNeitherDiscountTwiceNorLoseTime() {
        startResponse()
        advance(400)
        viewModel.onHostPaused()
        assertEquals(600L, remainingMillis())
        advance(3_000)
        viewModel.onHostResumed()
        advance(200)
        viewModel.onHostPaused()
        assertEquals(400L, remainingMillis())
        viewModel.onHostPaused()
        advance(3_000)
        assertEquals(400L, remainingMillis())
        assertEquals(1, marks())

        viewModel.onHostResumed()
        advance(399)
        assertEquals(1, marks())
        advance(1)
        assertEquals(2, marks())
    }

    // Criterion 3
    @Test
    fun pausingAnOpeningBehavesTheSame() {
        startOpening()
        assertEquals(1_000L, remainingMillis())
        advance(400)
        viewModel.onHostPaused()
        assertEquals(600L, remainingMillis())
        repeat(3) { viewModel.onHostPaused() }
        advance(5_000)
        assertEquals(600L, remainingMillis())
        assertEquals(0, marks())

        viewModel.onHostResumed()
        advance(599)
        assertEquals(0, marks())
        advance(1)
        assertEquals(1, marks())
        advance(10_000)
        assertEquals(1, marks())
    }

    @Test
    fun twoSuccessivePausesOnAnOpeningKeepTheRemaining() {
        startOpening()
        advance(400)
        viewModel.onHostPaused()
        viewModel.onHostResumed()
        advance(200)
        viewModel.onHostPaused()
        viewModel.onHostPaused()
        advance(2_000)
        assertEquals(400L, remainingMillis())
        assertEquals(0, marks())
    }

    // Criterion 4
    @Test
    fun pausingWithoutAPendingTurnKeepsAValidSnapshotWithoutPending() {
        viewModel.onHostPaused()
        val fresh = snapshotIn()
        assertNull(fresh.pendingComputerRemainingNanos)
        assertEquals(Turn.Human, fresh.turn)
        assertEquals(GameResult.InProgress, fresh.result)

        viewModel.onHostResumed()
        startResponse()
        advance(1_000)
        assertEquals(2, marks())
        viewModel.onHostPaused()
        val afterReply = snapshotIn()
        assertNull(afterReply.pendingComputerRemainingNanos)
        assertEquals(viewModel.uiState.value.board, afterReply.board)
        assertEquals(Turn.Human, afterReply.turn)
    }

    // Criterion 5
    @Test
    fun theCapturedValueRestoresATurnThatRunsOnceAfterExactlyThatRemaining() {
        startResponse()
        advance(400)
        viewModel.onHostPaused()
        advance(5_000)
        val captured = handle.get<Any>(GameSessionSnapshot.SAVED_STATE_KEY)

        val preferences = FakeGamePreferences()
        val restoredHandle = SavedStateHandle(mapOf(GameSessionSnapshot.SAVED_STATE_KEY to captured))
        val restored = harness.createViewModel(TicTacToeGame(), preferences = preferences, savedStateHandle = restoredHandle)
        assertEquals(1, marks(restored))
        advance(5_000)
        assertEquals(1, marks(restored))

        restored.onHostResumed()
        advance(599)
        assertEquals(1, marks(restored))
        advance(1)
        assertEquals(2, marks(restored))
        advance(10_000)
        assertEquals(2, marks(restored))
        assertNull(snapshotIn(restoredHandle).pendingComputerRemainingNanos)
        assertEquals(0, preferences.scoreWrites)
    }
}
