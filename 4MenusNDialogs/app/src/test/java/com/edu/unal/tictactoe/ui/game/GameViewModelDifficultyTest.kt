package com.edu.unal.tictactoe.ui.game

import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewModelDifficultyTest {
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
        assertEquals(callsBefore, randomizer.calls)
        assertEquals(terminalBoard, terminal.board)
    }

    @Test
    fun setDifficultyDuringAnActiveGameIsRejectedWithoutPublishingOrMoving() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)
        viewModel.onHumanMove(0)
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

        viewModel.onHumanMove(0)
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
        assertEquals(listOf(Mark.O) + List(8) { Mark.Empty }, openingOState.board)
        assertFalse(openingOState.canChangeDifficulty)
        assertEquals(callsBeforeNewGame, randomizer.calls)

        viewModel.newGame()

        val openingXState = viewModel.uiState.value
        assertEquals(DifficultyLevel.Impossible, openingXState.difficulty)
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
        assertEquals(listOf(Mark.O) + List(8) { Mark.Empty }, state.board)
        assertEquals(DifficultyLevel.Impossible, state.difficulty)
        assertEquals(0, randomizer.calls)
        assertFalse(state.canChangeDifficulty)
    }

    @Test
    fun easyDifficultyIsForwardedToTheEngineOnEveryComputerTurn() {
        val randomizer = RecordingRandomizer(selectedIndex = 0)
        val viewModel = newViewModel(randomizer)
        assertTrue(viewModel.setDifficulty(DifficultyLevel.Easy))

        viewModel.onHumanMove(0)
        assertEquals(DifficultyLevel.Easy, viewModel.uiState.value.difficulty)
        viewModel.onHumanMove(2)
        assertEquals(DifficultyLevel.Easy, viewModel.uiState.value.difficulty)
        viewModel.onHumanMove(4)

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

        viewModel.onHumanMove(0)
        assertEquals(DifficultyLevel.Easy, viewModel.uiState.value.difficulty)
        viewModel.onHumanMove(2)
        assertEquals(DifficultyLevel.Easy, viewModel.uiState.value.difficulty)
        viewModel.onHumanMove(4)

        val beforeWinningMove = viewModel.uiState.value
        assertEquals(GameResult.InProgress, beforeWinningMove.result)
        assertEquals(DifficultyLevel.Easy, beforeWinningMove.difficulty)
        assertEquals(3, randomizer.calls)

        viewModel.onHumanMove(8)

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

        viewModel.onHumanMove(0)
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
            viewModel.onHumanMove(location)
            humanMoves++
        }
        assertFalse(
            "The helper must reach a terminal state",
            viewModel.uiState.value.result == GameResult.InProgress,
        )
    }

    private fun newViewModel(
        randomizer: MoveRandomizer = RecordingRandomizer(selectedIndex = 0),
    ): GameViewModel = GameViewModel(TicTacToeGame(randomizer))

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
