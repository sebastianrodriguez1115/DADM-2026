package com.edu.unal.tictactoe.ui.game

import com.edu.unal.tictactoe.data.preferences.FakeGamePreferences
import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelPreferencesTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    @Test
    fun emptyStoreYieldsZeroScoreExpertAndTheUnchangedInitialSession() {
        val store = FakeGamePreferences()

        val state = harness.createViewModel(preferences = store).uiState.value

        assertEquals(Score(), state.score)
        assertEquals(DifficultyLevel.Expert, state.difficulty)
        assertInitialSession(state)
    }

    @Test
    fun seededStoreIsReflectedExactlyForEveryDifficultyWithoutAlteringTheSession() {
        DifficultyLevel.entries.forEach { level ->
            val store = FakeGamePreferences(Score(3, 5, 2), level)

            val state = harness.createViewModel(preferences = store).uiState.value

            assertEquals(Score(3, 5, 2), state.score)
            assertEquals(level, state.difficulty)
            assertInitialSession(state)
        }
    }

    @Test
    fun constructionAndNewGameReadTheStoreButNeverWriteToIt() {
        val store = FakeGamePreferences(Score(1, 2, 3), DifficultyLevel.Hard)
        val viewModel = harness.createViewModel(preferences = store)

        viewModel.newGame()
        viewModel.newGame()

        assertEquals(1, store.scoreReads)
        assertEquals(1, store.difficultyReads)
        assertEquals(0, store.scoreWrites)
        assertEquals(0, store.difficultyWrites)
    }

    @Test
    fun aSecondSessionOnTheSameStoreRestoresTheSameValues() {
        val store = FakeGamePreferences(Score(4, 0, 7), DifficultyLevel.Impossible)
        val first = harness.createViewModel(preferences = store)

        val second = harness.createViewModel(preferences = store)

        assertEquals(first.uiState.value.score, second.uiState.value.score)
        assertEquals(Score(4, 0, 7), second.uiState.value.score)
        assertEquals(DifficultyLevel.Impossible, second.uiState.value.difficulty)
    }

    @Test
    fun aStoreChangedBetweenSessionsIsLoadedByTheNewSessionOnly() {
        val store = FakeGamePreferences(Score(1, 1, 1), DifficultyLevel.Easy)
        val first = harness.createViewModel(preferences = store)

        store.writeScore(Score(9, 8, 7))
        store.writeDifficulty(DifficultyLevel.Hard)
        val second = harness.createViewModel(preferences = store)

        assertEquals(Score(9, 8, 7), second.uiState.value.score)
        assertEquals(DifficultyLevel.Hard, second.uiState.value.difficulty)
        assertEquals(Score(1, 1, 1), first.uiState.value.score)
        assertEquals(DifficultyLevel.Easy, first.uiState.value.difficulty)
    }

    @Test
    fun newGameKeepsTheLoadedScoreAndDifficulty() {
        val store = FakeGamePreferences(Score(3, 5, 2), DifficultyLevel.Impossible)
        val viewModel = harness.createViewModel(preferences = store)

        viewModel.newGame()

        assertEquals(Score(3, 5, 2), viewModel.uiState.value.score)
        assertEquals(DifficultyLevel.Impossible, viewModel.uiState.value.difficulty)
    }

    @Test
    fun theLoadedDifficultyDrivesTheComputerMove() {
        val easyRandomizer = CountingRandomizer()
        val easy = harness.createViewModelResumed(
            TicTacToeGame(easyRandomizer),
            FakeGamePreferences(difficulty = DifficultyLevel.Easy),
        )
        playHumanMoveAndLetComputerReply(easy)
        assertEquals(1, easyRandomizer.calls)

        val impossibleRandomizer = CountingRandomizer()
        val impossible = harness.createViewModelResumed(
            TicTacToeGame(impossibleRandomizer),
            FakeGamePreferences(difficulty = DifficultyLevel.Impossible),
        )
        playHumanMoveAndLetComputerReply(impossible)
        assertEquals(0, impossibleRandomizer.calls)
        assertEquals(2, impossible.uiState.value.board.count { it != Mark.Empty })
    }

    private fun playHumanMoveAndLetComputerReply(viewModel: GameViewModel) {
        viewModel.onHumanMove(0)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.board.count { it == Mark.O })
    }

    private fun assertInitialSession(state: GameUiState) {
        assertEquals(List(9) { Mark.Empty }, state.board)
        assertEquals(Turn.Human, state.turn)
        assertEquals(GameResult.InProgress, state.result)
        assertEquals(Player.X, state.starter)
        assertEquals(Player.O, state.nextStarter)
        assertTrue(state.soundEnabled)
    }

    private class CountingRandomizer : MoveRandomizer {
        var calls = 0
            private set

        override fun nextInt(bound: Int): Int {
            calls++
            return 0
        }
    }
}
