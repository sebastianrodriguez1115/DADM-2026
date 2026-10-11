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
import com.edu.unal.tictactoe.data.preferences.GamePreferenceSchema
import com.edu.unal.tictactoe.data.preferences.SharedPreferencesGamePreferences
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicLong

/**
 * Production wiring of the session snapshot: the real MainActivity dumps its real
 * SavedStateRegistry (through a real Parcel), and a brand-new host with a new ViewModelStore and
 * a new SavedStateRegistryController builds a new GameViewModel through the production factory.
 * Activity recreation in place is deliberately not used: it retains the ViewModelStore.
 */
@RunWith(AndroidJUnit4::class)
class GameViewModelSavedStateWiringTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val fixture = GamePreferencesTestFixture()
    private var scenario: ActivityScenario<MainActivity>? = null
    private var activityViewModel: GameViewModel? = null
    private val hosts = mutableListOf<RestoredSessionHost>()

    @After
    fun cleanup() {
        try {
            try {
                instrumentation.runOnMainSync { hosts.forEach(RestoredSessionHost::dispose) }
                hosts.clear()
            } finally {
                scenario?.close()
            }
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun activeHumanTurnWithMutedSoundIsRestoredIntoANewViewModel() {
        fixture.prepare(context, GamePreferencesTestFixture.Seed(xWins = 4, oWins = 5, ties = 6, difficultyCode = 1))
        launch()
        val original = checkNotNull(activityViewModel)
        onActivity { original.toggleSound() }
        humanMove(0)
        awaitHumanOrTerminal(original)
        val expected = original.uiState.value
        assertEquals(Turn.Human, expected.turn)
        assertEquals(GameResult.InProgress, expected.result)
        assertEquals(false, expected.soundEnabled)

        val restored = restore(captureBundleAtCreated())

        assertNotSame(original, restored)
        val actual = restored.uiState.value
        assertEquals(expected.board, actual.board)
        assertEquals(expected.turn, actual.turn)
        assertEquals(expected.result, actual.result)
        assertEquals(expected.starter, actual.starter)
        assertEquals(expected.nextStarter, actual.nextStarter)
        assertEquals(false, actual.soundEnabled)
        val prefs = SharedPreferencesGamePreferences(context)
        assertEquals(Score(4, 5, 6), actual.score)
        assertEquals(prefs.readScore(), actual.score)
        assertEquals(DifficultyLevel.Hard, actual.difficulty)
        assertEquals(prefs.readDifficulty(), actual.difficulty)
    }

    @Test
    fun alreadyScoredTerminalGameIsRestoredWithoutScoringAgain() {
        fixture.prepare(context)
        launch()
        val original = checkNotNull(activityViewModel)
        playUntilTerminal(original)
        val expected = original.uiState.value
        assertEquals(1, scoreTotal(expected.score))
        val prefs = SharedPreferencesGamePreferences(context)
        val persistedScore = prefs.readScore()
        val persistedDifficulty = prefs.readDifficulty()
        assertEquals(persistedScore, expected.score)
        val rawBefore = rawPreferences()

        val restored = restore(captureBundleAtCreated())

        assertNotSame(original, restored)
        val actual = restored.uiState.value
        assertTrue(actual.result != GameResult.InProgress)
        assertEquals(expected.result, actual.result)
        assertEquals(expected.board, actual.board)
        assertEquals(expected.turn, actual.turn)
        assertEquals(persistedScore, actual.score)
        assertEquals(1, scoreTotal(actual.score))
        assertEquals(persistedDifficulty, actual.difficulty)
        // Resuming must not touch the terminal game either.
        resumeHost(restored)
        SystemClock.sleep(300)
        assertEquals(persistedScore, SharedPreferencesGamePreferences(context).readScore())
        assertEquals(persistedDifficulty, SharedPreferencesGamePreferences(context).readDifficulty())
        assertEquals(rawBefore, rawPreferences())
        assertEquals(expected.board, restored.uiState.value.board)
    }

    @Test
    fun pendingComputerTurnIsRestoredLogicallyAndRunsOnceOnlyAfterHostResumed() {
        fixture.prepare(context)
        launch()
        val original = checkNotNull(activityViewModel)
        humanMove(0)
        val bundle = captureBundleAtCreated()
        val atCapture = original.uiState.value
        assertEquals(
            "Timing precondition: O must not have moved before the activity was stopped",
            0,
            atCapture.board.count { it == Mark.O },
        )

        val restored = restore(bundle)
        val sink = RecordingSink()
        onMain { restored.registerMoveSoundSink(sink, sink) }

        val initial = restored.uiState.value
        assertEquals(Mark.X, initial.board[0])
        assertEquals(1, initial.board.count { it != Mark.Empty })
        assertEquals(0, initial.board.count { it == Mark.O })
        assertEquals(Turn.Computer, initial.turn)
        assertEquals(GameResult.InProgress, initial.result)

        // A full O delay passes without the host reaching RESUMED: the logical turn must not run.
        SystemClock.sleep(1_200)
        assertEquals(0, restored.uiState.value.board.count { it == Mark.O })
        assertEquals(Turn.Computer, restored.uiState.value.turn)
        assertEquals(emptyList<Player>(), sink.plays())

        resumeHost(restored)
        composeTestRule.waitUntil(timeoutMillis = 4_000) {
            restored.uiState.value.board.count { it == Mark.O } == 1
        }
        SystemClock.sleep(1_200)
        val finalState = restored.uiState.value
        assertEquals(Mark.X, finalState.board[0])
        assertEquals(1, finalState.board.count { it == Mark.X })
        assertEquals("exactly one O", 1, finalState.board.count { it == Mark.O })
        assertEquals(Turn.Human, finalState.turn)
        // Only the genuinely new O move sounds; nothing already on the board is replayed.
        assertEquals(listOf(Player.O), sink.plays())
    }

    @Test
    fun restoringAndResumingNeverReplaysSoundForMarksAlreadyOnTheBoard() {
        fixture.prepare(context)
        launch()
        val original = checkNotNull(activityViewModel)
        humanMove(0)
        awaitHumanOrTerminal(original)
        humanMove(original.uiState.value.board.indexOfFirst { it == Mark.Empty })
        awaitHumanOrTerminal(original)
        val bundle = captureBundleAtCreated()
        val expected = original.uiState.value
        assertTrue(expected.board.count { it != Mark.Empty } >= 4)

        val sink = RecordingSink()
        val vm = restore(bundle)
        onMain { vm.registerMoveSoundSink(sink, sink) }
        assertEquals(expected.board, vm.uiState.value.board)
        assertEquals(emptyList<Player>(), sink.plays())
        resumeHost(vm)
        SystemClock.sleep(300)
        assertEquals(emptyList<Player>(), sink.plays())
        assertEquals(0, sink.stops())
        assertEquals(expected.board, vm.uiState.value.board)
    }

    @Test
    fun absentSavedStateCreatesFreshSessionWithCurrentPreferences() {
        fixture.prepare(context, GamePreferencesTestFixture.Seed(xWins = 2, oWins = 3, ties = 1, difficultyCode = 0))
        var restored: GameViewModel? = null
        onMain { restored = RestoredSessionHost(null).also(hosts::add).createViewModel() }
        val state = checkNotNull(restored).uiState.value

        assertEquals(List(9) { Mark.Empty }, state.board)
        assertEquals(Turn.Human, state.turn)
        assertEquals(GameResult.InProgress, state.result)
        assertEquals(Player.X, state.starter)
        assertEquals(Player.O, state.nextStarter)
        assertEquals(true, state.soundEnabled)
        assertEquals(Score(2, 3, 1), state.score)
        assertEquals(DifficultyLevel.Easy, state.difficulty)
    }

    @Test
    fun pauseCapturesTheRemainingOfOAndTheRestoredTurnCompletesNearIt() {
        fixture.prepare(context)
        launch()
        val original = checkNotNull(activityViewModel)
        val lifecycleTimes = PauseTimestamp()
        val application = context.applicationContext as Application
        application.registerActivityLifecycleCallbacks(lifecycleTimes)
        try {
            val moveStartedAt = AtomicLong(0L)
            onActivity {
                moveStartedAt.set(SystemClock.elapsedRealtimeNanos())
                original.onHumanMove(0)
            }
            SystemClock.sleep(500)
            // Real pause (MainActivity.onPause) happens on the way to CREATED, before the state is saved.
            val bundle = captureBundleAtCreated()
            val foregroundNanos = lifecycleTimes.pausedAt.get() - moveStartedAt.get()
            val foregroundMillis = foregroundNanos / NANOS_PER_MILLISECOND
            assertTrue("measured foreground interval was $foregroundMillis ms", foregroundMillis in 350L..800L)
            assertEquals(
                "Timing precondition: O must not have moved before the pause",
                0,
                original.uiState.value.board.count { it == Mark.O },
            )
            val remainingNanos = COMPUTER_DELAY_NANOS - foregroundNanos

            val restored = restore(bundle)
            val completedAt = AtomicLong(0L)
            val observer = CompletionTimestamp(restored, completedAt)
            try {
                assertEquals(0, restored.uiState.value.board.count { it == Mark.O })
                val resumedAt = AtomicLong(0L)
                onMain {
                    resumedAt.set(SystemClock.elapsedRealtimeNanos())
                    restored.onHostResumed()
                }
                composeTestRule.waitUntil(timeoutMillis = 2_000) { completedAt.get() > 0L }
                val elapsedNanos = completedAt.get() - resumedAt.get()
                val detail = "remaining=${remainingNanos / NANOS_PER_MILLISECOND} ms " +
                    "elapsedAfterResume=${elapsedNanos / NANOS_PER_MILLISECOND} ms " +
                    "foreground=$foregroundMillis ms"
                assertTrue("O must not complete before the captured remaining ($detail)", elapsedNanos >= remainingNanos - PROBE_MARGIN_NANOS)
                assertTrue("O must complete near the captured remaining ($detail)", elapsedNanos <= remainingNanos + COMPLETION_JITTER_NANOS)
                assertTrue("O must complete clearly before a full second ($detail)", elapsedNanos <= MAX_COMPLETION_BEFORE_RESTART_NANOS)
                SystemClock.sleep(1_100)
                assertEquals("exactly one O", 1, restored.uiState.value.board.count { it == Mark.O })
            } finally {
                observer.close()
            }
        } finally {
            application.unregisterActivityLifecycleCallbacks(lifecycleTimes)
        }
    }

    @Test
    fun soundIsNeverPersistedAndAHostWithoutSavedStateStartsWithSoundOn() {
        fixture.prepare(context, GamePreferencesTestFixture.Seed(xWins = 1, oWins = 2, ties = 3, difficultyCode = 1))
        launch()
        val original = checkNotNull(activityViewModel)
        onActivity { original.toggleSound() }
        assertEquals(false, original.uiState.value.soundEnabled)
        humanMove(0)
        awaitHumanOrTerminal(original)

        val schemaKeys = setOf(
            GamePreferenceSchema.SCORE_X_WINS,
            GamePreferenceSchema.SCORE_O_WINS,
            GamePreferenceSchema.SCORE_TIES,
            GamePreferenceSchema.DIFFICULTY_CODE,
        )
        val stored = rawPreferences()
        assertEquals("only score and difficulty keys exist: ${stored.keys}", schemaKeys, stored.keys)
        assertTrue(
            "no key mentions sound: ${stored.keys}",
            stored.keys.none { it.contains("sound", ignoreCase = true) || it.contains("mute", ignoreCase = true) },
        )

        val fresh = restore(null)
        assertEquals(true, fresh.uiState.value.soundEnabled)
    }

    private fun launch() {
        val launched = ActivityScenario.launch(MainActivity::class.java)
        scenario = launched
        launched.onActivity { activityViewModel = ViewModelProvider(it)[GameViewModel::class.java] }
        composeTestRule.waitForIdle()
        composeTestRule.startLocalGame()
    }

    private fun onActivity(block: () -> Unit) {
        checkNotNull(scenario).onActivity { block() }
    }

    private fun humanMove(location: Int) {
        val vm = checkNotNull(activityViewModel)
        onActivity { vm.onHumanMove(location) }
    }

    private fun awaitHumanOrTerminal(vm: GameViewModel) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val state = vm.uiState.value
            state.turn == Turn.Human || state.result != GameResult.InProgress
        }
        // The state is published and the side effects (score write, snapshot, sound) are issued in
        // the same main-thread event. Draining the main looper guarantees that event has finished
        // before the test thread reads preferences or sinks.
        instrumentation.runOnMainSync {}
    }

    private fun playUntilTerminal(vm: GameViewModel) {
        var humanMoves = 0
        while (vm.uiState.value.result == GameResult.InProgress) {
            assertTrue("game did not finish within five human moves", humanMoves < 5)
            awaitHumanOrTerminal(vm)
            if (vm.uiState.value.result != GameResult.InProgress) break
            val location = vm.uiState.value.board.indexOfFirst { it == Mark.Empty }
            assertTrue(location >= 0)
            humanMove(location)
            humanMoves++
            awaitHumanOrTerminal(vm)
        }
    }

    /**
     * Real stop, then the real Activity writes its real SavedStateRegistry into a new Bundle,
     * which goes through a real Parcel. The scenario is closed afterwards (state lost).
     */
    private fun captureBundleAtCreated(): Bundle {
        val current = checkNotNull(scenario)
        current.moveToState(Lifecycle.State.CREATED)
        var parceled: Bundle? = null
        current.onActivity { activity ->
            val out = Bundle()
            instrumentation.callActivityOnSaveInstanceState(activity, out)
            parceled = parcelRoundTrip(out, MainActivity::class.java.classLoader)
        }
        current.close()
        scenario = null
        return checkNotNull(parceled)
    }

    private fun restore(bundle: Bundle?): GameViewModel {
        var created: GameViewModel? = null
        onMain { created = RestoredSessionHost(bundle).also(hosts::add).createViewModel() }
        return checkNotNull(created)
    }

    private fun resumeHost(vm: GameViewModel) = onMain { vm.onHostResumed() }

    private fun onMain(block: () -> Unit) {
        var failure: Throwable? = null
        instrumentation.runOnMainSync {
            try {
                block()
            } catch (t: Throwable) {
                failure = t
            }
        }
        failure?.let { throw it }
    }

    private fun rawPreferences(): Map<String, Any?> = context.applicationContext
        .getSharedPreferences(
            GamePreferenceSchema.FILE_NAME,
            android.content.Context.MODE_PRIVATE,
        ).all.toMap()

    private fun scoreTotal(score: Score): Int = score.xWins + score.oWins + score.ties

    /** Records the real `onActivityPaused` callback, which runs inside `MainActivity.onPause`. */
    private class PauseTimestamp : Application.ActivityLifecycleCallbacks {
        val pausedAt = AtomicLong(0L)

        override fun onActivityPaused(activity: Activity) {
            if (activity is MainActivity) pausedAt.set(SystemClock.elapsedRealtimeNanos())
        }

        override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    /** Stamps the first moment an O mark shows up in the state, on the main thread. */
    private class CompletionTimestamp(viewModel: GameViewModel, completedAt: AtomicLong) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        init {
            scope.launch {
                viewModel.uiState.collect { state ->
                    if (state.board.any { it == Mark.O }) completedAt.compareAndSet(0L, SystemClock.elapsedRealtimeNanos())
                }
            }
        }

        fun close() = scope.cancel()
    }

    private class RecordingSink : MoveSoundSink {
        private val played = mutableListOf<Player>()
        private var stopCount = 0

        override fun play(player: Player) {
            synchronized(this) { played.add(player) }
        }

        override fun stop() {
            synchronized(this) { stopCount++ }
        }

        fun plays(): List<Player> = synchronized(this) { played.toList() }

        fun stops(): Int = synchronized(this) { stopCount }
    }

    private companion object {
        const val COMPUTER_DELAY_NANOS = 1_000_000_000L
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val PROBE_MARGIN_NANOS = 150_000_000L
        const val COMPLETION_JITTER_NANOS = 300_000_000L
        const val MAX_COMPLETION_BEFORE_RESTART_NANOS = 950_000_000L
    }
}
