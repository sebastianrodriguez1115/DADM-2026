package com.edu.unal.tictactoe.ui.game

import androidx.lifecycle.ViewModel
import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelCoroutineHarnessTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    @Test
    fun acceptedXPublishesComputerTurnAndWaitsWithoutAcceptingMoreHumanInput() {
        val randomizer = CountingRandomizer()
        val viewModel = harness.createViewModelResumed(TicTacToeGame(randomizer))

        viewModel.onHumanMove(0)
        val waiting = viewModel.uiState.value
        assertEquals(Mark.X, waiting.board[0])
        assertEquals(1, waiting.board.count { it != Mark.Empty })
        assertEquals(Turn.Computer, waiting.turn)
        assertEquals(0, randomizer.calls)

        viewModel.onHumanMove(2)
        assertEquals(waiting, viewModel.uiState.value)
        harness.scheduler.advanceTimeBy(999)
        harness.scheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.board.count { it != Mark.Empty })
        assertEquals(0, randomizer.calls)

        harness.scheduler.advanceTimeBy(1)
        harness.scheduler.runCurrent()
        assertEquals(2, viewModel.uiState.value.board.count { it != Mark.Empty })
        assertEquals(Mark.O, viewModel.uiState.value.board[1])
        assertEquals(Turn.Human, viewModel.uiState.value.turn)
        assertEquals(1, randomizer.calls)

        val afterResponse = viewModel.uiState.value
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(afterResponse, viewModel.uiState.value)
        assertEquals(1, randomizer.calls)
    }

    @Test
    fun terminalHumanWinAndTieNeverSelectOrScheduleAComputerMove() {
        val winRandomizer = CountingRandomizer()
        val winViewModel = harness.createViewModelResumed(TicTacToeGame(winRandomizer))
        winViewModel.setDifficulty(DifficultyLevel.Easy)
        listOf(0, 3, 4).forEach { playAndWait(winViewModel, it) }
        val winCallsBeforeTerminalX = winRandomizer.calls
        playAndWait(winViewModel, 8)
        assertEquals(GameResult.Won(Player.X), winViewModel.uiState.value.result)
        assertEquals(winCallsBeforeTerminalX, winRandomizer.calls)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(winCallsBeforeTerminalX, winRandomizer.calls)

        val tieRandomizer = CountingRandomizer()
        val tieViewModel = harness.createViewModelResumed(TicTacToeGame(tieRandomizer))
        listOf(0, 4, 2, 7, 5).forEach { playAndWait(tieViewModel, it) }
        val tieTerminal = tieViewModel.uiState.value
        assertEquals(GameResult.Tie, tieViewModel.uiState.value.result)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(tieTerminal, tieViewModel.uiState.value)
    }

    @Test
    fun startingANewGameCancelsThePendingResponseJob() {
        val randomizer = CountingRandomizer()
        val viewModel = harness.createViewModelResumed(TicTacToeGame(randomizer))
        viewModel.onHumanMove(0)
        assertEquals(0, randomizer.calls)

        viewModel.newGame()
        val newBoard = viewModel.uiState.value.board
        assertEquals(0, newBoard.count { it != Mark.Empty })
        assertEquals(Turn.Computer, viewModel.uiState.value.turn)
        assertEquals(0, randomizer.calls)

        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.board.count { it != Mark.Empty })
        assertEquals(1, randomizer.calls)
    }

    @Test
    fun OOpeningUsesSameOneSecondWaitAndIsCancelledByNewXGame() {
        val randomizer = CountingRandomizer()
        val viewModel = harness.createViewModelResumed(TicTacToeGame(randomizer))

        viewModel.newGame()
        val opening = viewModel.uiState.value
        assertEquals(List(9) { Mark.Empty }, opening.board)
        assertEquals(Turn.Computer, opening.turn)
        assertEquals(true, opening.canChangeDifficulty)
        assertEquals(0, randomizer.calls)

        harness.scheduler.advanceTimeBy(999)
        harness.scheduler.runCurrent()
        assertEquals(List(9) { Mark.Empty }, viewModel.uiState.value.board)
        assertEquals(0, randomizer.calls)
        harness.scheduler.advanceTimeBy(1)
        harness.scheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.board.count { it != Mark.Empty })
        assertEquals(Turn.Human, viewModel.uiState.value.turn)
        assertEquals(1, randomizer.calls)

        viewModel.newGame()
        assertEquals(Turn.Human, viewModel.uiState.value.turn)
        viewModel.newGame()
        val cancelledOpening = viewModel.uiState.value
        assertEquals(List(9) { Mark.Empty }, cancelledOpening.board)
        assertEquals(Turn.Computer, cancelledOpening.turn)
        viewModel.newGame()
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(List(9) { Mark.Empty }, viewModel.uiState.value.board)
        assertEquals(Turn.Human, viewModel.uiState.value.turn)
        assertEquals(1, randomizer.calls)
        assertEquals(opening.score, viewModel.uiState.value.score)
    }

    @Test
    fun newOpeningDoesNotRunAtDeadlineOfCancelledResponse() {
        val randomizer = CountingRandomizer()
        val viewModel = harness.createViewModelResumed(TicTacToeGame(randomizer))

        viewModel.onHumanMove(0)
        harness.scheduler.runCurrent()
        harness.scheduler.advanceTimeBy(400)
        harness.scheduler.runCurrent()
        viewModel.newGame()
        val openingScore = viewModel.uiState.value.score
        assertEquals(Turn.Computer, viewModel.uiState.value.turn)
        assertEquals(List(9) { Mark.Empty }, viewModel.uiState.value.board)

        harness.scheduler.advanceTimeBy(600)
        harness.scheduler.runCurrent()
        assertEquals(0, randomizer.calls)
        assertEquals(List(9) { Mark.Empty }, viewModel.uiState.value.board)
        assertEquals(openingScore, viewModel.uiState.value.score)

        harness.scheduler.advanceTimeBy(399)
        harness.scheduler.runCurrent()
        assertEquals(0, randomizer.calls)
        assertEquals(List(9) { Mark.Empty }, viewModel.uiState.value.board)

        harness.scheduler.advanceTimeBy(1)
        harness.scheduler.runCurrent()
        assertEquals(1, randomizer.calls)
        assertEquals(1, viewModel.uiState.value.board.count { it == Mark.O })
        assertEquals(0, viewModel.uiState.value.board.count { it == Mark.X })
        assertEquals(Turn.Human, viewModel.uiState.value.turn)
        assertEquals(openingScore, viewModel.uiState.value.score)
    }

    private fun playAndWait(viewModel: GameViewModel, location: Int) {
        viewModel.onHumanMove(location)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
    }

    private class CountingRandomizer : MoveRandomizer {
        var calls = 0
            private set

        override fun nextInt(bound: Int): Int {
            calls++
            return 0
        }
    }

    @Test
    fun clockTracksTheSharedSchedulerAcross999Plus1Milliseconds() {
        assertEquals(0L, harness.clock.nowNanos())
        harness.scheduler.advanceTimeBy(999)
        assertEquals(999_000_000L, harness.clock.nowNanos())
        harness.scheduler.advanceTimeBy(1)
        assertEquals(1_000_000_000L, harness.clock.nowNanos())
    }

    @Test
    fun earlySubMillisecondWakeIsRoundedUpAndNeverMovesBeforeMonotonicDeadline() {
        val randomizer = CountingRandomizer()
        var clockNanos = 0L
        val viewModel = GameViewModel(
            game = TicTacToeGame(randomizer),
            monotonicClock = MonotonicClock { clockNanos },
            mainDispatcher = harness.dispatcher,
        )
        viewModel.onHostResumed()
        viewModel.onHumanMove(0)

        clockNanos = 999_500_000L
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(0, randomizer.calls)
        assertEquals(1, viewModel.uiState.value.board.count { it != Mark.Empty })

        clockNanos = 1_000_000_000L
        harness.scheduler.advanceTimeBy(1)
        harness.scheduler.runCurrent()
        assertEquals(1, randomizer.calls)
        assertEquals(2, viewModel.uiState.value.board.count { it != Mark.Empty })
    }

    @Test
    fun coroutineDispatchDelayConsumesTimeFromAcceptanceBeforeSchedulingDelay() {
        val randomizer = CountingRandomizer()
        var clockNanos = 0L
        val viewModel = GameViewModel(
            game = TicTacToeGame(randomizer),
            monotonicClock = MonotonicClock { clockNanos },
            mainDispatcher = harness.dispatcher,
        )
        viewModel.onHostResumed()
        viewModel.onHumanMove(0)

        clockNanos = 400_000_000L
        harness.scheduler.runCurrent()
        harness.scheduler.advanceTimeBy(599)
        clockNanos = 999_000_000L
        harness.scheduler.runCurrent()
        assertEquals(0, randomizer.calls)

        clockNanos = 1_000_000_000L
        harness.scheduler.advanceTimeBy(1)
        harness.scheduler.runCurrent()
        assertEquals(1, randomizer.calls)
    }

    @Test
    fun newGameAtOldDeadlineCancelsCallbackNotYetDispatched() {
        val randomizer = CountingRandomizer()
        val viewModel = harness.createViewModelResumed(TicTacToeGame(randomizer))
        viewModel.onHumanMove(0)
        harness.scheduler.runCurrent()

        harness.scheduler.advanceTimeBy(1_000)
        viewModel.newGame()
        val newGameState = viewModel.uiState.value
        val callsAfterNewGame = randomizer.calls
        harness.scheduler.runCurrent()

        assertEquals(newGameState, viewModel.uiState.value)
        assertEquals(callsAfterNewGame, randomizer.calls)
    }

    @Test
    fun noArgumentViewModelConstructorRemainsUsableUnderMainRule() {
        val viewModel = GameViewModel()

        assertEquals(0, viewModel.uiState.value.board.count { it != Mark.Empty })
        assertEquals(ViewModel::class.java, viewModel.javaClass.superclass)
    }
}
