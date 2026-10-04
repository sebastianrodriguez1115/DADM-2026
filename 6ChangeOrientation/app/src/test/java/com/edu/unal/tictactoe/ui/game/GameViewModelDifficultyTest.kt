package com.edu.unal.tictactoe.ui.game

import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelDifficultyTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    private fun playMove(viewModel: GameViewModel, location: Int) {
        viewModel.onHumanMove(location)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
    }

    @Test
    fun aNewSessionStartsInExpertWithAnEditableEmptyBoard() {
        val viewModel = newViewModel()

        assertEquals(DifficultyLevel.Expert, viewModel.uiState.value.difficulty)
        assertTrue(viewModel.uiState.value.canChangeDifficulty)
    }

    @Test
    fun setDifficultyBeforeTheFirstMoveAcceptsPublishesAndDoesNotMoveOrScore() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)
        val before = viewModel.uiState.value

        assertTrue(viewModel.setDifficulty(DifficultyLevel.Impossible))

        val after = viewModel.uiState.value
        assertNotSame(before, after)
        assertEquals(DifficultyLevel.Impossible, after.difficulty)
        assertEquals(before.board, after.board)
        assertEquals(before.turn, after.turn)
        assertEquals(before.result, after.result)
        assertEquals(before.starter, after.starter)
        assertEquals(before.nextStarter, after.nextStarter)
        assertEquals(before.score, after.score)
        assertEquals(before.soundEnabled, after.soundEnabled)
        assertEquals(0, randomizer.calls)
    }

    @Test
    fun setDifficultyAcceptsTheCurrentLevelWhenEditablePublishingTheSameValues() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)
        val before = viewModel.uiState.value

        assertTrue(viewModel.setDifficulty(DifficultyLevel.Expert))

        val after = viewModel.uiState.value
        assertNotSame(before, after)
        assertEquals(DifficultyLevel.Expert, after.difficulty)
        assertEquals(before.board, after.board)
        assertEquals(before.score, after.score)
        assertEquals(before.soundEnabled, after.soundEnabled)
        assertEquals(0, randomizer.calls)
    }

    @Test
    fun setDifficultyAfterTerminalKeepsBoardResultAndScore() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)
        playFirstEmptyUntilTerminal(viewModel)
        val terminal = viewModel.uiState.value
        assertTrue(terminal.canChangeDifficulty)
        val terminalBoard = terminal.board.toList()
        val callsBefore = randomizer.calls

        assertTrue(viewModel.setDifficulty(DifficultyLevel.Easy))

        val after = viewModel.uiState.value
        assertEquals(DifficultyLevel.Easy, after.difficulty)
        assertEquals(terminalBoard, after.board)
        assertEquals(terminal.result, after.result)
        assertEquals(terminal.score, after.score)
        assertEquals(terminal.starter, after.starter)
        assertEquals(terminal.nextStarter, after.nextStarter)
        assertEquals(terminal.soundEnabled, after.soundEnabled)
        assertEquals(callsBefore, randomizer.calls)
        assertEquals(terminalBoard, terminal.board)
    }

    @Test
    fun setDifficultyDuringAnActiveGameIsRejectedWithoutPublishingOrMoving() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)
        playMove(viewModel, 0)
        val active = viewModel.uiState.value
        val callsAfterMove = randomizer.calls
        assertFalse(active.canChangeDifficulty)

        assertFalse(viewModel.setDifficulty(DifficultyLevel.Impossible))
        assertSame(active, viewModel.uiState.value)
        assertEquals(DifficultyLevel.Expert, viewModel.uiState.value.difficulty)

        assertFalse(viewModel.setDifficulty(DifficultyLevel.Expert))
        assertSame(active, viewModel.uiState.value)
        assertEquals(active.score, viewModel.uiState.value.score)
        assertEquals(callsAfterMove, randomizer.calls)
    }

    @Test
    fun aStaleRequestCapturedWhileEditableIsRejectedOnceTheGameIsActive() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)
        val staleEditableState = viewModel.uiState.value
        assertTrue(staleEditableState.canChangeDifficulty)

        playMove(viewModel, 0)
        val active = viewModel.uiState.value

        assertFalse(viewModel.setDifficulty(DifficultyLevel.Easy))
        assertSame(active, viewModel.uiState.value)
        assertEquals(DifficultyLevel.Expert, active.difficulty)
        assertEquals(List(9) { Mark.Empty }, staleEditableState.board)
    }

    @Test
    fun newGameConservesDifficultyAndScoreAndLocksDifficultyOnlyForTheOOpening() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)
        playFirstEmptyUntilTerminal(viewModel)
        val terminalScore = viewModel.uiState.value.score
        assertTrue(viewModel.setDifficulty(DifficultyLevel.Impossible))
        val callsBeforeNewGame = randomizer.calls

        viewModel.newGame()

        val openingOState = viewModel.uiState.value
        assertEquals(DifficultyLevel.Impossible, openingOState.difficulty)
        assertEquals(terminalScore, openingOState.score)
        assertEquals(Player.O, openingOState.starter)
        assertEquals(Player.X, openingOState.nextStarter)
        assertEquals(List(9) { Mark.Empty }, openingOState.board)
        assertTrue(openingOState.canChangeDifficulty)
        assertEquals(callsBeforeNewGame, randomizer.calls)

        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(listOf(Mark.O) + List(8) { Mark.Empty }, viewModel.uiState.value.board)
        assertFalse(viewModel.uiState.value.canChangeDifficulty)

        viewModel.newGame()

        val openingXState = viewModel.uiState.value
        assertEquals(DifficultyLevel.Impossible, openingXState.difficulty)
        assertEquals(true, openingXState.soundEnabled)
        assertEquals(terminalScore, openingXState.score)
        assertEquals(Player.X, openingXState.starter)
        assertEquals(Player.O, openingXState.nextStarter)
        assertEquals(List(9) { Mark.Empty }, openingXState.board)
        assertTrue(openingXState.canChangeDifficulty)
    }

    @Test
    fun openingMoveUsesTheCanonicalDifficultyAndAvoidsTheRandomizerForImpossible() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)
        assertTrue(viewModel.setDifficulty(DifficultyLevel.Impossible))

        viewModel.newGame()

        val state = viewModel.uiState.value
        assertEquals(Player.O, state.starter)
        assertEquals(List(9) { Mark.Empty }, state.board)
        assertEquals(DifficultyLevel.Impossible, state.difficulty)
        assertEquals(0, randomizer.calls)
        assertTrue(state.canChangeDifficulty)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(listOf(Mark.O) + List(8) { Mark.Empty }, viewModel.uiState.value.board)
        assertEquals(DifficultyLevel.Impossible, viewModel.uiState.value.difficulty)
        assertEquals(0, randomizer.calls)
        assertFalse(viewModel.uiState.value.canChangeDifficulty)
    }

    @Test
    fun staleEasySelectionIsDiscardedAfterReentrantDifficultyChange() {
        lateinit var viewModel: GameViewModel
        var changedDifficulty = false
        val randomizer = object : MoveRandomizer {
            var calls = 0
            override fun nextInt(bound: Int): Int {
                calls++
                if (!changedDifficulty) {
                    changedDifficulty = true
                    assertTrue(viewModel.setDifficulty(DifficultyLevel.Impossible))
                }
                return bound - 1
            }
        }
        viewModel = harness.createViewModelResumed(TicTacToeGame(randomizer))
        assertTrue(viewModel.setDifficulty(DifficultyLevel.Easy))
        viewModel.newGame()

        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()

        assertEquals(DifficultyLevel.Impossible, viewModel.uiState.value.difficulty)
        assertEquals(listOf(Mark.O) + List(8) { Mark.Empty }, viewModel.uiState.value.board)
        assertEquals(Score(), viewModel.uiState.value.score)
        assertEquals(1, randomizer.calls)
        assertFalse(viewModel.uiState.value.canChangeDifficulty)
        val afterOpening = viewModel.uiState.value
        assertFalse(viewModel.setDifficulty(DifficultyLevel.Easy))
        assertSame(afterOpening, viewModel.uiState.value)
    }

    @Test
    fun difficultyChangedAt400MillisecondsDoesNotRestartOpeningDeadline() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)
        assertTrue(viewModel.setDifficulty(DifficultyLevel.Easy))
        viewModel.newGame()

        harness.scheduler.advanceTimeBy(400)
        harness.scheduler.runCurrent()
        assertTrue(viewModel.setDifficulty(DifficultyLevel.Impossible))
        harness.scheduler.advanceTimeBy(599)
        harness.scheduler.runCurrent()
        assertEquals(List(9) { Mark.Empty }, viewModel.uiState.value.board)
        assertEquals(0, randomizer.calls)

        harness.scheduler.advanceTimeBy(1)
        harness.scheduler.runCurrent()
        assertEquals(listOf(Mark.O) + List(8) { Mark.Empty }, viewModel.uiState.value.board)
        assertEquals(1_000, harness.scheduler.currentTime)
        assertEquals(0, randomizer.calls)
        assertFalse(viewModel.setDifficulty(DifficultyLevel.Easy))
    }

    @Test
    fun reentrantNewGamesDiscardTheOldOpeningSelectionAndKeepTheNewOpeningDeadline() {
        lateinit var viewModel: GameViewModel
        var restarted = false
        val randomizer = object : MoveRandomizer {
            override fun nextInt(bound: Int): Int {
                if (!restarted) {
                    restarted = true
                    viewModel.newGame() // X opening
                    viewModel.newGame() // new O opening, with its own full second
                }
                return 0
            }
        }
        viewModel = harness.createViewModelResumed(TicTacToeGame(randomizer))
        viewModel.newGame()
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()

        val restartedOpening = viewModel.uiState.value
        assertEquals(Player.O, restartedOpening.starter)
        assertEquals(List(9) { Mark.Empty }, restartedOpening.board)
        assertEquals(Score(), restartedOpening.score)
        harness.scheduler.advanceTimeBy(999)
        harness.scheduler.runCurrent()
        assertEquals(List(9) { Mark.Empty }, viewModel.uiState.value.board)
        harness.scheduler.advanceTimeBy(1)
        harness.scheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.board.count { it == Mark.O })
        assertEquals(Score(), viewModel.uiState.value.score)
    }

    @Test
    fun easyDifficultyIsForwardedToTheEngineOnEveryComputerTurn() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)
        assertTrue(viewModel.setDifficulty(DifficultyLevel.Easy))

        playMove(viewModel, 0)
        assertEquals(DifficultyLevel.Easy, viewModel.uiState.value.difficulty)
        playMove(viewModel, 2)
        assertEquals(DifficultyLevel.Easy, viewModel.uiState.value.difficulty)
        playMove(viewModel, 4)

        // Tras la tercera jugada humana X amenaza la linea 0-4-8: Expert bloquearia
        // sin consultar el randomizador, por lo que este caso discrimina el nivel.
        val state = viewModel.uiState.value
        assertEquals(3, randomizer.calls)
        assertEquals(DifficultyLevel.Easy, state.difficulty)
        assertEquals(Mark.O, state.board[5])
        assertEquals(Mark.Empty, state.board[8])
        assertEquals(GameResult.InProgress, state.result)
    }

    @Test
    fun difficultyIsPreservedAcrossTurnsAndTheTerminalPublication() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)
        assertTrue(viewModel.setDifficulty(DifficultyLevel.Easy))
        val initial = viewModel.uiState.value

        playMove(viewModel, 0)
        assertEquals(DifficultyLevel.Easy, viewModel.uiState.value.difficulty)
        playMove(viewModel, 2)
        assertEquals(DifficultyLevel.Easy, viewModel.uiState.value.difficulty)
        playMove(viewModel, 4)

        val beforeWinningMove = viewModel.uiState.value
        assertEquals(GameResult.InProgress, beforeWinningMove.result)
        assertEquals(DifficultyLevel.Easy, beforeWinningMove.difficulty)
        assertEquals(3, randomizer.calls)

        playMove(viewModel, 8)

        val terminal = viewModel.uiState.value
        assertEquals(DifficultyLevel.Easy, terminal.difficulty)
        assertEquals(GameResult.Won(Player.X), terminal.result)
        assertEquals(Score(xWins = 1), terminal.score)
        assertEquals(3, randomizer.calls)

        assertEquals(List(9) { Mark.Empty }, initial.board)
        assertEquals(Score(), initial.score)

        assertTrue(viewModel.setDifficulty(DifficultyLevel.Impossible))
        assertEquals(Score(xWins = 1), viewModel.uiState.value.score)
        assertEquals(DifficultyLevel.Impossible, viewModel.uiState.value.difficulty)
    }

    @Test
    fun selectingImpossibleDoesNotLeakIntoANewViewModelSession() {
        val configured = newViewModel(RecordingRandomizer(selectedIndex = 0))
        assertTrue(configured.setDifficulty(DifficultyLevel.Impossible))
        assertEquals(DifficultyLevel.Impossible, configured.uiState.value.difficulty)

        val freshSession = newViewModel()

        assertEquals(DifficultyLevel.Expert, freshSession.uiState.value.difficulty)
        assertTrue(freshSession.uiState.value.canChangeDifficulty)
        assertEquals(List(9) { Mark.Empty }, freshSession.uiState.value.board)
        assertEquals(Score(), freshSession.uiState.value.score)
    }

    @Test
    fun difficultyGateFollowsTheDerivedStateThroughPlayAndTerminal() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)
        assertTrue(viewModel.uiState.value.canChangeDifficulty)

        playMove(viewModel, 0)
        assertFalse(viewModel.uiState.value.canChangeDifficulty)

        playFirstEmptyUntilTerminal(viewModel)
        assertTrue(viewModel.uiState.value.canChangeDifficulty)
    }

    private fun playFirstEmptyUntilTerminal(viewModel: GameViewModel) {
        var humanMoves = 0
        while (viewModel.uiState.value.result == GameResult.InProgress && humanMoves < 5) {
            val location = viewModel.uiState.value.board.indexOfFirst { it == Mark.Empty }
            if (location < 0) {
                break
            }
            playMove(viewModel, location)
            humanMoves++
        }
        assertFalse(
            "The helper must reach a terminal state",
            viewModel.uiState.value.result == GameResult.InProgress,
        )
    }

    private fun newViewModel(
        randomizer: MoveRandomizer = RecordingRandomizer(selectedIndex = 0),
    ): GameViewModel = harness.createViewModelResumed(TicTacToeGame(randomizer))

    private class RecordingRandomizer(
        private val selectedIndex: Int,
    ) : MoveRandomizer {
        var calls: Int = 0
            private set

        override fun nextInt(bound: Int): Int {
            calls++
            return selectedIndex
        }
    }
}
