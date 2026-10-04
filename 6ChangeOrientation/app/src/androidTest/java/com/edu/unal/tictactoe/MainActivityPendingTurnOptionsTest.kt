package com.edu.unal.tictactoe

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.AboutDialogTags
import com.edu.unal.tictactoe.ui.game.DifficultyDialogTags
import com.edu.unal.tictactoe.ui.game.GameMenuTags
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityPendingTurnOptionsTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var viewModel: GameViewModel

    @After
    fun closeActivity() {
        if (::scenario.isInitialized) scenario.close()
    }

    @Test
    fun openMenuDoesNotPauseResponseAndRemainsUsableAfterOneComputerMove() {
        launch()
        val scoreBefore = snapshot().score
        openMenu()
        scenario.onActivity {
            viewModel.onHumanMove(0)
            assertEquals(Turn.Computer, snapshot().turn)
            assertEquals(0, snapshot().board.count { it == Mark.O })
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            snapshot().board.count { it == Mark.O } == 1
        }
        composeRule.onNodeWithTag(GameMenuTags.MENU).assertIsDisplayed()
        assertEquals(Turn.Human, snapshot().turn)
        assertEquals(1, snapshot().board.count { it == Mark.O })
        assertEquals(scoreBefore, snapshot().score)

        composeRule.onNodeWithTag(GameMenuTags.ABOUT).assertIsEnabled().performClick()
        awaitTag(GameMenuTags.MENU, present = false)
        composeRule.onNodeWithTag(AboutDialogTags.DIALOG).assertIsDisplayed()
        assertEquals(scoreBefore, snapshot().score)
    }

    @Test
    fun acceptedDifficultyDuringEmptyOpeningAppliesToThatOpening() {
        launch()
        openDifficultyDialog()
        val impossibleOption = composeRule.onNodeWithTag(
            DifficultyDialogTags.option(DifficultyLevel.Impossible),
        ).performScrollTo()
        val selectImpossible = impossibleOption.fetchSemanticsNode()
            .config[SemanticsActions.OnClick]?.action
            ?: error("Impossible option has no OnClick semantics")
        scenario.onActivity {
            viewModel.newGame()
            assertTrue(snapshot().board.all { it == Mark.Empty })
            assertEquals(Turn.Computer, snapshot().turn)
            assertTrue("difficulty remains editable during the empty opening", snapshot().canChangeDifficulty)
            assertTrue("real option semantics action must be accepted", selectImpossible())
            assertEquals(DifficultyLevel.Impossible, snapshot().difficulty)
            assertTrue(snapshot().board.all { it == Mark.Empty })
        }
        awaitTag(DifficultyDialogTags.DIALOG, present = false)

        assertEquals(DifficultyLevel.Impossible, snapshot().difficulty)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            snapshot().board.count { it == Mark.O } == 1
        }
        assertEquals(listOf(Mark.O) + List(8) { Mark.Empty }, snapshot().board)
        assertEquals(DifficultyLevel.Impossible, snapshot().difficulty)
        assertFalse(snapshot().canChangeDifficulty)
    }

    @Test
    fun difficultyDialogOpenedDuringOpeningCannotApplyAfterOHasMoved() {
        launch()
        openDifficultyDialog()
        scenario.onActivity {
            viewModel.newGame()
            assertTrue(snapshot().board.all { it == Mark.Empty })
            assertEquals(Turn.Computer, snapshot().turn)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            snapshot().board.count { it == Mark.O } == 1
        }
        val afterOpening = snapshot()
        assertFalse(afterOpening.canChangeDifficulty)
        composeRule.onNodeWithTag(DifficultyDialogTags.option(DifficultyLevel.Hard))
            .performScrollTo().performClick()
        awaitTag(DifficultyDialogTags.DIALOG, present = false)

        assertSameGameState(afterOpening, snapshot())
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            viewModel = ViewModelProvider(activity)[GameViewModel::class.java]
        }
        composeRule.waitForIdle()
    }

    private fun openDifficultyDialog() {
        openMenu()
        composeRule.onNodeWithTag(GameMenuTags.DIFFICULTY).assertIsEnabled().performClick()
        awaitTag(DifficultyDialogTags.DIALOG, present = true)
    }

    private fun openMenu() {
        composeRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .assertIsDisplayed().performClick()
        composeRule.waitForIdle()
    }

    private fun snapshot(): GameUiState = viewModel.uiState.value

    private fun awaitTag(tag: String, present: Boolean) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            val presentNow = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            presentNow == present
        }
    }

    private fun assertSameGameState(expected: GameUiState, actual: GameUiState) {
        assertEquals(expected.board, actual.board)
        assertEquals(expected.turn, actual.turn)
        assertEquals(expected.result, actual.result)
        assertEquals(expected.starter, actual.starter)
        assertEquals(expected.nextStarter, actual.nextStarter)
        assertEquals(expected.score, actual.score)
        assertEquals(expected.difficulty, actual.difficulty)
        assertEquals(expected.soundEnabled, actual.soundEnabled)
    }
}
