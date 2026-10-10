package com.edu.unal.tictactoe.ui.game

import androidx.lifecycle.SavedStateHandle
import com.edu.unal.tictactoe.data.preferences.FakeGamePreferences
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
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

/** Task 2.10: sound lives only in the temporary snapshot; effects are for genuinely new moves. */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelSessionSoundTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    private val owner = Any()
    private val sink = RecordingSink()

    private fun handleOf(value: Any?) = SavedStateHandle().apply {
        if (value != null) set(GameSessionSnapshot.SAVED_STATE_KEY, value)
    }

    private fun handleOf(snapshot: GameSessionSnapshot) = handleOf(GameSessionSnapshotCodec.encode(snapshot))

    private fun advance(milliseconds: Long) {
        harness.scheduler.advanceTimeBy(milliseconds)
        harness.scheduler.runCurrent()
    }

    private fun restored(snapshot: GameSessionSnapshot, preferences: FakeGamePreferences = FakeGamePreferences()) =
        harness.createViewModel(preferences = preferences, savedStateHandle = handleOf(snapshot)).also {
            it.registerMoveSoundSink(owner, sink)
        }

    @Test
    fun mutedSnapshotRestoresMutedAndTheFutureMoveOfOIsSilent() {
        val snapshot = GameSessionTestEncodings.emptyStarterOPendingMid // sound false, 400 ms pending
        val vm = restored(snapshot)
        assertEquals(false, vm.uiState.value.soundEnabled)

        vm.onHostResumed()
        assertEquals(0, sink.played.size)
        advance(400)
        assertEquals(1, vm.uiState.value.board.count { it == Mark.O })
        advance(5_000)
        assertEquals(emptyList<Player>(), sink.played)
    }

    @Test
    fun activeSoundRestoreDoesNotReplayAndTheFutureMoveOfOPlaysExactlyOnce() {
        val snapshot = GameSessionTestEncodings.computerPending // X at 0, sound true, 400 ms pending
        val vm = restored(snapshot)
        assertEquals(true, vm.uiState.value.soundEnabled)

        assertEquals(emptyList<Player>(), sink.played)
        vm.onHostResumed()
        advance(399)
        assertEquals("marks already on the board are never replayed", emptyList<Player>(), sink.played)
        advance(1)
        assertEquals(listOf(Player.O), sink.played)
        advance(10_000)
        assertEquals(listOf(Player.O), sink.played)
    }

    @Test
    fun absentOrInvalidSnapshotsRestoreWithSoundActive() {
        assertEquals(true, harness.createViewModel(savedStateHandle = handleOf(null)).uiState.value.soundEnabled)
        GameSessionTestEncodings.corruptions.forEach { (name, value) ->
            val vm = harness.createViewModel(savedStateHandle = handleOf(value))
            assertEquals(name, true, vm.uiState.value.soundEnabled)
        }
    }

    @Test
    fun toggleSoundUpdatesTheSnapshotAndNeverWritesTheStore() {
        val preferences = FakeGamePreferences()
        val handle = SavedStateHandle()
        val vm = harness.createViewModel(preferences = preferences, savedStateHandle = handle)
        fun saved() = GameSessionSnapshotCodec.decode(handle.get<Any>(GameSessionSnapshot.SAVED_STATE_KEY))

        assertEquals(true, saved()!!.soundEnabled)
        vm.toggleSound()
        assertNotNull(saved())
        assertEquals(false, saved()!!.soundEnabled)
        vm.toggleSound()
        assertEquals(true, saved()!!.soundEnabled)
        vm.toggleSound()
        assertEquals(false, saved()!!.soundEnabled)

        assertEquals(0, preferences.scoreWrites)
        assertEquals(0, preferences.difficultyWrites)
    }

    private class RecordingSink : MoveSoundSink {
        val played = mutableListOf<Player>()

        override fun play(player: Player) {
            played.add(player)
        }

        override fun stop() = Unit
    }
}
