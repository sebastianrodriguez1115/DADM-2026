package com.edu.unal.tictactoe.ui.game

import androidx.lifecycle.SavedStateHandle
import com.edu.unal.tictactoe.data.preferences.FakeGamePreferences
import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.session.GameSessionSnapshot
import com.edu.unal.tictactoe.ui.game.session.GameSessionSnapshotCodec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/** The saved snapshot is valid and current after every relevant session change (task 2.5). */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelSessionSnapshotTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    private val preferences = FakeGamePreferences(Score(3, 5, 2), DifficultyLevel.Easy)
    private val handle = SavedStateHandle()
    private lateinit var viewModel: GameViewModel

    private fun start() {
        viewModel = harness.createViewModel(
            TicTacToeGame(MoveRandomizer { 0 }),
            preferences = preferences,
            savedStateHandle = handle,
        )
        viewModel.onHostResumed()
    }

    private fun advance(milliseconds: Long) {
        harness.scheduler.advanceTimeBy(milliseconds)
        harness.scheduler.runCurrent()
    }

    private fun savedSnapshot(from: SavedStateHandle = handle): GameSessionSnapshot? =
        GameSessionSnapshotCodec.decode(from.get<Any>(GameSessionSnapshot.SAVED_STATE_KEY))

    /** The handle holds a valid snapshot equivalent to the published state. */
    private fun assertSnapshotCurrent(step: String, pending: Long? = null) {
        val state = viewModel.uiState.value
        val expected = GameSessionSnapshot(
            board = state.board,
            turn = state.turn,
            result = state.result,
            starter = state.starter,
            nextStarter = state.nextStarter,
            soundEnabled = state.soundEnabled,
            pendingComputerRemainingNanos = pending,
        )
        assertNotNull("$step: snapshot must decode", savedSnapshot())
        assertEquals(step, expected, savedSnapshot())
    }

    // Criterion 7
    @Test
    fun snapshotIsValidAndCurrentAfterEveryOperation() {
        start()
        assertSnapshotCurrent("initial")

        viewModel.onHumanMove(4)
        assertSnapshotCurrent("human move, O pending", ONE_SECOND)
        viewModel.toggleSound()
        assertSnapshotCurrent("toggle while pending", ONE_SECOND)
        viewModel.toggleSound()
        advance(1_000)
        assertSnapshotCurrent("computer response")
        viewModel.onHumanMove(1)
        advance(1_000)
        assertSnapshotCurrent("second response")
        viewModel.onHumanMove(7)
        assertEquals(GameResult.Won(Player.X), viewModel.uiState.value.result)
        assertSnapshotCurrent("human terminal")
        viewModel.toggleSound()
        assertSnapshotCurrent("toggle on terminal")
        viewModel.resetScores()
        assertSnapshotCurrent("reset scores")

        viewModel.newGame()
        assertSnapshotCurrent("new game, O opens (pending)", ONE_SECOND)
        advance(1_000)
        assertSnapshotCurrent("opening played")
        viewModel.onHumanMove(4)
        advance(1_000)
        viewModel.onHumanMove(8)
        advance(1_000)
        assertEquals(GameResult.Won(Player.O), viewModel.uiState.value.result)
        assertEquals(Turn.Computer, viewModel.uiState.value.turn)
        assertSnapshotCurrent("computer terminal")

        viewModel.newGame()
        assertSnapshotCurrent("new game, X starts")
        assertEquals(Turn.Human, viewModel.uiState.value.turn)
        viewModel.setDifficulty(DifficultyLevel.Hard)
        assertSnapshotCurrent("set difficulty")
        viewModel.resetScores()
        assertSnapshotCurrent("reset scores again")
        viewModel.newGame()
        assertSnapshotCurrent("new game over a pending-free board, O opens", ONE_SECOND)
        viewModel.newGame()
        assertSnapshotCurrent("new game replacing pending turn")
    }

    // Criterion 9
    @Test
    fun handleValueRoundTripsThroughANewViewModelAtEveryPoint() {
        start()
        val points = mutableListOf<Pair<String, Any?>>()
        fun capture(name: String) = points.add(name to handle.get<Any>(GameSessionSnapshot.SAVED_STATE_KEY))

        capture("initial")
        viewModel.onHumanMove(4)
        capture("O pending")
        advance(1_000)
        capture("in progress")
        viewModel.toggleSound()
        capture("muted")
        viewModel.onHumanMove(1)
        advance(1_000)
        viewModel.onHumanMove(7)
        capture("terminal")
        viewModel.newGame()
        capture("O opening pending")

        points.forEach { (name, value) ->
            val before = savedSnapshot(SavedStateHandle(mapOf(GameSessionSnapshot.SAVED_STATE_KEY to value)))
            val restored = harness.createViewModel(
                TicTacToeGame(MoveRandomizer { 0 }),
                preferences = preferences,
                savedStateHandle = SavedStateHandle(mapOf(GameSessionSnapshot.SAVED_STATE_KEY to value)),
            )
            val state = restored.uiState.value
            assertEquals(name, before!!.board, state.board)
            assertEquals(name, before.turn, state.turn)
            assertEquals(name, before.result, state.result)
            assertEquals(name, before.starter, state.starter)
            assertEquals(name, before.nextStarter, state.nextStarter)
            assertEquals(name, before.soundEnabled, state.soundEnabled)
            assertEquals(name, preferences.storedScore, state.score)
            assertEquals(name, preferences.storedDifficulty, state.difficulty)
        }
    }

    private companion object {
        const val ONE_SECOND = 1_000_000_000L
    }
}
