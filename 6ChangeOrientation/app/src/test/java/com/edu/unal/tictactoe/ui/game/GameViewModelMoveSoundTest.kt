package com.edu.unal.tictactoe.ui.game

import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundSink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelMoveSoundTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    @Test
    fun emitsOnceForAcceptedHumanComputerAndTerminalMovesOnly() {
        val vm = harness.createViewModelResumed(TicTacToeGame(MoveRandomizer { 0 }))
        vm.setDifficulty(DifficultyLevel.Easy)
        val sink = RecordingSink()
        vm.registerMoveSoundSink(Any(), sink)

        vm.onHumanMove(-1) // out of bounds
        assertTrue(sink.plays.isEmpty())
        vm.onHumanMove(0)
        assertEquals(listOf(Player.X), sink.plays)
        vm.onHumanMove(0) // occupied during computer turn
        assertEquals(listOf(Player.X), sink.plays)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(listOf(Player.X, Player.O), sink.plays)

        vm.onHumanMove(3)
        assertEquals(listOf(Player.X, Player.O, Player.X), sink.plays)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(listOf(Player.X, Player.O, Player.X, Player.O), sink.plays)
        vm.onHumanMove(6) // terminal X
        vm.onHumanMove(1) // rejected after terminal
        assertEquals(listOf(Player.X, Player.O, Player.X, Player.O, Player.X), sink.plays)
        assertEquals(1, vm.uiState.value.score.xWins)
    }

    @Test
    fun openingAndTerminalComputerMoveEmitOnce() {
        val vm = harness.createViewModelResumed(TicTacToeGame(MoveRandomizer { 0 }))
        vm.setDifficulty(DifficultyLevel.Easy)
        val sink = RecordingSink()
        vm.registerMoveSoundSink(Any(), sink)
        vm.newGame() // O opening
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        vm.onHumanMove(3)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        vm.onHumanMove(4)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(listOf(Player.O, Player.X, Player.O, Player.X, Player.O), sink.plays)
        assertEquals(1, vm.uiState.value.score.oWins)
        vm.onHumanMove(8)
        assertEquals(5, sink.plays.size)
    }

    @Test
    fun registeringLaterDoesNotReplayAndMissingSinkDropsHumanAndComputerAttempts() {
        val vm = harness.createViewModelResumed()
        vm.onHumanMove(0)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(1, vm.uiState.value.board.count { it == Mark.O })

        val lateSink = RecordingSink()
        vm.registerMoveSoundSink(Any(), lateSink)
        assertTrue(lateSink.plays.isEmpty())
    }

    @Test
    fun replacingOwnerStopsOldSinkAndLateUnregisterCannotAffectCurrentOwner() {
        val vm = harness.createViewModelResumed()
        val oldSink = RecordingSink()
        val newSink = RecordingSink()
        val oldOwner = Any()
        val newOwner = Any()
        vm.registerMoveSoundSink(oldOwner, oldSink)
        vm.registerMoveSoundSink(newOwner, newSink)
        assertEquals(1, oldSink.stops)
        assertTrue(vm.isMoveSoundOwnerCurrent(newOwner))

        vm.unregisterMoveSoundSink(oldOwner)
        assertTrue(vm.isMoveSoundOwnerCurrent(newOwner))
        vm.onHumanMove(0)
        assertEquals(listOf(Player.X), newSink.plays)
        assertTrue(oldSink.plays.isEmpty())
        assertEquals(0, newSink.stops)
    }

    @Test
    fun unregisterCurrentOwnerDetachesAndStopsBeforeReturning() {
        val vm = harness.createViewModelResumed()
        val sink = RecordingSink()
        val owner = Any()
        vm.registerMoveSoundSink(owner, sink)
        vm.unregisterMoveSoundSink(owner)
        assertEquals(1, sink.stops)
        assertTrue(!vm.isMoveSoundOwnerCurrent(owner))
        vm.onHumanMove(0)
        assertTrue(sink.plays.isEmpty())
    }

    @Test
    fun pauseAccountsForForegroundTimeBeforeAudioStopCost() {
        val vm = harness.createViewModelResumed()
        val sink = object : MoveSoundSink {
            override fun play(player: Player) = Unit
            override fun stop() {
                harness.scheduler.advanceTimeBy(200)
            }
        }
        vm.registerMoveSoundSink(Any(), sink)
        vm.onHumanMove(0)
        harness.scheduler.advanceTimeBy(400)
        harness.scheduler.runCurrent()

        vm.onHostPaused()
        assertEquals(600L, harness.scheduler.currentTime)
        vm.onHostResumed()
        harness.scheduler.advanceTimeBy(599)
        harness.scheduler.runCurrent()
        assertEquals(0, vm.uiState.value.board.count { it == Mark.O })
        harness.scheduler.advanceTimeBy(1)
        harness.scheduler.runCurrent()
        assertEquals(1, vm.uiState.value.board.count { it == Mark.O })
    }

    @Test
    fun mutedAndPausedAcceptedMovesAreDroppedWithoutReplay() {
        val mutedVm = harness.createViewModelResumed()
        val mutedSink = RecordingSink()
        mutedVm.registerMoveSoundSink(Any(), mutedSink)
        mutedVm.toggleSound()
        assertEquals(1, mutedSink.stops)
        mutedVm.onHumanMove(0)
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(1, mutedVm.uiState.value.board.count { it == Mark.O })
        assertTrue(mutedSink.plays.isEmpty())

        val pausedVm = harness.createViewModelResumed()
        val pausedSink = RecordingSink()
        pausedVm.registerMoveSoundSink(Any(), pausedSink)
        pausedVm.onHostPaused()
        pausedVm.onHumanMove(0)
        assertTrue(pausedSink.plays.isEmpty())
        pausedVm.onHostResumed()
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(1, pausedVm.uiState.value.board.count { it == Mark.O })
        assertTrue(pausedSink.plays.isEmpty())
        val resumedSink = RecordingSink()
        pausedVm.registerMoveSoundSink(Any(), resumedSink)
        assertTrue(resumedSink.plays.isEmpty())
    }

    @Test
    fun reentrantSoundNewGamesCannotBeOverwrittenByOldPendingTurn() {
        val vm = harness.createViewModelResumed()
        val sink = object : MoveSoundSink {
            var reentered = false
            override fun play(player: Player) {
                if (player == Player.X && !reentered) {
                    reentered = true
                    vm.newGame()
                    vm.newGame()
                }
            }
            override fun stop() = Unit
        }
        vm.registerMoveSoundSink(Any(), sink)
        vm.onHumanMove(0)
        assertEquals(Player.X, vm.uiState.value.starter)
        assertEquals(Turn.Human, vm.uiState.value.turn)
        assertEquals(0, vm.uiState.value.board.count { it != Mark.Empty })
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(0, vm.uiState.value.board.count { it != Mark.Empty })
    }

    @Test
    fun throwingSinkCannotRollbackMoveOrPreventResponseAndStopFailuresAreAbsorbed() {
        val vm = harness.createViewModelResumed()
        val throwingSink = ThrowingSink()
        vm.registerMoveSoundSink(Any(), throwingSink)
        vm.onHumanMove(0)
        assertEquals(1, throwingSink.playCalls)
        assertEquals(Mark.X, vm.uiState.value.board[0])
        harness.scheduler.advanceTimeBy(1_000)
        harness.scheduler.runCurrent()
        assertEquals(1, vm.uiState.value.board.count { it == Mark.O })

        vm.toggleSound()
        assertEquals(1, throwingSink.stopCalls)
        assertTrue(!vm.uiState.value.soundEnabled)
        vm.onHostPaused()
        assertEquals(2, throwingSink.stopCalls)
        assertEquals(1, vm.uiState.value.board.count { it == Mark.O })
    }

    private class RecordingSink : MoveSoundSink {
        val plays = mutableListOf<Player>()
        var stops = 0
        override fun play(player: Player) { plays += player }
        override fun stop() { stops++ }
    }

    private class ThrowingSink : MoveSoundSink {
        var playCalls = 0
        var stopCalls = 0
        override fun play(player: Player) {
            playCalls++
            error("test play failure")
        }
        override fun stop() {
            stopCalls++
            error("test stop failure")
        }
    }
}
