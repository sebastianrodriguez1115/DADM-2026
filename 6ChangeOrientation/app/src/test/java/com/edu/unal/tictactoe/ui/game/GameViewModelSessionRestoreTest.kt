package com.edu.unal.tictactoe.ui.game

import androidx.lifecycle.SavedStateHandle
import com.edu.unal.tictactoe.data.preferences.FakeGamePreferences
import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundSink
import com.edu.unal.tictactoe.ui.game.session.GameSessionSnapshot
import com.edu.unal.tictactoe.ui.game.session.GameSessionSnapshotCodec
import com.edu.unal.tictactoe.ui.game.session.GameSessionTestEncodings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Construction-time restore and fallback of the session from a SavedStateHandle (tasks 2.3-2.4). */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelSessionRestoreTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    private val storedScore = Score(3, 5, 2)
    private val storedDifficulty = DifficultyLevel.Impossible
    private var randomizerCalls = 0
    private val randomizer = MoveRandomizer { randomizerCalls++; 0 }

    private fun store(
        score: Score = storedScore,
        difficulty: DifficultyLevel = storedDifficulty,
    ) = FakeGamePreferences(score, difficulty)

    private fun handleOf(value: Any?) = SavedStateHandle().apply {
        if (value != null) set(GameSessionSnapshot.SAVED_STATE_KEY, value)
    }

    private fun handleOf(snapshot: GameSessionSnapshot) = SavedStateHandle(
        mapOf(GameSessionSnapshot.SAVED_STATE_KEY to GameSessionSnapshotCodec.encode(snapshot)),
    )

    private fun advance(milliseconds: Long) {
        harness.scheduler.advanceTimeBy(milliseconds)
        harness.scheduler.runCurrent()
    }

    private fun marks(viewModel: GameViewModel) = viewModel.uiState.value.board.count { it != Mark.Empty }

    private fun assertFreshSession(viewModel: GameViewModel, game: TicTacToeGame, preferences: FakeGamePreferences) {
        val state = viewModel.uiState.value
        assertEquals(List(9) { Mark.Empty }, state.board)
        assertEquals(Turn.Human, state.turn)
        assertEquals(GameResult.InProgress, state.result)
        assertEquals(Player.X, state.starter)
        assertEquals(Player.O, state.nextStarter)
        assertTrue(state.soundEnabled)
        assertEquals(preferences.storedScore, state.score)
        assertEquals(preferences.storedDifficulty, state.difficulty)
        assertEquals(List(9) { Mark.Empty }, game.board)
    }

    private fun assertMatches(snapshot: GameSessionSnapshot, viewModel: GameViewModel, preferences: FakeGamePreferences) {
        val state = viewModel.uiState.value
        assertEquals(snapshot.board, state.board)
        assertEquals(snapshot.turn, state.turn)
        assertEquals(snapshot.result, state.result)
        assertEquals(snapshot.starter, state.starter)
        assertEquals(snapshot.nextStarter, state.nextStarter)
        assertEquals(snapshot.soundEnabled, state.soundEnabled)
        assertEquals(preferences.storedScore, state.score)
        assertEquals(preferences.storedDifficulty, state.difficulty)
    }

    // 2.3: every class of corruption falls back to a fresh session with current preferences.
    @Test
    fun everyCorruptionFallsBackToFreshSessionAndRewritesAValidSnapshot() {
        GameSessionTestEncodings.corruptions.forEach { (name, value) ->
            val preferences = store()
            val game = TicTacToeGame(randomizer)
            val handle = handleOf(value)
            val viewModel = harness.createViewModel(game, preferences = preferences, savedStateHandle = handle)

            assertFreshSession(viewModel, game, preferences)
            viewModel.onHostResumed()
            advance(5_000)
            assertEquals("$name: no pending turn appears", 0, marks(viewModel))
            assertEquals(name, 0, preferences.scoreWrites)
            assertEquals(name, 0, preferences.difficultyWrites)
            assertEquals(name, 0, randomizerCalls)
            val rewritten = GameSessionSnapshotCodec.decode(handle.get<Any>(GameSessionSnapshot.SAVED_STATE_KEY))
            assertNotNull("$name: handle holds a valid snapshot", rewritten)
            assertEquals(name, false, rewritten!!.pendingComputerRemainingNanos != null)
            assertEquals(name, Turn.Human, rewritten.turn)
        }
    }

    // 2.4 criterion 2
    @Test
    fun activeBoardWithHumanTurnIsRestoredWithoutEffects() {
        val snapshot = GameSessionTestEncodings.humanTurnStarterO
        val preferences = store()
        val game = TicTacToeGame(randomizer)
        val viewModel = harness.createViewModel(game, preferences = preferences, savedStateHandle = handleOf(snapshot))
        val sink = RecordingSink()
        val owner = Any()
        viewModel.registerMoveSoundSink(owner, sink)

        assertMatches(snapshot, viewModel, preferences)
        assertEquals(snapshot.board, game.board)
        viewModel.onHostResumed()
        advance(5_000)

        assertEquals(0, preferences.scoreWrites)
        assertEquals(0, preferences.difficultyWrites)
        assertEquals(0, randomizerCalls)
        assertEquals(0, sink.plays)
        assertEquals(0, sink.stops)
        assertMatches(snapshot, viewModel, preferences)

        viewModel.onHumanMove(0)
        assertEquals(snapshot.board, viewModel.uiState.value.board)
        viewModel.onHumanMove(2)
        assertEquals(Mark.X, viewModel.uiState.value.board[2])
        assertEquals(Mark.X, game.board[2])
    }

    // 2.4 criterion 3
    @Test
    fun terminalBoardsAreRestoredWithoutScoringAgain() {
        with(GameSessionTestEncodings) { listOf(xWin, oWin, tie, oWinStarterO, tieStarterO) }.forEach { snapshot ->
            val preferences = store()
            val viewModel = harness.createViewModel(
                TicTacToeGame(randomizer),
                preferences = preferences,
                savedStateHandle = handleOf(snapshot),
            )
            assertMatches(snapshot, viewModel, preferences)
            assertEquals(storedScore, viewModel.uiState.value.score)

            viewModel.onHostResumed()
            repeat(9) { viewModel.onHumanMove(it) }
            advance(5_000)
            assertEquals("$snapshot", 0, preferences.scoreWrites)
            assertEquals("$snapshot", storedScore, viewModel.uiState.value.score)
            assertMatches(snapshot, viewModel, preferences)

            viewModel.newGame()
            assertEquals(snapshot.nextStarter, viewModel.uiState.value.starter)
            assertEquals(0, preferences.scoreWrites)
        }
    }

    @Test
    fun gameAfterARestoredTerminalScoresExactlyOnce() {
        val preferences = store()
        val viewModel = harness.createViewModel(
            TicTacToeGame(randomizer),
            preferences = preferences,
            savedStateHandle = handleOf(GameSessionTestEncodings.xWin),
        )
        viewModel.onHostResumed()
        viewModel.newGame() // O opens (restored nextStarter), randomizer 0 plays first free cell
        advance(1_000)
        viewModel.onHumanMove(4)
        advance(1_000)
        viewModel.onHumanMove(8)
        advance(1_000)

        assertEquals(GameResult.Won(Player.O), viewModel.uiState.value.result)
        assertEquals(1, preferences.scoreWrites)
        assertEquals(storedScore.copy(oWins = storedScore.oWins + 1), preferences.storedScore)
        advance(5_000)
        assertEquals(1, preferences.scoreWrites)
    }

    // 2.4 criterion 4
    @Test
    fun restoredPendingTurnMovesOnceAfterRemainingElapsesInForeground() {
        listOf(GameSessionTestEncodings.emptyStarterOPendingMid, GameSessionTestEncodings.computerPending).forEach { snapshot ->
            val preferences = store(difficulty = DifficultyLevel.Easy)
            val before = snapshot.board.count { it != Mark.Empty }
            val viewModel = harness.createViewModel(
                TicTacToeGame(randomizer),
                preferences = preferences,
                savedStateHandle = handleOf(snapshot),
            )
            assertMatches(snapshot, viewModel, preferences)
            advance(5_000)
            assertEquals("not before the host is in foreground", before, marks(viewModel))
            assertEquals(0, randomizerCalls)

            viewModel.onHostResumed()
            advance(399)
            assertEquals("not before the saved remaining", before, marks(viewModel))
            advance(1)
            assertEquals(before + 1, marks(viewModel))
            assertEquals(Turn.Human, viewModel.uiState.value.turn)
            advance(10_000)
            assertEquals("exactly once", before + 1, marks(viewModel))
            assertEquals(1, randomizerCalls)
            randomizerCalls = 0
        }
    }

    // 2.4 criterion 5
    @Test
    fun preferencesAreTheOnlySourceOfScoreAndDifficulty() {
        val snapshot = GameSessionTestEncodings.humanTurn
        val first = store(Score(1, 2, 3), DifficultyLevel.Easy)
        val second = store(Score(9, 8, 7), DifficultyLevel.Hard)
        val a = harness.createViewModel(preferences = first, savedStateHandle = handleOf(snapshot))
        val b = harness.createViewModel(preferences = second, savedStateHandle = handleOf(snapshot))

        assertEquals(Score(1, 2, 3), a.uiState.value.score)
        assertEquals(DifficultyLevel.Easy, a.uiState.value.difficulty)
        assertEquals(Score(9, 8, 7), b.uiState.value.score)
        assertEquals(DifficultyLevel.Hard, b.uiState.value.difficulty)
        assertEquals(a.uiState.value.board, b.uiState.value.board)
    }

    // 2.4 criterion 6
    @Test
    fun soundPreferenceIsRestoredFromSnapshotAndDefaultsToTrue() {
        val muted = harness.createViewModel(savedStateHandle = handleOf(GameSessionTestEncodings.humanTurn))
        assertEquals(false, muted.uiState.value.soundEnabled)

        val fallback = harness.createViewModel(savedStateHandle = handleOf(LongArray(3)))
        assertTrue(fallback.uiState.value.soundEnabled)
    }

    // 2.5 criterion 8
    @Test
    fun emptyHandleStartsAFreshSessionAndStoresItsSnapshot() {
        val handle = SavedStateHandle()
        val preferences = store()
        val game = TicTacToeGame(randomizer)
        val viewModel = harness.createViewModel(game, preferences = preferences, savedStateHandle = handle)

        assertFreshSession(viewModel, game, preferences)
        val stored = GameSessionSnapshotCodec.decode(handle.get<Any>(GameSessionSnapshot.SAVED_STATE_KEY))
        assertEquals(GameSessionTestEncodings.emptyStarterX, stored)
        assertEquals(0, preferences.scoreWrites)
    }

    // 2.5 criterion 10
    @Test
    fun newViewModelWithEmptyHandleDoesNotResumeAClosedSession() {
        val oldHandle = SavedStateHandle()
        val old = harness.createViewModelResumed(preferences = store(), savedStateHandle = oldHandle)
        old.onHumanMove(4)
        advance(1_000)
        assertNotNull(oldHandle.get<Any>(GameSessionSnapshot.SAVED_STATE_KEY))

        val preferences = store()
        val game = TicTacToeGame(randomizer)
        val fresh = harness.createViewModel(game, preferences = preferences, savedStateHandle = SavedStateHandle())
        assertFreshSession(fresh, game, preferences)
    }

    private class RecordingSink : MoveSoundSink {
        var plays = 0
        var stops = 0

        override fun play(player: Player) {
            plays++
        }

        override fun stop() {
            stops++
        }
    }
}
