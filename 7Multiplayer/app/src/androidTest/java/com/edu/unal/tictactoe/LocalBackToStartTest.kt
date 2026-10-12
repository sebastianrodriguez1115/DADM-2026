package com.edu.unal.tictactoe

import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.GameMenuTags
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import com.edu.unal.tictactoe.ui.navigation.AppScreen
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BOARD_TAG = "tic_tac_toe_board"
private const val CELL_TAG_PREFIX = "tic_tac_toe_cell_"
private const val WAIT_MILLIS = 10_000L
private const val LONGER_THAN_COMPUTER_DELAY_MILLIS = 1_500L

/**
 * The "Volver al inicio" entry of the local options menu, on the real Activity: it closes the menu,
 * shows the start screen like Back, keeps the local game and leaves it paused while not shown.
 */
@RunWith(AndroidJUnit4::class)
class LocalBackToStartTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val fixture = GamePreferencesTestFixture()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var game: GameViewModel
    private lateinit var navigation: NavigationViewModel

    @Before
    fun launch() {
        fixture.prepare(context)
        seedStoredNickname(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            game = ViewModelProvider(it)[GameViewModel::class.java]
            navigation = ViewModelProvider(it)[NavigationViewModel::class.java]
        }
        composeRule.waitForIdle()
        composeRule.startLocalGame()
    }

    @After
    fun close() {
        try {
            if (::scenario.isInitialized) scenario.close()
        } finally {
            try {
                clearStoredNickname(context)
            } finally {
                fixture.cleanup()
            }
        }
    }

    @Test
    fun menuEntryClosesTheMenuShowsStartKeepsTheGameAndLeavesItPaused() {
        composeRule.onNodeWithTag(CELL_TAG_PREFIX + 0).performClick()
        composeRule.waitForIdle()
        val before = game.uiState.value
        assertEquals(Turn.Computer, before.turn)
        assertEquals(0, before.board.count { it == Mark.O })

        chooseBackToStartFromMenu()

        assertEquals(0, composeRule.onAllNodesWithTag(GameMenuTags.MENU).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag(BOARD_TAG).fetchSemanticsNodes().size)
        assertEquals(AppScreen.Start, navigation.currentScreen.value)
        SystemClock.sleep(LONGER_THAN_COMPUTER_DELAY_MILLIS)
        assertSame(game, currentGameViewModel())
        assertSameGame(before, game.uiState.value)
        assertEquals("the computer moved while the game was not shown", 0, game.uiState.value.board.count { it == Mark.O })
    }

    @Test
    fun returningToTheLocalGameContinuesWhereItWasAndBackStillShowsStart() {
        composeRule.onNodeWithTag(CELL_TAG_PREFIX + 0).performClick()
        composeRule.waitForIdle()
        val before = game.uiState.value

        chooseBackToStartFromMenu()
        composeRule.startLocalGame()

        assertEquals(before.board.take(1), game.uiState.value.board.take(1))
        assertEquals(Mark.X, game.uiState.value.board[0])
        assertEquals(before.score, game.uiState.value.score)
        assertEquals(before.difficulty, game.uiState.value.difficulty)
        assertEquals(before.soundEnabled, game.uiState.value.soundEnabled)
        composeRule.waitUntil(WAIT_MILLIS) { game.uiState.value.board.count { it == Mark.O } == 1 }

        pressBackAsUser()
        awaitStartShown()

        assertEquals(AppScreen.Start, navigation.currentScreen.value)
    }

    private fun chooseBackToStartFromMenu() {
        composeRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(GameMenuTags.BACK_TO_START).assertIsDisplayed().performClick()
        awaitStartShown()
    }

    private fun awaitStartShown() {
        val entry = context.getString(R.string.start_play_local)
        composeRule.waitUntil(WAIT_MILLIS) {
            composeRule.onAllNodesWithText(entry).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun currentGameViewModel(): GameViewModel {
        var viewModel: GameViewModel? = null
        scenario.onActivity { viewModel = ViewModelProvider(it)[GameViewModel::class.java] }
        return checkNotNull(viewModel)
    }

    private fun assertSameGame(expected: GameUiState, actual: GameUiState) {
        assertEquals(expected.board, actual.board)
        assertEquals(expected.turn, actual.turn)
        assertEquals(expected.result, actual.result)
        assertEquals(expected.score, actual.score)
        assertEquals(expected.difficulty, actual.difficulty)
        assertEquals(expected.soundEnabled, actual.soundEnabled)
    }
}
