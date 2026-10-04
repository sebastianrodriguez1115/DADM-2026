package com.edu.unal.tictactoe.ui.game.audio

import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.ui.game.GameViewModelCoroutineTestHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoveSoundHostTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    @Test
    fun preparesRegistersThenResumesAndStaleHostCannotAffectReplacement() {
        val vm = harness.createViewModel(TicTacToeGame(MoveRandomizer { 0 }))
        vm.setDifficulty(DifficultyLevel.Easy)
        vm.newGame() // O opening remains pending before a host is resumed.
        val oldBackend = FakePlayer()
        val oldHost = MoveSoundHost(vm, MoveSoundPlayerFactory { oldBackend })

        oldHost.onResume()
        assertEquals(listOf("load", "load"), oldBackend.events)
        assertEquals(0, vm.uiState.value.board.count { it == Mark.O })
        oldBackend.completeLoads()
        harness.scheduler.advanceTimeBy(999)
        harness.scheduler.runCurrent()
        assertEquals(0, vm.uiState.value.board.count { it == Mark.O })
        harness.scheduler.advanceTimeBy(1)
        harness.scheduler.runCurrent()
        assertEquals(listOf(2), oldBackend.played)

        oldHost.onPause()
        assertEquals(1, oldBackend.releases)
        val newBackend = FakePlayer()
        val newHost = MoveSoundHost(vm, MoveSoundPlayerFactory { newBackend })
        newHost.onResume()
        assertEquals(listOf("load", "load"), newBackend.events)
        val oldLateCallback = oldBackend.callbacks.first()
        oldLateCallback(1, 0) // Same ID the replacement pool assigned to X.
        oldHost.onPause() // Must be ignored by the retained session while the new owner is active.
        oldHost.onDestroy()

        vm.onHumanMove(vm.uiState.value.board.indexOf(Mark.Empty))
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(2, vm.uiState.value.board.count { it == Mark.O })
        assertEquals(emptyList<Int>(), newBackend.played)

        // Repeating stale lifecycle events cannot unregister or pause the new host.
        oldLateCallback(1, 0)
        newBackend.completeLoads()
        vm.onHumanMove(vm.uiState.value.board.indexOf(Mark.Empty))
        assertEquals(listOf(1), newBackend.played)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(listOf(1, 2), newBackend.played)
        newHost.onDestroy()
        assertEquals(1, newBackend.releases)
    }

    @Test
    fun zeroRemainingTurnCompletesOnReplacementResumeWithoutAudioReplay() {
        val dispatcher = UnconfinedTestDispatcher(harness.scheduler)
        val vm = harness.createViewModel(TicTacToeGame(MoveRandomizer { 0 }), dispatcher)
        vm.setDifficulty(DifficultyLevel.Easy)
        vm.newGame()

        val firstBackend = FakePlayer()
        val firstHost = MoveSoundHost(vm, MoveSoundPlayerFactory { firstBackend })
        firstHost.onResume()
        firstBackend.completeLoads()
        harness.scheduler.advanceTimeBy(1_000) // Due, but not dispatched at the boundary.
        assertEquals(0, vm.uiState.value.board.count { it == Mark.O })
        firstHost.onPause() // Stores exactly zero and cancels the due job.
        assertEquals(1, firstBackend.releases)

        val replacementBackend = FakePlayer()
        val replacementHost = MoveSoundHost(vm, MoveSoundPlayerFactory { replacementBackend })
        replacementHost.onResume() // Unconfined dispatcher resolves zero remainder inline.
        assertEquals(1, vm.uiState.value.board.count { it == Mark.O })
        assertEquals(emptyList<Int>(), replacementBackend.played) // Not ready when accepted.
        replacementBackend.completeLoads()
        assertEquals(emptyList<Int>(), replacementBackend.played) // Readiness never replays.
        assertEquals(0, vm.uiState.value.score.oWins)
        replacementHost.onDestroy()
    }

    @Test
    fun viewModelMuteStopsActiveStreamImmediatelyAndKeepsPendingTurn() {
        val vm = harness.createViewModel()
        vm.setDifficulty(DifficultyLevel.Easy)
        val backend = FakePlayer()
        val host = MoveSoundHost(vm, MoveSoundPlayerFactory { backend })
        host.onResume()
        backend.completeLoads()

        vm.onHumanMove(0)
        assertEquals(listOf(1), backend.played)
        assertEquals(emptyList<Int>(), backend.stopped)
        vm.toggleSound()

        assertFalse(vm.uiState.value.soundEnabled)
        assertEquals(listOf(1), backend.stopped)
        harness.scheduler.advanceTimeBy(999)
        harness.scheduler.runCurrent()
        assertEquals(0, vm.uiState.value.board.count { it == Mark.O })
        harness.scheduler.advanceTimeBy(1)
        harness.scheduler.runCurrent()
        assertEquals(1, vm.uiState.value.board.count { it == Mark.O })
        assertEquals(listOf(1), backend.played)
        host.onDestroy()
    }

    private class FakePlayer : MoveSoundPlayer {
        val events = mutableListOf<String>()
        val callbacks = mutableListOf<(Int, Int) -> Unit>()
        val played = mutableListOf<Int>()
        val stopped = mutableListOf<Int>()
        var releases = 0

        override fun load(resourceId: Int, onLoaded: (Int, Int) -> Unit): Int {
            events += "load"
            callbacks += onLoaded
            return callbacks.size
        }

        fun completeLoads() = callbacks.forEachIndexed { index, callback -> callback(index + 1, 0) }

        override fun play(sampleId: Int): Int {
            events += "play"
            played += sampleId
            return played.size
        }

        override fun stop(streamId: Int) {
            events += "stop"
            stopped += streamId
        }

        override fun release() {
            releases++
        }
    }
}
