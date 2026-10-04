package com.edu.unal.tictactoe

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
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
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.GameMenuTags
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

private const val SOUND_TEST_CELL_PREFIX = "tic_tac_toe_cell_"

@RunWith(AndroidJUnit4::class)
class MainActivitySoundOptionsTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var viewModel: GameViewModel
    private val preferencesFixture = GamePreferencesTestFixture()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun launch() {
        preferencesFixture.prepare(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        viewModel = currentViewModel()
        composeRule.waitForIdle()
    }

    @After
    fun close() {
        try {
            if (::scenario.isInitialized) scenario.close()
        } finally {
            preferencesFixture.cleanup()
        }
    }

    @Test
    fun muteIsAccessibleAndSurvivesNewGameRecreationAndBackgroundButNotNewSession() {
        assertTrue("each ViewModel session starts with sound enabled", snapshot().soundEnabled)

        // Finish X's response turn before persistence assertions, so no legitimate pending
        // deadline can change the board during lifecycle checks.
        composeRule.onNodeWithTag(SOUND_TEST_CELL_PREFIX + "0")
            .performScrollTo().performClick()
        await { snapshot().turn == Turn.Human }
        val humanState = snapshot()

        toggleSoundAndAssertAccessibleState(enabled = true)
        assertSameExceptSound(humanState, snapshot())

        openMenu()
        composeRule.onNodeWithTag(GameMenuTags.NEW_GAME).assertIsEnabled().performClick()
        awaitMenuClosed()
        assertTrue("mute is retained by a new game", !snapshot().soundEnabled)
        await { snapshot().turn == Turn.Human }
        val mutedIdleSnapshot = snapshot()

        scenario.recreate()
        composeRule.waitForIdle()
        assertSame(viewModel, currentViewModel())
        assertTrue("mute survives Activity recreation", !snapshot().soundEnabled)
        assertSameExceptSound(mutedIdleSnapshot, snapshot())
        openMenu()
        assertSoundState(enabled = false)
        pressBack()
        awaitMenuClosed()

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
        assertSame(viewModel, currentViewModel())
        assertTrue("mute survives background/resume", !snapshot().soundEnabled)
        assertSameExceptSound(mutedIdleSnapshot, snapshot())

        val oldViewModel = viewModel
        scenario.onActivity { it.finish() }
        composeRule.waitUntil(timeoutMillis = 5_000) { scenario.state == Lifecycle.State.DESTROYED }

        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        viewModel = currentViewModel()
        composeRule.waitForIdle()
        assertNotSame("a finished session must not restore its ViewModel", oldViewModel, viewModel)
        assertTrue("new ViewModel session does not restore mute", snapshot().soundEnabled)
        openMenu()
        assertSoundState(enabled = true)
    }

    private fun toggleSoundAndAssertAccessibleState(enabled: Boolean) {
        openMenu()
        assertSoundState(enabled)
        composeRule.onNodeWithTag(GameMenuTags.SOUND).assertIsEnabled().performClick()
        awaitMenuClosed()
        assertEquals(!enabled, snapshot().soundEnabled)
    }

    private fun assertSoundState(enabled: Boolean) {
        val expected = context.getString(if (enabled) R.string.sound_enabled else R.string.sound_disabled)
        composeRule.onNodeWithTag(GameMenuTags.SOUND).assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected))
        composeRule.onNodeWithText(context.getString(R.string.menu_sound)).assertExists()
        composeRule.onNodeWithText(expected).assertExists()
        assertEquals(enabled, snapshot().soundEnabled)
    }

    private fun openMenu() {
        composeRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .assertIsDisplayed().performClick()
    }

    private fun awaitMenuClosed() = await {
        composeRule.onAllNodesWithTag(GameMenuTags.MENU).fetchSemanticsNodes().isEmpty()
    }

    private fun await(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 5_000, condition = condition)
    }

    private fun snapshot(): GameUiState = viewModel.uiState.value

    private fun assertSameExceptSound(expected: GameUiState, actual: GameUiState) {
        assertEquals(expected.board, actual.board)
        assertEquals(expected.turn, actual.turn)
        assertEquals(expected.result, actual.result)
        assertEquals(expected.starter, actual.starter)
        assertEquals(expected.nextStarter, actual.nextStarter)
        assertEquals(expected.score, actual.score)
        assertEquals(expected.difficulty, actual.difficulty)
        assertEquals(expected.canChangeDifficulty, actual.canChangeDifficulty)
    }

    private fun currentViewModel(): GameViewModel {
        var current: GameViewModel? = null
        scenario.onActivity { current = ViewModelProvider(it)[GameViewModel::class.java] }
        return checkNotNull(current)
    }
}
