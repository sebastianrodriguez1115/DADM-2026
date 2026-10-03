package com.edu.unal.tictactoe.ui.game

import androidx.lifecycle.ViewModelStore
import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Turn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelLifecycleTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    @Test
    fun pausedInitiallyForBothResponseAndOpening() {
        val response = model()
        response.onHumanMove(0)
        advance(2_000)
        assertEquals(1, count(response.uiState.value.board))

        response.onHostResumed()
        advance(1_000)
        assertEquals(2, count(response.uiState.value.board))

        val opening = model()
        opening.newGame()
        advance(2_000)
        assertEquals(0, count(opening.uiState.value.board))

        opening.onHostResumed()
        advance(1_000)
        assertEquals(1, count(opening.uiState.value.board))
    }

    @Test
    fun pauseRetainsTimeAndRepeatedResumeDoesNotResetIt() {
        val viewModel = resumedWithResponse()
        advance(400)
        viewModel.onHostPaused()
        advance(5_000)
        assertEquals(1, count(viewModel.uiState.value.board))

        viewModel.onHostResumed()
        advance(599)
        assertEquals(1, count(viewModel.uiState.value.board))
        advance(1)
        assertEquals(2, count(viewModel.uiState.value.board))

        val repeatedResume = resumedWithResponse()
        advance(400)
        repeatedResume.onHostResumed()
        advance(600)
        assertEquals(2, count(repeatedResume.uiState.value.board))
    }

    @Test
    fun repeatedPauseDoesNotOverConsumeRemainingTime() {
        val viewModel = resumedWithResponse()
        advance(400)
        viewModel.onHostPaused()
        advance(2_000)
        viewModel.onHostPaused()
        advance(8_000)
        assertEquals(1, count(viewModel.uiState.value.board))

        viewModel.onHostResumed()
        advance(599)
        assertEquals(1, count(viewModel.uiState.value.board))
        advance(1)
        assertEquals(2, count(viewModel.uiState.value.board))
    }

    @Test
    fun zeroRemainingFiresOnceAndRemainsStableAcrossPauseAndResume() {
        val viewModel = resumedWithResponse()
        harness.scheduler.advanceTimeBy(1_000)
        viewModel.onHostPaused()
        harness.scheduler.runCurrent()
        assertEquals(1, count(viewModel.uiState.value.board))

        viewModel.onHostResumed()
        harness.scheduler.runCurrent()
        val completedState = viewModel.uiState.value
        assertEquals(2, count(completedState.board))

        advance(1_000)
        viewModel.onHostPaused()
        advance(2_000)
        viewModel.onHostResumed()
        advance(1_000)
        assertEquals(completedState, viewModel.uiState.value)
        assertEquals(2, count(viewModel.uiState.value.board))
    }

    @Test
    fun newGameWhilePausedInvalidatesPendingAndRotationUsesSameViewModel() {
        val viewModel = resumedWithResponse()
        advance(400)
        viewModel.onHostPaused()
        viewModel.newGame()
        viewModel.onHostResumed()
        advance(1_000)
        assertEquals(Turn.Human, viewModel.uiState.value.turn)
        assertEquals(1, count(viewModel.uiState.value.board))
        assertEquals(1, viewModel.uiState.value.board.count { it == Mark.O })

        viewModel.newGame()
        viewModel.newGame()
        advance(400)
        viewModel.onHostPaused()
        viewModel.onHostResumed()
        advance(600)
        assertEquals(1, count(viewModel.uiState.value.board))
        assertEquals(1, viewModel.uiState.value.board.count { it == Mark.O })
    }

    @Test
    fun clearingViewModelStoreCancelsQueuedDeadlineAndLeavesStateStable() {
        val viewModel = resumedWithResponse()
        advance(400)
        val stateBeforeClear = viewModel.uiState.value
        val store = ViewModelStore().apply { put("game", viewModel) }

        store.clear()
        advance(2_000)

        assertEquals(stateBeforeClear, viewModel.uiState.value)
        assertEquals(1, count(viewModel.uiState.value.board))
    }

    @Test
    fun pauseDuringSynchronousSelectionDiscardsItAndResumeCompletesExactlyOnce() {
        lateinit var viewModel: GameViewModel
        var pauseOnFirstSelection = true
        var randomizerCalls = 0
        val randomizer = MoveRandomizer { bound ->
            randomizerCalls++
            if (pauseOnFirstSelection) {
                pauseOnFirstSelection = false
                viewModel.onHostPaused()
            }
            bound - 1
        }
        viewModel = harness.createViewModelResumed(TicTacToeGame(randomizer))
        viewModel.newGame()
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()

        val pausedState = viewModel.uiState.value
        assertEquals(0, pausedState.board.count { it == Mark.O })
        assertEquals(Turn.Computer, pausedState.turn)
        assertEquals(1, randomizerCalls)
        harness.scheduler.advanceTimeBy(2_000)
        harness.scheduler.runCurrent()
        assertSame(pausedState, viewModel.uiState.value)
        assertEquals(1, randomizerCalls)

        viewModel.onHostResumed()
        harness.scheduler.runCurrent()
        assertEquals(2, randomizerCalls)
        assertEquals(1, viewModel.uiState.value.board.count { it == Mark.O })
        assertEquals(Turn.Human, viewModel.uiState.value.turn)
    }

    private fun resumedWithResponse(): GameViewModel = model().also {
        it.onHostResumed()
        it.onHumanMove(0)
    }

    private fun model() = GameViewModel(
        game = TicTacToeGame(MoveRandomizer { 0 }),
        monotonicClock = harness.clock,
        mainDispatcher = harness.dispatcher,
    )

    private fun advance(milliseconds: Long) {
        harness.scheduler.advanceTimeBy(milliseconds)
        harness.scheduler.runCurrent()
    }

    private fun count(board: List<Mark>) = board.count { it != Mark.Empty }
}
