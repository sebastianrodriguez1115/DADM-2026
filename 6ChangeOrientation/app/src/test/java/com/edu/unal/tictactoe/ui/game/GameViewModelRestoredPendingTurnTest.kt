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
import com.edu.unal.tictactoe.ui.game.session.GameSessionSnapshot
import com.edu.unal.tictactoe.ui.game.session.GameSessionSnapshotCodec
import com.edu.unal.tictactoe.ui.game.session.GameSessionTestEncodings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Task 2.9: a restored pending turn of O is only a logical duration until the host is RESUMED. */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelRestoredPendingTurnTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    private enum class Kind { Response, Opening }

    private fun millisToNanos(millis: Long) = millis * 1_000_000L

    /** X at 0 and O to answer (starter X), or an empty board with O opening (starter O). */
    private fun pending(kind: Kind, remainingMillis: Long): GameSessionSnapshot = when (kind) {
        Kind.Response -> GameSessionSnapshot(
            board = GameSessionTestEncodings.board("X........"),
            turn = Turn.Computer,
            result = GameResult.InProgress,
            starter = Player.X,
            nextStarter = Player.O,
            soundEnabled = true,
            pendingComputerRemainingNanos = millisToNanos(remainingMillis),
        )

        Kind.Opening -> GameSessionSnapshot(
            board = GameSessionTestEncodings.board("........."),
            turn = Turn.Computer,
            result = GameResult.InProgress,
            starter = Player.O,
            nextStarter = Player.X,
            soundEnabled = true,
            pendingComputerRemainingNanos = millisToNanos(remainingMillis),
        )
    }

    private fun handleOf(snapshot: GameSessionSnapshot) = SavedStateHandle(
        mapOf(GameSessionSnapshot.SAVED_STATE_KEY to GameSessionSnapshotCodec.encode(snapshot)),
    )

    private fun restore(
        kind: Kind,
        remainingMillis: Long,
        preferences: FakeGamePreferences = FakeGamePreferences(),
        randomizer: MoveRandomizer = MoveRandomizer { 0 },
    ) = harness.createViewModel(
        TicTacToeGame(randomizer),
        preferences = preferences,
        savedStateHandle = handleOf(pending(kind, remainingMillis)),
    )

    private fun advance(milliseconds: Long) {
        harness.scheduler.advanceTimeBy(milliseconds)
        harness.scheduler.runCurrent()
    }

    private fun marks(vm: GameViewModel) = vm.uiState.value.board.count { it != Mark.Empty }

    private fun initialMarks(kind: Kind) = if (kind == Kind.Response) 1 else 0

    // Criterion 1
    @Test
    fun noMoveWithoutResumeThenExactlyOneAtTheRemaining() {
        Kind.entries.forEach { kind ->
            val vm = restore(kind, 1_000)
            advance(5_000)
            assertEquals("$kind: nothing before RESUMED", initialMarks(kind), marks(vm))

            vm.onHostResumed()
            advance(999)
            assertEquals("$kind: nothing at 999 ms", initialMarks(kind), marks(vm))
            advance(1)
            assertEquals("$kind: one move at 1000 ms", initialMarks(kind) + 1, marks(vm))
            assertEquals(Turn.Human, vm.uiState.value.turn)
            advance(10_000)
            assertEquals("$kind: no second move", initialMarks(kind) + 1, marks(vm))
        }
    }

    // Criterion 2
    @Test
    fun repeatedResumeSchedulesOnlyOnce() {
        Kind.entries.forEach { kind ->
            val vm = restore(kind, 1_000)
            vm.onHostResumed()
            vm.onHostResumed()
            advance(500)
            vm.onHostResumed()
            advance(500)
            vm.onHostResumed()
            advance(10_000)
            assertEquals("$kind", initialMarks(kind) + 1, marks(vm))
        }
    }

    // Criterion 3
    @Test
    fun zeroRemainingRunsExactlyOnceOnResumeWithoutExtraTime() {
        Kind.entries.forEach { kind ->
            val vm = restore(kind, 0)
            advance(5_000)
            assertEquals("$kind: nothing before RESUMED", initialMarks(kind), marks(vm))

            val resumedAt = harness.scheduler.currentTime
            vm.onHostResumed()
            harness.scheduler.runCurrent()
            assertEquals("$kind: one move without waiting", initialMarks(kind) + 1, marks(vm))
            assertEquals(resumedAt, harness.scheduler.currentTime)
            advance(10_000)
            assertEquals("$kind: exactly once", initialMarks(kind) + 1, marks(vm))
        }
    }

    // Criterion 4
    @Test
    fun pauseAndResumeOnARestoredTurnKeepTheIntermediateRemaining() {
        Kind.entries.forEach { kind ->
            val vm = restore(kind, 700)
            vm.onHostResumed()
            advance(300)
            vm.onHostPaused()
            advance(5_000)
            assertEquals("$kind: nothing in background", initialMarks(kind), marks(vm))

            vm.onHostResumed()
            advance(399)
            assertEquals("$kind: nothing at 399 ms", initialMarks(kind), marks(vm))
            advance(1)
            assertEquals("$kind: one move at 400 ms", initialMarks(kind) + 1, marks(vm))
            advance(10_000)
            assertEquals("$kind: exactly once", initialMarks(kind) + 1, marks(vm))
        }
    }

    // Criterion 5
    @Test
    fun newGameCancelsTheRestoredTurnBeforeAndAfterResume() {
        listOf(false, true).forEach { resumedFirst ->
            // Response (starter X, next O): the new game opens with O after its own full second.
            val response = restore(Kind.Response, 300)
            if (resumedFirst) {
                response.onHostResumed()
                advance(100)
            }
            response.newGame()
            assertEquals(Player.O, response.uiState.value.starter)
            assertEquals(0, marks(response))
            if (!resumedFirst) response.onHostResumed()
            advance(999)
            assertEquals("resumedFirst=$resumedFirst: restored 300 ms must be gone", 0, marks(response))
            advance(1)
            assertEquals("resumedFirst=$resumedFirst: only the new opening", 1, marks(response))
            advance(10_000)
            assertEquals(1, marks(response))

            // Opening (starter O, next X): the new game starts with X and no O ever appears.
            val opening = restore(Kind.Opening, 300)
            if (resumedFirst) {
                opening.onHostResumed()
                advance(100)
            }
            opening.newGame()
            assertEquals(Player.X, opening.uiState.value.starter)
            assertEquals(Turn.Human, opening.uiState.value.turn)
            if (!resumedFirst) opening.onHostResumed()
            advance(10_000)
            assertEquals("resumedFirst=$resumedFirst: no O after X starts", 0, marks(opening))
        }
    }

    @Test
    fun newGameThatStartsXAlsoCancelsARestoredResponse() {
        val handle = handleOf(
            GameSessionSnapshot(
                board = GameSessionTestEncodings.board("O...X...."),
                turn = Turn.Computer,
                result = GameResult.InProgress,
                starter = Player.O,
                nextStarter = Player.X,
                soundEnabled = true,
                pendingComputerRemainingNanos = millisToNanos(300),
            ),
        )
        val vm = harness.createViewModel(savedStateHandle = handle)
        vm.onHostResumed()
        vm.newGame()
        advance(10_000)
        assertEquals(Player.X, vm.uiState.value.starter)
        assertEquals(0, marks(vm))
        assertNull(GameSessionSnapshotCodec.decode(handle.get<Any>(GameSessionSnapshot.SAVED_STATE_KEY))!!.pendingComputerRemainingNanos)
    }

    // Criterion 6
    @Test
    fun restoredMoveUsesCurrentDifficultyAndKeepsScoreAndSnapshotConsistent() {
        // O started and moves with equal counts: it can win at 2; Easy takes the randomizer's last free cell (8).
        val board = "OO.XX...."
        val snapshot = GameSessionSnapshot(
            board = GameSessionTestEncodings.board(board),
            turn = Turn.Computer,
            result = GameResult.InProgress,
            starter = Player.O,
            nextStarter = Player.X,
            soundEnabled = true,
            pendingComputerRemainingNanos = millisToNanos(250),
        )

        val easyStore = FakeGamePreferences(Score(1, 2, 3), DifficultyLevel.Easy)
        val easyHandle = handleOf(snapshot)
        val easy = harness.createViewModel(TicTacToeGame(MoveRandomizer { it - 1 }), preferences = easyStore, savedStateHandle = easyHandle)
        easy.onHostResumed()
        advance(250)
        assertEquals(Mark.O, easy.uiState.value.board[8])
        assertEquals(GameResult.InProgress, easy.uiState.value.result)
        assertEquals(0, easyStore.scoreWrites)
        assertNull(GameSessionSnapshotCodec.decode(easyHandle.get<Any>(GameSessionSnapshot.SAVED_STATE_KEY))!!.pendingComputerRemainingNanos)

        val expertStore = FakeGamePreferences(Score(1, 2, 3), DifficultyLevel.Expert)
        val expertHandle = handleOf(snapshot)
        val expert = harness.createViewModel(TicTacToeGame(MoveRandomizer { 0 }), preferences = expertStore, savedStateHandle = expertHandle)
        expert.onHostResumed()
        advance(250)
        assertEquals(Mark.O, expert.uiState.value.board[2])
        assertEquals(GameResult.Won(Player.O), expert.uiState.value.result)
        assertEquals(1, expertStore.scoreWrites)
        assertEquals(Score(1, 3, 3), expertStore.storedScore)
        advance(10_000)
        assertEquals(1, expertStore.scoreWrites)
        val stored = GameSessionSnapshotCodec.decode(expertHandle.get<Any>(GameSessionSnapshot.SAVED_STATE_KEY))
        assertNotNull(stored)
        assertNull(stored!!.pendingComputerRemainingNanos)
        assertEquals(GameResult.Won(Player.O), stored.result)
    }

    // Criterion 7
    @Test
    fun theHandleCarriesOnlyADurationAndNothingTiesTheRestoredTurnToTheRestoreInstant() {
        val handle = handleOf(pending(Kind.Response, 700))
        val vm = harness.createViewModel(savedStateHandle = handle)
        val rewritten = GameSessionSnapshotCodec.decode(handle.get<Any>(GameSessionSnapshot.SAVED_STATE_KEY))!!
        assertEquals(millisToNanos(700), rewritten.pendingComputerRemainingNanos)

        // A long stretch of virtual time passes before RESUMED: no clock was captured at restore.
        advance(60_000)
        vm.onHostResumed()
        advance(699)
        assertEquals(1, marks(vm))
        advance(1)
        assertEquals(2, marks(vm))
    }
}
