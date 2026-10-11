package com.edu.unal.tictactoe

import android.os.SystemClock
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import com.edu.unal.tictactoe.ui.navigation.AppScreen
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val COMPUTER_DELAY_MILLIS = 1_000L
private const val FOREGROUND_BEFORE_LEAVING_MILLIS = 400L
private const val LONGER_THAN_DELAY_MILLIS = 1_500L
private const val JITTER_MILLIS = 150L

/**
 * The local mode must not advance while its screen is not shown. Uses the real Activity and the
 * real Back action. Sound is not asserted directly: the player is a private detail of the host, and
 * move sounds are only emitted together with a move, so an unchanged board is the observable proxy.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityLocalPauseTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var game: GameViewModel
    private lateinit var navigation: NavigationViewModel
    private val preferencesFixture = GamePreferencesTestFixture()

    @Before
    fun launch() {
        preferencesFixture.prepare(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            game = ViewModelProvider(it)[GameViewModel::class.java]
            navigation = ViewModelProvider(it)[NavigationViewModel::class.java]
        }
        composeRule.waitForIdle()
    }

    @After
    fun close() {
        try {
            scenario.close()
        } finally {
            preferencesFixture.cleanup()
        }
    }

    @Test
    fun pendingTurnDoesNotAdvanceOnStartAndResumesFromRemainingTimeOnReturn() {
        chooseLocal()
        val movedAt = startPendingTurnAndConsumeSomeTime()
        val leftWith = game.uiState.value

        pressBackAsUser()
        awaitScreen(AppScreen.Start)
        val foregroundUpper = SystemClock.elapsedRealtime() - movedAt
        SystemClock.sleep(LONGER_THAN_DELAY_MILLIS)

        assertSameGame(leftWith, game.uiState.value)
        assertReturnToLocalCompletesOnceAfterRemainingTime(leftWith, foregroundUpper)
    }

    @Test
    fun pendingTurnDoesNotAdvanceInProvisionalLobbyAndResumesFromRemainingTimeOnReturn() {
        chooseLocal()
        val movedAt = startPendingTurnAndConsumeSomeTime()
        val leftWith = game.uiState.value

        pressBackAsUser()
        awaitScreen(AppScreen.Start)
        val foregroundUpper = SystemClock.elapsedRealtime() - movedAt
        composeRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        awaitScreen(AppScreen.Lobby)
        SystemClock.sleep(LONGER_THAN_DELAY_MILLIS)
        assertSameGame(leftWith, game.uiState.value)

        pressBackAsUser()
        awaitScreen(AppScreen.Start)
        assertReturnToLocalCompletesOnceAfterRemainingTime(leftWith, foregroundUpper)
    }

    @Test
    fun resumingTheActivityOnStartDoesNotResumeTheLocalTurn() {
        chooseLocal()
        startPendingTurnAndConsumeSomeTime()
        val leftWith = game.uiState.value
        pressBackAsUser()
        awaitScreen(AppScreen.Start)

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
        SystemClock.sleep(LONGER_THAN_DELAY_MILLIS)

        assertSameGame(leftWith, game.uiState.value)
        assertEquals(AppScreen.Start, navigation.currentScreen.value)
    }

    @Test
    fun backgroundingOnLocalPausesAndResumeContinuesTheTurn() {
        chooseLocal()
        val movedAt = startPendingTurnAndConsumeSomeTime()
        val leftWith = game.uiState.value

        scenario.moveToState(Lifecycle.State.CREATED)
        val foregroundUpper = SystemClock.elapsedRealtime() - movedAt
        SystemClock.sleep(LONGER_THAN_DELAY_MILLIS)
        assertSameGame(leftWith, game.uiState.value)

        val resumedAt = SystemClock.elapsedRealtime()
        scenario.moveToState(Lifecycle.State.RESUMED)
        assertCompletesOnceAfterRemainingTime(leftWith, foregroundUpper, resumedAt, resumedAt)
    }

    private fun chooseLocal() {
        composeRule.onNodeWithText(context.getString(R.string.start_play_local)).performClick()
        awaitScreen(AppScreen.Local)
    }

    /** Human plays and O's pending turn consumes some time; returns when the human moved. */
    private fun startPendingTurnAndConsumeSomeTime(): Long {
        var movedAt = 0L
        scenario.onActivity {
            movedAt = SystemClock.elapsedRealtime()
            game.onHumanMove(0)
        }
        assertEquals(Turn.Computer, game.uiState.value.turn)
        SystemClock.sleep(FOREGROUND_BEFORE_LEAVING_MILLIS)
        assertEquals(0, game.uiState.value.board.count { it == Mark.O })
        return movedAt
    }

    private fun assertReturnToLocalCompletesOnceAfterRemainingTime(
        leftWith: GameUiState,
        foregroundUpperMillis: Long,
    ) {
        val beforeClick = SystemClock.elapsedRealtime()
        composeRule.onNodeWithText(context.getString(R.string.start_play_local)).performClick()
        val afterClick = SystemClock.elapsedRealtime()
        assertCompletesOnceAfterRemainingTime(leftWith, foregroundUpperMillis, beforeClick, afterClick)
    }

    /**
     * [foregroundUpperMillis] bounds the time the turn could have consumed before the screen
     * was left, so the remaining time is at least the delay minus it; the consumed time is at least
     * [FOREGROUND_BEFORE_LEAVING_MILLIS], so a restarted count would take the whole delay.
     */
    private fun assertCompletesOnceAfterRemainingTime(
        leftWith: GameUiState,
        foregroundUpperMillis: Long,
        beforeReturn: Long,
        afterReturn: Long,
    ) {
        val remainingLower = COMPUTER_DELAY_MILLIS - foregroundUpperMillis
        composeRule.waitUntil(timeoutMillis = 3_000) {
            game.uiState.value.board.count { it == Mark.O } == 1
        }
        val completedAt = SystemClock.elapsedRealtime()
        assertTrue(
            "O moved ${completedAt - afterReturn} ms after returning; remaining was at least $remainingLower ms",
            completedAt - afterReturn >= remainingLower - JITTER_MILLIS,
        )
        // True remaining is at most 600 ms; a restarted count would need the full delay.
        val restartBound = COMPUTER_DELAY_MILLIS - FOREGROUND_BEFORE_LEAVING_MILLIS + 2 * JITTER_MILLIS
        assertTrue(
            "O moved ${completedAt - beforeReturn} ms after returning; the count must not restart",
            completedAt - beforeReturn < restartBound,
        )
        val completed = game.uiState.value
        assertEquals(Mark.X, completed.board[0])
        assertEquals(1, completed.board.count { it == Mark.X })
        assertEquals(leftWith.score, completed.score)
        SystemClock.sleep(LONGER_THAN_DELAY_MILLIS)
        assertEquals(completed.board, game.uiState.value.board)
    }

    private fun awaitScreen(screen: AppScreen) {
        composeRule.waitUntil(timeoutMillis = 5_000) { navigation.currentScreen.value == screen }
        composeRule.waitForIdle()
    }

    private fun assertSameGame(expected: GameUiState, actual: GameUiState) {
        assertEquals(expected.board, actual.board)
        assertEquals(expected.turn, actual.turn)
        assertEquals(expected.result, actual.result)
        assertEquals(expected.score, actual.score)
    }
}
