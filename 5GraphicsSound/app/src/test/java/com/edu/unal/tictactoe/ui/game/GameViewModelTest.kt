package com.edu.unal.tictactoe.ui.game

import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.MoveResult
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    private fun playMove(viewModel: GameViewModel, location: Int) {
        viewModel.onHumanMove(location)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
    }

    @Test
    fun initialStateStartsWithAnEmptyBoardAndZeroScore() {
        val viewModel = harness.createViewModelResumed()

        val state = viewModel.uiState.value

        assertEquals(List(9) { Mark.Empty }, state.board)
        assertEquals(Turn.Human, state.turn)
        assertEquals(GameResult.InProgress, state.result)
        assertEquals(Player.X, state.starter)
        assertEquals(Player.O, state.nextStarter)
        assertEquals(Score(), state.score)
        assertEquals(true, state.soundEnabled)
    }

    @Test
    fun repeatedStateReadsReturnTheSameSnapshotWithoutStartingOrScoring() {
        val viewModel = harness.createViewModelResumed()

        val firstSnapshot = viewModel.uiState.value
        repeat(3) {
            assertSame(firstSnapshot, viewModel.uiState.value)
        }

        assertEquals(List(9) { Mark.Empty }, viewModel.uiState.value.board)
        assertEquals(Score(), viewModel.uiState.value.score)
        assertEquals(GameResult.InProgress, viewModel.uiState.value.result)
    }

    @Test
    fun newGamesAlternateOpenersAndRepeatedReadsDoNotStartAnotherGame() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)

        val firstState = viewModel.uiState.value
        assertEquals(Player.X, firstState.starter)
        assertEquals(Player.O, firstState.nextStarter)
        assertEquals(List(9) { Mark.Empty }, firstState.board)
        assertEquals(0, randomizer.calls)

        repeat(3) {
            assertSame(firstState, viewModel.uiState.value)
        }

        viewModel.newGame()

        val secondState = viewModel.uiState.value
        assertEquals(Player.O, secondState.starter)
        assertEquals(Player.X, secondState.nextStarter)
        assertEquals(List(9) { Mark.Empty }, secondState.board)
        assertEquals(Turn.Computer, secondState.turn)
        assertEquals(GameResult.InProgress, secondState.result)
        assertEquals(Score(), secondState.score)
        assertEquals(true, secondState.soundEnabled)
        assertEquals(0, randomizer.calls)
        repeat(3) { assertSame(secondState, viewModel.uiState.value) }
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        val openingState = viewModel.uiState.value
        assertEquals(listOf(Mark.O) + List(8) { Mark.Empty }, openingState.board)
        assertEquals(Turn.Human, openingState.turn)
        assertEquals(1, randomizer.calls)
        repeat(3) { assertSame(openingState, viewModel.uiState.value) }

        viewModel.newGame()

        val thirdState = viewModel.uiState.value
        assertEquals(Player.X, thirdState.starter)
        assertEquals(Player.O, thirdState.nextStarter)
        assertEquals(List(9) { Mark.Empty }, thirdState.board)
        assertEquals(Turn.Human, thirdState.turn)
        assertEquals(GameResult.InProgress, thirdState.result)
        assertEquals(Score(), thirdState.score)
        assertEquals(true, thirdState.soundEnabled)
        assertEquals(1, randomizer.calls)
        repeat(3) {
            assertSame(thirdState, viewModel.uiState.value)
        }
    }

    @Test
    fun stateFlowIsReadOnlyToConsumers() {
        val viewModel = harness.createViewModelResumed()

        assertFalse(viewModel.uiState is MutableStateFlow<*>)
    }

    @Test
    fun initializationClearsAnInjectedGameWithoutCallingTheRandomizer() {
        val randomizer = RecordingRandomizer()
        val game = TicTacToeGame(randomizer)
        assertEquals(MoveResult.Applied, game.setMove(Player.X, 0))
        assertEquals(MoveResult.Applied, game.setMove(Player.O, 1))

        val viewModel = GameViewModel(game)

        assertEquals(List(9) { Mark.Empty }, game.board)
        assertEquals(List(9) { Mark.Empty }, viewModel.uiState.value.board)
        assertEquals(0, randomizer.calls)
    }

    @Test
    fun legalHumanMovePlacesXAndExactlyOneComputerMoveReturnsToHuman() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)

        playMove(viewModel, 0)

        val state = viewModel.uiState.value
        assertEquals(Mark.X, state.board[0])
        assertEquals(Mark.O, state.board[1])
        assertEquals(Turn.Human, state.turn)
        assertEquals(GameResult.InProgress, state.result)
        assertEquals(Score(), state.score)
        assertEquals(true, state.soundEnabled)
        assertEquals(1, randomizer.calls)
    }

    @Test
    fun humanVictoryIsDetectedBeforeComputerCanMove() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)

        listOf(0, 3, 4, 8).forEach { playMove(viewModel, it) }

        val state = viewModel.uiState.value
        assertEquals(listOf(Mark.X, Mark.O, Mark.Empty, Mark.X, Mark.X, Mark.O, Mark.O, Mark.Empty, Mark.X), state.board)
        assertEquals(GameResult.Won(Player.X), state.result)
        assertEquals(Turn.Human, state.turn)
        assertEquals(Score(xWins = 1), state.score)
        assertEquals(1, randomizer.calls)

        assertTerminalStateIsStable(viewModel, Score(xWins = 1))
        assertEquals(1, randomizer.calls)
    }

    @Test
    fun humanFillingTheLastCellWithNoWinProducesATieWithoutComputerMove() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)

        listOf(0, 4, 2, 7, 5).forEach { playMove(viewModel, it) }

        val state = viewModel.uiState.value
        assertEquals(
            listOf(Mark.X, Mark.O, Mark.X, Mark.O, Mark.X, Mark.X, Mark.O, Mark.X, Mark.O),
            state.board,
        )
        assertEquals(GameResult.Tie, state.result)
        assertEquals(Turn.Human, state.turn)
        assertEquals(Score(ties = 1), state.score)
        assertEquals(2, randomizer.calls)

        assertTerminalStateIsStable(viewModel, Score(ties = 1))
        assertEquals(2, randomizer.calls)
    }

    @Test
    fun computerVictoryLeavesComputerTurnAndDoesNotEnableHumanMoves() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)

        listOf(0, 2, 6, 5).forEach { playMove(viewModel, it) }

        val terminalState = viewModel.uiState.value
        assertEquals(
            listOf(Mark.X, Mark.O, Mark.X, Mark.O, Mark.O, Mark.X, Mark.X, Mark.O, Mark.Empty),
            terminalState.board,
        )
        assertEquals(GameResult.Won(Player.O), terminalState.result)
        assertEquals(Turn.Computer, terminalState.turn)
        assertEquals(Score(oWins = 1), terminalState.score)
        assertEquals(2, randomizer.calls)

        assertTerminalStateIsStable(viewModel, Score(oWins = 1))
        assertSame(terminalState, viewModel.uiState.value)
        assertEquals(2, randomizer.calls)
    }

    @Test
    fun invalidAndOccupiedLocationsAreExactNoOpsWithoutInvokingComputer() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)

        val initialState = viewModel.uiState.value
        listOf(-1, 9, Int.MIN_VALUE, Int.MAX_VALUE).forEach { playMove(viewModel, it) }
        assertSame(initialState, viewModel.uiState.value)
        assertEquals(0, randomizer.calls)

        playMove(viewModel, 0)
        val afterFirstMove = viewModel.uiState.value
        assertEquals(1, randomizer.calls)

        playMove(viewModel, 0)
        playMove(viewModel, 1)

        assertSame(afterFirstMove, viewModel.uiState.value)
        assertEquals(1, randomizer.calls)
    }

    @Test
    fun newGameAlternatesAfterAbandoningActiveXAndActiveOOpening() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)

        playMove(viewModel, 0)
        assertEquals(Player.X, viewModel.uiState.value.starter)
        assertEquals(Score(), viewModel.uiState.value.score)
        assertEquals(GameResult.InProgress, viewModel.uiState.value.result)

        viewModel.newGame()

        val abandonedOState = viewModel.uiState.value
        assertEquals(Player.O, abandonedOState.starter)
        assertEquals(Player.X, abandonedOState.nextStarter)
        assertEquals(List(9) { Mark.Empty }, abandonedOState.board)
        assertEquals(Turn.Computer, abandonedOState.turn)
        assertEquals(GameResult.InProgress, abandonedOState.result)
        assertEquals(Score(), abandonedOState.score)

        // A fresh game immediately invalidates the pending opening.
        viewModel.newGame()
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(List(9) { Mark.Empty }, viewModel.uiState.value.board)
        assertEquals(Turn.Human, viewModel.uiState.value.turn)
        playMove(viewModel, 4)
        assertEquals(GameResult.InProgress, viewModel.uiState.value.result)
        assertEquals(Score(), viewModel.uiState.value.score)

        viewModel.newGame()

        val nextOOpeningState = viewModel.uiState.value
        assertEquals(Player.O, nextOOpeningState.starter)
        assertEquals(Player.X, nextOOpeningState.nextStarter)
        assertEquals(List(9) { Mark.Empty }, nextOOpeningState.board)
        assertEquals(Turn.Computer, nextOOpeningState.turn)
        assertEquals(GameResult.InProgress, nextOOpeningState.result)
        assertEquals(Score(), nextOOpeningState.score)
    }

    @Test
    fun newGamesPreserveScoresAcrossTerminalGamesAndKeepOldSnapshotsIntact() {
        val viewModel = newViewModel(RecordingRandomizer(selectedIndex = 0))

        listOf(0, 3, 4, 8).forEach { playMove(viewModel, it) }
        val xWinSnapshot = viewModel.uiState.value
        assertEquals(GameResult.Won(Player.X), xWinSnapshot.result)
        assertEquals(Score(xWins = 1), xWinSnapshot.score)

        viewModel.newGame()
        assertEquals(Player.O, viewModel.uiState.value.starter)
        assertEquals(List(9) { Mark.Empty }, viewModel.uiState.value.board)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(listOf(Mark.O) + List(8) { Mark.Empty }, viewModel.uiState.value.board)
        assertEquals(Score(xWins = 1), viewModel.uiState.value.score)

        listOf(1, 3, 5).forEach { playMove(viewModel, it) }
        val oWinSnapshot = viewModel.uiState.value
        assertEquals(
            listOf(Mark.O, Mark.X, Mark.O, Mark.X, Mark.O, Mark.X, Mark.O, Mark.Empty, Mark.Empty),
            oWinSnapshot.board,
        )
        assertEquals(GameResult.Won(Player.O), oWinSnapshot.result)
        assertEquals(Score(xWins = 1, oWins = 1), oWinSnapshot.score)

        viewModel.newGame()
        assertEquals(Player.X, viewModel.uiState.value.starter)
        assertEquals(List(9) { Mark.Empty }, viewModel.uiState.value.board)
        assertEquals(Score(xWins = 1, oWins = 1), viewModel.uiState.value.score)

        listOf(0, 4, 2, 7, 5).forEach { playMove(viewModel, it) }
        val firstTieSnapshot = viewModel.uiState.value
        assertEquals(GameResult.Tie, firstTieSnapshot.result)
        assertEquals(Score(xWins = 1, oWins = 1, ties = 1), firstTieSnapshot.score)

        viewModel.newGame()
        assertEquals(Player.O, viewModel.uiState.value.starter)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(listOf(Mark.O) + List(8) { Mark.Empty }, viewModel.uiState.value.board)
        assertEquals(Score(xWins = 1, oWins = 1, ties = 1), viewModel.uiState.value.score)

        listOf(4, 2, 3, 8).forEach { playMove(viewModel, it) }
        val secondTieSnapshot = viewModel.uiState.value
        assertEquals(
            listOf(Mark.O, Mark.O, Mark.X, Mark.X, Mark.X, Mark.O, Mark.O, Mark.O, Mark.X),
            secondTieSnapshot.board,
        )
        assertEquals(GameResult.Tie, secondTieSnapshot.result)
        assertEquals(Score(xWins = 1, oWins = 1, ties = 2), secondTieSnapshot.score)

        assertEquals(GameResult.Won(Player.X), xWinSnapshot.result)
        assertEquals(
            listOf(Mark.X, Mark.O, Mark.Empty, Mark.X, Mark.X, Mark.O, Mark.O, Mark.Empty, Mark.X),
            xWinSnapshot.board,
        )
        assertEquals(Score(xWins = 1), xWinSnapshot.score)
        assertEquals(GameResult.Won(Player.O), oWinSnapshot.result)
        assertEquals(
            listOf(Mark.O, Mark.X, Mark.O, Mark.X, Mark.O, Mark.X, Mark.O, Mark.Empty, Mark.Empty),
            oWinSnapshot.board,
        )
        assertEquals(Score(xWins = 1, oWins = 1), oWinSnapshot.score)
        assertEquals(GameResult.Tie, firstTieSnapshot.result)
        assertEquals(
            listOf(Mark.X, Mark.O, Mark.X, Mark.O, Mark.X, Mark.X, Mark.O, Mark.X, Mark.O),
            firstTieSnapshot.board,
        )
        assertEquals(Score(xWins = 1, oWins = 1, ties = 1), firstTieSnapshot.score)
    }

    @Test
    fun independentViewModelStartsWithAZeroScoreAndX() {
        val firstViewModel = newViewModel(RecordingRandomizer(selectedIndex = 0))
        playMove(firstViewModel, 0)

        val secondViewModel = harness.createViewModelResumed()
        assertEquals(Player.X, secondViewModel.uiState.value.starter)
        assertEquals(Player.O, secondViewModel.uiState.value.nextStarter)
        assertEquals(List(9) { Mark.Empty }, secondViewModel.uiState.value.board)
        assertEquals(Score(), secondViewModel.uiState.value.score)
    }

    @Test
    fun incompleteGamesDoNotUpdateAnyScore() {
        val viewModel = newViewModel(RecordingRandomizer(selectedIndex = 0))

        playMove(viewModel, 0)
        assertEquals(Score(), viewModel.uiState.value.score)

        playMove(viewModel, 3)
        assertEquals(GameResult.InProgress, viewModel.uiState.value.result)
        assertEquals(Score(), viewModel.uiState.value.score)
    }

    @Test
    fun humanInputDuringComputerTurnIsRejectedReentrantlyBeforeComputerMove() {
        lateinit var viewModel: GameViewModel
        var turnDuringComputerSelection: Turn? = null
        var stateBeforeRejectedInput: GameUiState? = null
        var stateAfterRejectedInput: GameUiState? = null
        val randomizer = RecordingRandomizer(selectedIndex = 0) {
            turnDuringComputerSelection = viewModel.uiState.value.turn
            stateBeforeRejectedInput = viewModel.uiState.value
            viewModel.onHumanMove(2)
            stateAfterRejectedInput = viewModel.uiState.value
        }
        viewModel = newViewModel(randomizer)

        playMove(viewModel, 0)

        assertEquals(Turn.Computer, turnDuringComputerSelection)
        assertSame(stateBeforeRejectedInput, stateAfterRejectedInput)
        assertEquals(Mark.X, viewModel.uiState.value.board[0])
        assertEquals(Mark.O, viewModel.uiState.value.board[1])
        assertEquals(Turn.Human, viewModel.uiState.value.turn)
        assertEquals(1, randomizer.calls)
    }

    private fun newViewModel(randomizer: RecordingRandomizer): GameViewModel =
        harness.createViewModelResumed(TicTacToeGame(randomizer))

    private fun assertTerminalStateIsStable(viewModel: GameViewModel, expectedScore: Score) {
        val terminalState = viewModel.uiState.value
        val locations = listOf(Int.MIN_VALUE, -1) + (0..8).toList() + listOf(9, Int.MAX_VALUE)

        repeat(3) {
            assertSame(terminalState, viewModel.uiState.value)
            assertEquals(expectedScore, viewModel.uiState.value.score)
        }
        locations.forEach { location ->
            playMove(viewModel, location)
            assertSame(terminalState, viewModel.uiState.value)
            assertEquals(expectedScore, viewModel.uiState.value.score)
        }
    }

    private class RecordingRandomizer(
        private val selectedIndex: Int = 0,
        private val onCall: (() -> Unit)? = null,
    ) : MoveRandomizer {
        var calls: Int = 0
            private set

        override fun nextInt(bound: Int): Int {
            calls++
            onCall?.invoke()
            return selectedIndex
        }
    }
}
