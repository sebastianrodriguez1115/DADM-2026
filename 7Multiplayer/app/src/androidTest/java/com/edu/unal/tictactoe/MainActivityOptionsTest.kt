package com.edu.unal.tictactoe

import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
import com.edu.unal.tictactoe.data.preferences.GamePreferenceSchema
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.AboutDialogTags
import com.edu.unal.tictactoe.ui.game.DifficultyDialogTags
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
private const val BOARD_TAG = "tic_tac_toe_board"

@RunWith(AndroidJUnit4::class)
class MainActivityOptionsTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var activityViewModel: GameViewModel
    private val preferencesFixture = GamePreferencesTestFixture()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun launchActivity() {
        preferencesFixture.prepare(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        activityViewModel = viewModelFromCurrentActivity()
        composeTestRule.waitForIdle()
        composeTestRule.startLocalGame()
    }

    @After
    fun closeActivity() {
        try {
            if (::scenario.isInitialized) scenario.close()
        } finally {
            preferencesFixture.cleanup()
        }
    }

    @Test
    fun menuOffersNoWayToQuitAndNoQuitConfirmationAppears() {
        clickCell(0)
        val before = snapshot()

        openMenu()
        assertEquals(
            5,
            composeTestRule.onAllNodes(
                hasAnyAncestor(hasTestTag(GameMenuTags.MENU)) and hasClickAction(),
            ).fetchSemanticsNodes().size,
        )
        composeTestRule.onNodeWithTag(GameMenuTags.RESET_SCORES).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Salir").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("¿Seguro que quieres salir?").assertCountEquals(0)

        pressBack()
        awaitTag(GameMenuTags.MENU, present = false)
        composeTestRule.onAllNodesWithText("Salir").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("¿Seguro que quieres salir?").assertCountEquals(0)
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
        assertSameSnapshot(before, snapshot())
    }

    @Test
    fun backWithMenuOpenClosesItWithoutChangingTheGame() {
        clickCell(0)
        val before = snapshot()

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.MENU).assertIsDisplayed()
        pressBack()
        awaitTag(GameMenuTags.MENU, present = false)

        assertEquals(Lifecycle.State.RESUMED, scenario.state)
        assertSame(activityViewModel, viewModelFromCurrentActivity())
        assertSameSnapshot(before, snapshot())
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .assertIsDisplayed()
    }

    @Test
    fun backWithoutOverlayShowsStartKeepsTheLocalGameAndDoesNotFinishTheTask() {
        clickCell(0)
        val before = snapshot()
        assertEquals(0, composeTestRule.onAllNodesWithTag(GameMenuTags.MENU).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithTag(DifficultyDialogTags.DIALOG).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithTag(AboutDialogTags.DIALOG).fetchSemanticsNodes().size)

        pressBackAsUser()
        val startEntry = context.getString(R.string.start_play_local)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(startEntry).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(startEntry).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.start_play_online)).assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithTag(BOARD_TAG).fetchSemanticsNodes().size)
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
        var finishing = true
        scenario.onActivity { finishing = it.isFinishing }
        assertFalse("Back on the local game must not finish the task", finishing)
        assertSame(activityViewModel, viewModelFromCurrentActivity())
        assertSameSnapshot(before, snapshot())

        composeTestRule.startLocalGame()

        assertSameSnapshot(before, snapshot())
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.cell_x, 1, 1))
            .assertExists()
    }

    @Test
    fun resetScoresFromTheRealMenuZeroesCountersKeepsTheFinishedGameAndSurvivesANewSession() {
        val terminal = playUntilTerminal()
        assertEquals(1, scoreTotal(terminal))

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.RESET_SCORES)
            .assertIsDisplayed().assertIsEnabled().performClick()
        awaitTag(GameMenuTags.MENU, present = false)
        composeTestRule.waitForIdle()

        val afterReset = snapshot()
        assertEquals(0, scoreTotal(afterReset))
        assertEquals(terminal.board, afterReset.board)
        assertEquals(terminal.result, afterReset.result)
        assertEquals(terminal.turn, afterReset.turn)
        assertEquals(terminal.starter, afterReset.starter)
        assertEquals(terminal.nextStarter, afterReset.nextStarter)
        assertEquals(terminal.difficulty, afterReset.difficulty)
        assertScoreStored(xWins = 0, oWins = 0, ties = 0)

        scenario.onActivity { it.finish() }
        awaitActivityDestroyed()
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val fresh = viewModelFromCurrentActivity().uiState.value
        composeTestRule.waitForIdle()
        composeTestRule.startLocalGame()

        assertEquals(0, scoreTotal(fresh))
        assertTrue(fresh.board.all { it == Mark.Empty })
        assertScoreStored(xWins = 0, oWins = 0, ties = 0)
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
    fun newSessionAfterFinishLoadsPersistedDifficulty() {
        selectDifficulty(DifficultyLevel.Impossible)
        clickCell(4)
        assertEquals(DifficultyLevel.Impossible, snapshot().difficulty)
        val oldViewModel = activityViewModel

        scenario.onActivity { it.finish() }
        awaitActivityDestroyed()

        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val newViewModel = viewModelFromCurrentActivity()
        composeTestRule.waitForIdle()
        composeTestRule.startLocalGame()

        assertNotSame(oldViewModel, newViewModel)
        val fresh = newViewModel.uiState.value
        assertEquals(DifficultyLevel.Impossible, fresh.difficulty)
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
            .performScrollTo().assertIsDisplayed().performClick()
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

    private fun awaitTag(tag: String, present: Boolean) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().run {
                if (present) isNotEmpty() else isEmpty()
            }
        }
    }

    private fun assertScoreStored(xWins: Int, oWins: Int, ties: Int) {
        val stored = context.getSharedPreferences(GamePreferenceSchema.FILE_NAME, Context.MODE_PRIVATE)
        assertEquals(xWins, stored.getInt(GamePreferenceSchema.SCORE_X_WINS, -1))
        assertEquals(oWins, stored.getInt(GamePreferenceSchema.SCORE_O_WINS, -1))
        assertEquals(ties, stored.getInt(GamePreferenceSchema.SCORE_TIES, -1))
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
}
