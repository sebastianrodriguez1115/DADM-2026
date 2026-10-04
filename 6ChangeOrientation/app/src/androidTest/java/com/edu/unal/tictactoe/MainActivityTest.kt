package com.edu.unal.tictactoe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val NEW_GAME_TAG = "tic_tac_toe_new_game"

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var activityViewModel: GameViewModel
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun launchActivity() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            activityViewModel = ViewModelProvider(activity)[GameViewModel::class.java]
        }
        composeTestRule.waitForIdle()
    }

    @After
    fun closeActivity() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @Test
    fun humanMoveUsesActivityViewModelAndRecompositionDoesNotRepeatIt() {
        val initialViewModel = activityViewModel

        clickCell(0)

        val snapshot = snapshot(initialViewModel)
        assertEquals(Mark.X, snapshot.board[0])
        assertEquals(1, snapshot.board.count { it == Mark.O })
        assertEquals(Player.X, snapshot.starter)
        assertEquals(0, snapshot.score.xWins)
        assertEquals(0, snapshot.score.oWins)
        assertEquals(0, snapshot.score.ties)
        composeTestRule.onNodeWithContentDescription(cellDescription(R.string.cell_x, 1, 1))
            .assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.turn_human)).assertExists()

        repeat(3) {
            composeTestRule.waitForIdle()
            assertSameSnapshot(snapshot, snapshot(initialViewModel))
            assertSame(initialViewModel, viewModelFromCurrentActivity())
        }
    }

    @Test
    fun newGameActionStartsOOnceAndPreservesScoreAcrossRedraws() {
        val initialViewModel = activityViewModel
        val scoreBefore = snapshot(initialViewModel).score

        clickNewGame()

        val snapshot = snapshot(initialViewModel)
        assertEquals(Player.O, snapshot.starter)
        assertEquals(Player.X, snapshot.nextStarter)
        assertEquals(Turn.Human, snapshot.turn)
        assertEquals(1, snapshot.board.count { it == Mark.O })
        assertEquals(0, snapshot.board.count { it == Mark.X })
        assertEquals(scoreBefore, snapshot.score)
        composeTestRule.onNodeWithText(context.getString(R.string.turn_human)).assertExists()

        repeat(3) {
            composeTestRule.waitForIdle()
            assertSameSnapshot(snapshot, snapshot(initialViewModel))
        }
        assertEquals(1, snapshot(initialViewModel).board.count { it == Mark.O })
    }

    @Test
    fun activityRecreationKeepsTheSameViewModelAndCurrentGame() {
        val initialViewModel = activityViewModel

        clickCell(4)
        val beforeRecreation = snapshot(initialViewModel)
        assertEquals(Mark.X, beforeRecreation.board[4])
        assertEquals(1, beforeRecreation.board.count { it == Mark.O })

        scenario.recreate()
        composeTestRule.waitForIdle()

        val recreatedViewModel = viewModelFromCurrentActivity()
        val afterRecreation = snapshot(recreatedViewModel)
        assertSame(initialViewModel, recreatedViewModel)
        assertSameSnapshot(beforeRecreation, afterRecreation)
        composeTestRule.onNodeWithContentDescription(cellDescription(R.string.cell_x, 2, 2))
            .assertExists()
    }

    @Test
    fun terminalGameKeepsResultBoardAndSingleScoreAfterRecreation() {
        val initialViewModel = activityViewModel

        val beforeRecreation = playUntilTerminal(initialViewModel)
        assertTrue(beforeRecreation.result != GameResult.InProgress)
        assertEquals(1, scoreTotal(beforeRecreation))
        assertTerminalStatus(beforeRecreation.result)

        scenario.recreate()
        composeTestRule.waitForIdle()

        val recreatedViewModel = viewModelFromCurrentActivity()
        val afterRecreation = snapshot(recreatedViewModel)
        assertSame(initialViewModel, recreatedViewModel)
        assertSameSnapshot(beforeRecreation, afterRecreation)
        assertEquals(1, scoreTotal(afterRecreation))
        assertTerminalStatus(afterRecreation.result)
    }

    @Test
    fun openingOAndFollowingXOpeningSurviveRecreationWithoutExtraOpening() {
        val initialViewModel = activityViewModel
        val scoreBefore = snapshot(initialViewModel).score

        clickNewGame()
        val openingO = snapshot(initialViewModel)
        assertEquals(Player.O, openingO.starter)
        assertEquals(Player.X, openingO.nextStarter)
        assertEquals(Turn.Human, openingO.turn)
        assertEquals(1, openingO.board.count { it == Mark.O })
        assertEquals(0, openingO.board.count { it == Mark.X })
        assertEquals(scoreBefore, openingO.score)

        scenario.recreate()
        composeTestRule.waitForIdle()

        val recreatedViewModel = viewModelFromCurrentActivity()
        assertSame(initialViewModel, recreatedViewModel)
        assertSameSnapshot(openingO, snapshot(recreatedViewModel))

        clickNewGame()
        val openingX = snapshot(recreatedViewModel)
        assertEquals(Player.X, openingX.starter)
        assertEquals(Player.O, openingX.nextStarter)
        assertEquals(Turn.Human, openingX.turn)
        assertEquals(GameResult.InProgress, openingX.result)
        assertTrue(openingX.board.all { it == Mark.Empty })
        assertEquals(scoreBefore, openingX.score)
    }

    @Test
    fun activeAndTerminalGamesSurviveRealBackgroundAndForegroundTransitions() {
        val initialViewModel = activityViewModel

        clickCell(0)
        val activeBeforeBackground = snapshot(initialViewModel)
        assertEquals(GameResult.InProgress, activeBeforeBackground.result)

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.waitForIdle()

        val resumedViewModel = viewModelFromCurrentActivity()
        assertSame(initialViewModel, resumedViewModel)
        assertSameSnapshot(activeBeforeBackground, snapshot(resumedViewModel))

        val terminalBeforeBackground = playUntilTerminal(resumedViewModel)
        assertTrue(terminalBeforeBackground.result != GameResult.InProgress)
        assertEquals(1, scoreTotal(terminalBeforeBackground))

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.waitForIdle()

        val terminalResumedViewModel = viewModelFromCurrentActivity()
        assertSame(initialViewModel, terminalResumedViewModel)
        assertSameSnapshot(terminalBeforeBackground, snapshot(terminalResumedViewModel))
        assertTerminalStatus(snapshot(terminalResumedViewModel).result)
    }

    @Test
    fun closingActivityAndLaunchingNewInstanceStartsANewSession() {
        val oldViewModel = activityViewModel

        val terminal = playUntilTerminal(oldViewModel)
        assertTrue(terminal.result != GameResult.InProgress)
        assertEquals(1, scoreTotal(terminal))
        clickNewGame()
        val oldSession = snapshot(oldViewModel)
        assertEquals(Player.O, oldSession.starter)
        assertEquals(Player.X, oldSession.nextStarter)
        assertEquals(Turn.Human, oldSession.turn)
        assertEquals(1, oldSession.board.count { it == Mark.O })
        assertEquals(1, scoreTotal(oldSession))
        scenario.onActivity { activity -> activity.finish() }
        scenario.close()

        scenario = ActivityScenario.launch(MainActivity::class.java)
        var newViewModel: GameViewModel? = null
        scenario.onActivity { activity ->
            newViewModel = ViewModelProvider(activity)[GameViewModel::class.java]
        }
        composeTestRule.waitForIdle()

        val newSessionViewModel = checkNotNull(newViewModel)
        val newSession = snapshot(newSessionViewModel)
        assertNotSame(oldViewModel, newSessionViewModel)
        assertEquals(9, newSession.board.size)
        assertTrue(newSession.board.all { it == Mark.Empty })
        assertEquals(Turn.Human, newSession.turn)
        assertEquals(GameResult.InProgress, newSession.result)
        assertEquals(0, newSession.score.xWins)
        assertEquals(0, newSession.score.oWins)
        assertEquals(0, newSession.score.ties)
        assertEquals(Player.X, newSession.starter)
        assertEquals(Player.O, newSession.nextStarter)
        composeTestRule.onNodeWithText(context.getString(R.string.first_human))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.score_human_wins, 0))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.score_computer_wins, 0))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.score_ties, 0))
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun viewModelFromCurrentActivity(): GameViewModel {
        var viewModel: GameViewModel? = null
        scenario.onActivity { activity ->
            viewModel = ViewModelProvider(activity)[GameViewModel::class.java]
        }
        return checkNotNull(viewModel)
    }

    private fun snapshot(viewModel: GameViewModel): GameUiState = viewModel.uiState.value

    private fun playUntilTerminal(viewModel: GameViewModel): GameUiState {
        var humanMoves = 0
        while (viewModel.uiState.value.result == GameResult.InProgress) {
            assertTrue("game did not finish within five human moves", humanMoves < 5)
            val state = snapshot(viewModel)
            assertEquals(Turn.Human, state.turn)
            val location = state.board.indexOfFirst { it == Mark.Empty }
            assertTrue("active game has no free cell", location >= 0)
            clickCell(location)
            humanMoves++
        }
        return snapshot(viewModel)
    }

    private fun clickCell(location: Int) {
        composeTestRule.onNodeWithTag("tic_tac_toe_cell_$location")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()
        awaitHumanOrTerminal(activityViewModel)
    }

    private fun clickNewGame() {
        composeTestRule.onNodeWithTag(NEW_GAME_TAG)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()
        awaitHumanOrTerminal(activityViewModel)
    }

    private fun awaitHumanOrTerminal(viewModel: GameViewModel) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val state = viewModel.uiState.value
            state.turn == Turn.Human || state.result != GameResult.InProgress
        }
    }

    private fun assertTerminalStatus(result: GameResult) {
        val resourceId = when (result) {
            GameResult.Tie -> R.string.result_tie
            is GameResult.Won -> when (result.player) {
                Player.X -> R.string.result_human_wins
                Player.O -> R.string.result_computer_wins
            }

            GameResult.InProgress -> error("expected a terminal result")
        }
        composeTestRule.onNodeWithText(context.getString(resourceId))
            .performScrollTo()
            .assertIsDisplayed()
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

    private fun cellDescription(resourceId: Int, row: Int, column: Int): String =
        context.getString(resourceId, row, column)

}
