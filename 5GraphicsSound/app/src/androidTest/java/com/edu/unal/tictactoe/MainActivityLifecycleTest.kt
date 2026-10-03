package com.edu.unal.tictactoe

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.GameViewModel
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLifecycleTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var viewModel: GameViewModel
    private val computerMoveObservers = mutableListOf<ComputerMoveTimestamp>()

    @Before
    fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { viewModel = ViewModelProvider(it)[GameViewModel::class.java] }
        composeTestRule.waitForIdle()
    }

    @After
    fun close() {
        computerMoveObservers.forEach(ComputerMoveTimestamp::close)
        computerMoveObservers.clear()
        if (::scenario.isInitialized) scenario.close()
    }

    @Test
    fun pauseSuspendsPendingResponseAndResumeCompletesItOnce() {
        val lifecycleTimes = observeHostLifecycle()
        val computerMoveTime = observeComputerMove(viewModel)
        try {
            val moveStartedAt = AtomicLong(0L)
            scenario.onActivity {
                moveStartedAt.set(SystemClock.elapsedRealtimeNanos())
                viewModel.onHumanMove(0)
            }
            assertEquals(1, viewModel.uiState.value.board.count { it != Mark.Empty })

            SystemClock.sleep(400)
            scenario.moveToState(Lifecycle.State.CREATED)
            val intervalEnd = lifecycleTimes.pausedAt.get()
            val foregroundNanos = intervalEnd - moveStartedAt.get()
            val foregroundMillis = foregroundNanos / NANOS_PER_MILLISECOND
            // Device timing is measured, not assumed to be exactly 400 ms; bound it so
            // the remaining deadline is meaningful while tolerating instrumentation jitter.
            assertTrue("measured foreground interval was $foregroundMillis ms", foregroundMillis in 350L..800L)
            // Longer than a complete O delay in background: no response may be applied.
            SystemClock.sleep(1_200)
            assertEquals(1, viewModel.uiState.value.board.count { it != Mark.Empty })
            assertEquals(0, viewModel.uiState.value.board.count { it == Mark.O })

            scenario.moveToState(Lifecycle.State.RESUMED)
            val deadlineResume = lifecycleTimes.resumedAt.get()
            assertTrue("observer must see the real ON_RESUME callback", deadlineResume > intervalEnd)
            val remainingNanos = COMPUTER_DELAY_NANOS - foregroundNanos
            composeTestRule.waitUntil(timeoutMillis = 1_000) {
                viewModel.uiState.value.board.count { it == Mark.O } == 1
            }
            val observedAt = computerMoveTime.completedAt.get()
            assertTrue("computer move timestamp must be captured", observedAt > 0L)
            val allowedCompletionNanos = minOf(
                MAX_COMPLETION_BEFORE_RESTART_NANOS,
                remainingNanos + COMPLETION_JITTER_NANOS,
            )
            val earliestCompletionNanos = (remainingNanos - PROBE_MARGIN_NANOS).coerceAtLeast(0L)
            val elapsedAfterResume = observedAt - deadlineResume
            assertTrue(
                "O must not complete before its measured remaining delay (remaining=$remainingNanos ns)",
                elapsedAfterResume >= earliestCompletionNanos,
            )
            assertTrue(
                "O must complete near its measured remaining deadline (remaining=$remainingNanos ns)",
                elapsedAfterResume <= allowedCompletionNanos,
            )
            val completed = viewModel.uiState.value
            assertEquals(1, completed.board.count { it == Mark.X })
            assertEquals(1, completed.board.count { it == Mark.O })
            SystemClock.sleep(1_100)
            assertEquals(completed.board, viewModel.uiState.value.board)
            assertSame(viewModel, currentViewModel())
        } finally {
            lifecycleTimes.close()
        }
    }

    @Test
    fun recreationDuringPendingResponseRetainsSessionWithoutDuplicatingTurn() {
        val lifecycleTimes = observeHostLifecycle()
        val computerMoveTime = observeComputerMove(viewModel)
        try {
            var startedAt = 0L
            scenario.onActivity {
                viewModel.setDifficulty(DifficultyLevel.Hard)
                viewModel.toggleSound()
                startedAt = SystemClock.elapsedRealtimeNanos()
                viewModel.onHumanMove(0)
            }
            // The foreground interval before recreation is intentionally measured.
            SystemClock.sleep(400)
            val retained = viewModel
            val beforeRecreation = retained.uiState.value
            scenario.recreate()
            composeTestRule.waitForIdle()
            assertSame(retained, currentViewModel())

            val afterRecreation = retained.uiState.value
            assertEquals(Mark.X, afterRecreation.board[0])
            assertEquals(1, afterRecreation.board.count { it == Mark.X })
            assertTrue(afterRecreation.board.count { it == Mark.O } <= 1)
            assertEquals(beforeRecreation.starter, afterRecreation.starter)
            assertEquals(beforeRecreation.nextStarter, afterRecreation.nextStarter)
            assertEquals(beforeRecreation.score, afterRecreation.score)
            assertEquals(beforeRecreation.difficulty, afterRecreation.difficulty)
            assertEquals(beforeRecreation.soundEnabled, afterRecreation.soundEnabled)
            awaitRetainedPendingTurn(lifecycleTimes, startedAt, retained, computerMoveTime)
            val completedBoard = retained.uiState.value.board
            SystemClock.sleep(1_100)
            assertEquals(completedBoard, retained.uiState.value.board)
            assertEquals(1, completedBoard.count { it == Mark.O })
        } finally {
            lifecycleTimes.close()
        }
    }

    @Test
    fun recreationDuringPendingOpeningRetainsSessionAndExecutesOneOpening() {
        val lifecycleTimes = observeHostLifecycle()
        val computerMoveTime = observeComputerMove(viewModel)
        try {
            var startedAt = 0L
            scenario.onActivity {
                viewModel.setDifficulty(DifficultyLevel.Hard)
                viewModel.toggleSound()
                startedAt = SystemClock.elapsedRealtimeNanos()
                viewModel.newGame()
            }
            SystemClock.sleep(400)
            val beforeRecreation = viewModel.uiState.value
            assertEquals(Turn.Computer, beforeRecreation.turn)
            assertEquals(0, beforeRecreation.board.count { it != Mark.Empty })

            val retained = viewModel
            scenario.recreate()
            composeTestRule.waitForIdle()
            assertSame(retained, currentViewModel())
            val afterRecreation = retained.uiState.value
            assertEquals(beforeRecreation.starter, afterRecreation.starter)
            assertEquals(beforeRecreation.nextStarter, afterRecreation.nextStarter)
            assertEquals(beforeRecreation.score, afterRecreation.score)
            assertEquals(beforeRecreation.difficulty, afterRecreation.difficulty)
            assertEquals(beforeRecreation.soundEnabled, afterRecreation.soundEnabled)
            assertTrue(afterRecreation.board.count { it == Mark.O } <= 1)
            assertEquals(0, afterRecreation.board.count { it == Mark.X })
            awaitRetainedPendingTurn(lifecycleTimes, startedAt, retained, computerMoveTime)
            assertEquals(1, retained.uiState.value.board.count { it == Mark.O })
            assertEquals(0, retained.uiState.value.board.count { it == Mark.X })
        } finally {
            lifecycleTimes.close()
        }
    }

    @Test
    fun finishingWithPendingTurnCancelsItAndNextActivityStartsFreshSession() {
        scenario.onActivity { viewModel.onHumanMove(0) }
        val oldViewModel = viewModel
        val beforeFinish = oldViewModel.uiState.value
        assertEquals(1, beforeFinish.board.count { it == Mark.X })
        assertEquals(0, beforeFinish.board.count { it == Mark.O })
        assertEquals(Turn.Computer, beforeFinish.turn)
        scenario.onActivity { it.finish() }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { scenario.state == Lifecycle.State.DESTROYED }
        SystemClock.sleep(1_100)
        assertEquals(beforeFinish.board, oldViewModel.uiState.value.board)

        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        viewModel = currentViewModel()
        assertNotSame(oldViewModel, viewModel)
        assertTrue(viewModel.uiState.value.board.all { it == Mark.Empty })
        assertEquals(
            0,
            viewModel.uiState.value.score.xWins + viewModel.uiState.value.score.oWins + viewModel.uiState.value.score.ties,
        )
    }

    private fun currentViewModel(): GameViewModel {
        var result: GameViewModel? = null
        scenario.onActivity { result = ViewModelProvider(it)[GameViewModel::class.java] }
        return checkNotNull(result)
    }

    private fun observeHostLifecycle(): HostLifecycleTimestamps {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        return HostLifecycleTimestamps(application).also(application::registerActivityLifecycleCallbacks)
    }

    private fun observeComputerMove(viewModel: GameViewModel): ComputerMoveTimestamp =
        ComputerMoveTimestamp(viewModel).also(computerMoveObservers::add)

    private fun awaitRetainedPendingTurn(
        lifecycleTimes: HostLifecycleTimestamps,
        startedAt: Long,
        retained: GameViewModel,
        computerMoveTime: ComputerMoveTimestamp,
    ) {
        val pausedAt = lifecycleTimes.pausedAt.get()
        val resumedAt = lifecycleTimes.resumedAt.get()
        assertTrue("recreation must deliver a real pause callback", pausedAt > startedAt)
        assertTrue("recreation must deliver a real resume callback", resumedAt > pausedAt)
        val foregroundNanos = pausedAt - startedAt
        val foregroundMillis = foregroundNanos / NANOS_PER_MILLISECOND
        assertTrue("measured foreground interval was $foregroundMillis ms", foregroundMillis in 350L..800L)
        val remainingNanos = COMPUTER_DELAY_NANOS - foregroundNanos
        composeTestRule.waitUntil(timeoutMillis = 1_800) {
            retained.uiState.value.board.count { it == Mark.O } == 1 &&
                computerMoveTime.completedAt.get() > 0L
        }
        val observedAt = computerMoveTime.completedAt.get()
        assertTrue("computer move timestamp must be captured", observedAt > 0L)
        val earliestExpectedNanos = (remainingNanos - PROBE_MARGIN_NANOS).coerceAtLeast(0L)
        val latestExpectedNanos = minOf(MAX_COMPLETION_BEFORE_RESTART_NANOS, remainingNanos + COMPLETION_JITTER_NANOS)
        val elapsedAfterResume = observedAt - resumedAt
        assertTrue("O ran before the retained remaining delay", elapsedAfterResume >= earliestExpectedNanos)
        assertTrue("O did not complete within the retained remaining window", elapsedAfterResume <= latestExpectedNanos)
    }

    private class HostLifecycleTimestamps(private val application: Application) : Application.ActivityLifecycleCallbacks {
        val pausedAt = AtomicLong(0L)
        val resumedAt = AtomicLong(0L)

        override fun onActivityPaused(activity: Activity) {
            if (activity is MainActivity) pausedAt.set(SystemClock.elapsedRealtimeNanos())
        }

        override fun onActivityResumed(activity: Activity) {
            if (activity is MainActivity) resumedAt.set(SystemClock.elapsedRealtimeNanos())
        }

        override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit

        fun close() {
            application.unregisterActivityLifecycleCallbacks(this)
        }
    }

    private class ComputerMoveTimestamp(viewModel: GameViewModel) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val completedAt = AtomicLong(0L)

        init {
            scope.launch {
                viewModel.uiState.collect { state ->
                    if (state.board.any { it == Mark.O }) {
                        completedAt.compareAndSet(0L, SystemClock.elapsedRealtimeNanos())
                    }
                }
            }
        }

        fun close() = scope.cancel()
    }

    private companion object {
        const val COMPUTER_DELAY_NANOS = 1_000_000_000L
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val PROBE_MARGIN_NANOS = 150_000_000L
        const val COMPLETION_JITTER_NANOS = 300_000_000L
        const val MAX_COMPLETION_BEFORE_RESTART_NANOS = 950_000_000L
    }
}
