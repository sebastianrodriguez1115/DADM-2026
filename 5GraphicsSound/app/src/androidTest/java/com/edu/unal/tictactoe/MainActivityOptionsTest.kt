package com.edu.unal.tictactoe

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.AboutDialogTags
import com.edu.unal.tictactoe.ui.game.DifficultyDialogTags
import com.edu.unal.tictactoe.ui.game.ExitConfirmationDialogTags
import com.edu.unal.tictactoe.ui.game.GameMenuTags
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val NEW_GAME_SHORTCUT_TAG = "tic_tac_toe_new_game"
private const val CELL_TAG_PREFIX = "tic_tac_toe_cell_"

@RunWith(AndroidJUnit4::class)
class MainActivityOptionsTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var activityViewModel: GameViewModel

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun launchActivity() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        activityViewModel = viewModelFromCurrentActivity()
        composeTestRule.waitForIdle()
    }

    @After
    fun closeActivity() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @Test
    fun exitDialogBlocksBackAndOutsideThenNoPreservesTheActiveSession() {
        clickCell(0)
        val before = snapshot()
        assertEquals(GameResult.InProgress, before.result)

        openExitDialog()
        assertDialogVisible(ExitConfirmationDialogTags.DIALOG)
        composeTestRule.waitForIdle()
        pressBack()
        composeTestRule.waitForIdle()
        assertDialogVisible(ExitConfirmationDialogTags.DIALOG)

        tapOutsideDialog()
        assertDialogVisible(ExitConfirmationDialogTags.DIALOG)
        assertSameSnapshot(before, snapshot())

        clickTag(ExitConfirmationDialogTags.CANCEL)
        awaitTag(ExitConfirmationDialogTags.DIALOG, present = false)

        assertEquals(Lifecycle.State.RESUMED, scenario.state)
        assertSame(activityViewModel, viewModelFromCurrentActivity())
        assertSameSnapshot(before, snapshot())
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .assertIsDisplayed()
    }

    @Test
    fun exitDialogConfirmYesFinishesTheRealActivity() {
        openExitDialog()
        assertDialogVisible(ExitConfirmationDialogTags.DIALOG)

        clickTag(ExitConfirmationDialogTags.CONFIRM)
        awaitActivityDestroyed()
    }

    @Test
    fun impossibleSelectionAndGateSurviveRecreationBackgroundAndMenuBack() {
        selectDifficulty(DifficultyLevel.Impossible)
        val selected = snapshot()
        assertEquals(DifficultyLevel.Impossible, selected.difficulty)
        assertTrue(selected.board.all { it == Mark.Empty })
        assertTrue(selected.canChangeDifficulty)

        clickCell(4)
        val active = snapshot()
        assertEquals(Mark.X, active.board[4])
        assertEquals(1, active.board.count { it == Mark.O })
        assertEquals(Turn.Human, active.turn)
        assertEquals(GameResult.InProgress, active.result)
        assertEquals(DifficultyLevel.Impossible, active.difficulty)
        assertFalse(active.canChangeDifficulty)

        scenario.recreate()
        composeTestRule.waitForIdle()
        assertSame(activityViewModel, viewModelFromCurrentActivity())
        assertSameSnapshot(active, snapshot())

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.waitForIdle()
        assertSame(activityViewModel, viewModelFromCurrentActivity())
        assertSameSnapshot(active, snapshot())

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY).assertIsNotEnabled()
        composeTestRule.waitForIdle()
        pressBack()
        awaitTag(GameMenuTags.MENU, present = false)
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .assertIsDisplayed()
        assertSameSnapshot(active, snapshot())
    }

    @Test
    fun terminalGameEnablesDifficultyAndKeepsNewGameRulesAndScore() {
        val terminal = playUntilTerminal()
        assertTrue(terminal.result != GameResult.InProgress)
        assertEquals(1, scoreTotal(terminal))
        assertTrue(terminal.canChangeDifficulty)

        selectDifficulty(DifficultyLevel.Hard)
        val afterSelection = snapshot()
        assertEquals(DifficultyLevel.Hard, afterSelection.difficulty)
        assertEquals(terminal.board, afterSelection.board)
        assertEquals(terminal.result, afterSelection.result)
        assertEquals(terminal.score, afterSelection.score)

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.NEW_GAME).assertIsEnabled()
        clickTag(GameMenuTags.NEW_GAME)
        awaitTag(GameMenuTags.MENU, present = false)
        val oOpening = snapshot()
        assertEquals(Player.O, oOpening.starter)
        assertEquals(Player.X, oOpening.nextStarter)
        assertEquals(1, oOpening.board.count { it == Mark.O })
        assertEquals(0, oOpening.board.count { it == Mark.X })
        assertEquals(1, scoreTotal(oOpening))
        assertEquals(DifficultyLevel.Hard, oOpening.difficulty)
        assertFalse(oOpening.canChangeDifficulty)

        clickTag(NEW_GAME_SHORTCUT_TAG, scroll = true)
        val xOpening = snapshot()
        assertEquals(Player.X, xOpening.starter)
        assertEquals(Player.O, xOpening.nextStarter)
        assertTrue(xOpening.board.all { it == Mark.Empty })
        assertTrue(xOpening.canChangeDifficulty)
        assertEquals(1, scoreTotal(xOpening))
        assertEquals(DifficultyLevel.Hard, xOpening.difficulty)
    }

    @Test
    fun newSessionAfterExitStartsInExpertWithoutPersistence() {
        selectDifficulty(DifficultyLevel.Impossible)
        clickCell(4)
        assertEquals(DifficultyLevel.Impossible, snapshot().difficulty)
        val oldViewModel = activityViewModel

        openExitDialog()
        clickTag(ExitConfirmationDialogTags.CONFIRM)
        awaitActivityDestroyed()

        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val newViewModel = viewModelFromCurrentActivity()
        composeTestRule.waitForIdle()

        assertNotSame(oldViewModel, newViewModel)
        val fresh = newViewModel.uiState.value
        assertEquals(DifficultyLevel.Expert, fresh.difficulty)
        assertTrue(fresh.soundEnabled)
        assertTrue(fresh.board.all { it == Mark.Empty })
        assertEquals(0, scoreTotal(fresh))
        assertTrue(fresh.canChangeDifficulty)
        assertEquals(Player.X, fresh.starter)
    }

    @Test
    fun aboutDialogDuringAnActiveGameShowsTheAuthorAndKeepsTheSnapshot() {
        clickCell(0)
        val before = snapshot()

        openMenu()
        clickTag(GameMenuTags.ABOUT)
        awaitTag(GameMenuTags.MENU, present = false)
        awaitTag(AboutDialogTags.DIALOG, present = true)

        composeTestRule.onNodeWithText(context.getString(R.string.about_title)).assertExists()
        composeTestRule.onNodeWithText("Juan Sebastian Rodriguez").assertExists()
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.about_image_description),
        ).assertIsDisplayed()
        assertSameSnapshot(before, snapshot())

        clickTag(AboutDialogTags.ACCEPT)
        awaitTag(AboutDialogTags.DIALOG, present = false)
        assertSameSnapshot(before, snapshot())
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
    }

    private fun snapshot(): GameUiState = activityViewModel.uiState.value

    private fun viewModelFromCurrentActivity(): GameViewModel {
        var viewModel: GameViewModel? = null
        scenario.onActivity { viewModel = ViewModelProvider(it)[GameViewModel::class.java] }
        return checkNotNull(viewModel)
    }

    private fun openMenu() {
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
    }

    private fun clickTag(tag: String, scroll: Boolean = false) {
        var node = composeTestRule.onNodeWithTag(tag)
        if (scroll) {
            node = node.performScrollTo().assertIsEnabled()
        }
        node.assertIsDisplayed().performClick()
        if (tag == GameMenuTags.NEW_GAME || tag == NEW_GAME_SHORTCUT_TAG) {
            awaitHumanOrTerminal()
        }
    }

    private fun openExitDialog() {
        openMenu()
        clickTag(GameMenuTags.EXIT)
        awaitTag(GameMenuTags.MENU, present = false)
        awaitTag(ExitConfirmationDialogTags.DIALOG, present = true)
    }

    private fun selectDifficulty(level: DifficultyLevel) {
        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY).assertIsEnabled()
        clickTag(GameMenuTags.DIFFICULTY)
        awaitTag(GameMenuTags.MENU, present = false)
        awaitTag(DifficultyDialogTags.DIALOG, present = true)
        DifficultyLevel.entries.forEach {
            composeTestRule.onNodeWithTag(DifficultyDialogTags.option(it)).assertExists()
        }
        composeTestRule.onNodeWithTag(DifficultyDialogTags.option(level))
            .performScrollTo().performClick()
        awaitTag(DifficultyDialogTags.DIALOG, present = false)
    }

    private fun clickCell(location: Int) {
        clickTag(CELL_TAG_PREFIX + location, scroll = true)
        awaitHumanOrTerminal()
    }

    private fun playUntilTerminal(): GameUiState {
        var humanMoves = 0
        while (snapshot().result == GameResult.InProgress) {
            assertTrue("game did not finish within five human moves", humanMoves < 5)
            val state = snapshot()
            assertEquals(Turn.Human, state.turn)
            val location = state.board.indexOfFirst { it == Mark.Empty }
            assertTrue("active game has no free cell", location >= 0)
            clickCell(location)
            humanMoves++
        }
        return snapshot()
    }

    private fun awaitHumanOrTerminal() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val state = snapshot()
            state.turn == Turn.Human || state.result != GameResult.InProgress
        }
    }

    private fun assertDialogVisible(tag: String) {
        composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun awaitTag(tag: String, present: Boolean) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().run {
                if (present) isNotEmpty() else isEmpty()
            }
        }
    }

    private fun awaitActivityDestroyed() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (scenario.state != Lifecycle.State.DESTROYED && SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            SystemClock.sleep(25)
        }
        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
    }

    private fun assertSameSnapshot(expected: GameUiState, actual: GameUiState) {
        assertEquals(expected.board, actual.board)
        assertEquals(expected.turn, actual.turn)
        assertEquals(expected.result, actual.result)
        assertEquals(expected.starter, actual.starter)
        assertEquals(expected.nextStarter, actual.nextStarter)
        assertEquals(expected.score, actual.score)
        assertEquals(expected.difficulty, actual.difficulty)
        assertEquals(expected.soundEnabled, actual.soundEnabled)
        assertEquals(expected.canChangeDifficulty, actual.canChangeDifficulty)
    }

    private fun scoreTotal(state: GameUiState): Int =
        state.score.xWins + state.score.oWins + state.score.ties

    private fun tapOutsideDialog() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val downTime = SystemClock.uptimeMillis()
        val y = context.resources.displayMetrics.heightPixels / 2f
        val down = touchEvent(MotionEvent.ACTION_DOWN, downTime, downTime, 4f, y)
        val up = touchEvent(MotionEvent.ACTION_UP, downTime, downTime + 16, 4f, y)
        try {
            assertTrue(instrumentation.uiAutomation.injectInputEvent(down, true))
            SystemClock.sleep(16)
            assertTrue(instrumentation.uiAutomation.injectInputEvent(up, true))
        } finally {
            down.recycle()
            up.recycle()
        }
        composeTestRule.waitForIdle()
    }

    private fun touchEvent(action: Int, downTime: Long, eventTime: Long, x: Float, y: Float) =
        MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
            .apply { source = InputDevice.SOURCE_TOUCHSCREEN }
}
