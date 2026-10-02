package com.edu.unal.tictactoe

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.ui.game.GameMenu
import com.edu.unal.tictactoe.ui.game.GameMenuTags
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameMenuTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun eachEnabledEntryShowsItsLabelClosesMenuAndInvokesCallbackOnce() {
        var newGameCount = 0
        var difficultyCount = 0
        var aboutCount = 0
        var exitCount = 0
        setMenu(
            canChangeDifficulty = true,
            onNewGame = { newGameCount++ },
            onDifficulty = { difficultyCount++ },
            onAbout = { aboutCount++ },
            onExit = { exitCount++ },
        )

        clickEntry(GameMenuTags.NEW_GAME, R.string.new_game)
        assertEquals(1, newGameCount)
        assertMenuClosed()

        clickEntry(GameMenuTags.DIFFICULTY, R.string.menu_difficulty)
        assertEquals(1, difficultyCount)
        assertMenuClosed()

        clickEntry(GameMenuTags.ABOUT, R.string.menu_about)
        assertEquals(1, aboutCount)
        assertMenuClosed()

        clickEntry(GameMenuTags.EXIT, R.string.menu_exit)
        assertEquals(1, exitCount)
        assertMenuClosed()
    }

    @Test
    fun disabledDifficultyStaysVisibleAndClickingItDoesNotInvokeOrClose() {
        var difficultyCount = 0
        setMenu(canChangeDifficulty = false, onDifficulty = { difficultyCount++ })

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.NEW_GAME).assertIsEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.menu_difficulty)).assertExists()
        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY)
            .assertIsDisplayed()
            .assertIsNotEnabled()

        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY).performTouchInput { click() }
        composeTestRule.waitForIdle()

        assertEquals(0, difficultyCount)
        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(GameMenuTags.MENU).assertExists()
    }

    @Test
    fun recompositionReflectsCanChangeWithoutInvokingCallbacks() {
        var canChangeDifficulty by mutableStateOf(false)
        var difficultyCount = 0
        composeTestRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GameMenu(
                        canChangeDifficulty = canChangeDifficulty,
                        onNewGame = {},
                        onDifficulty = { difficultyCount++ },
                        onAbout = {},
                        onExit = {},
                    )
                }
            }
        }

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY).assertIsNotEnabled()

        composeTestRule.runOnIdle { canChangeDifficulty = true }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY).assertIsEnabled()

        composeTestRule.runOnIdle { canChangeDifficulty = false }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY).assertIsNotEnabled()

        assertEquals(0, difficultyCount)
    }

    @Test
    fun backDismissesMenuWithoutLeavingTheContent() {
        setMenu(canChangeDifficulty = true)

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.MENU).assertExists()

        pressBack()
        composeTestRule.waitForIdle()

        assertMenuClosed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .assertExists()
    }

    private fun setMenu(
        canChangeDifficulty: Boolean,
        onNewGame: () -> Unit = {},
        onDifficulty: () -> Unit = {},
        onAbout: () -> Unit = {},
        onExit: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GameMenu(
                        canChangeDifficulty = canChangeDifficulty,
                        onNewGame = onNewGame,
                        onDifficulty = onDifficulty,
                        onAbout = onAbout,
                        onExit = onExit,
                    )
                }
            }
        }
    }

    private fun openMenu() {
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun clickEntry(tag: String, @StringRes labelRes: Int) {
        openMenu()
        composeTestRule.onNodeWithText(context.getString(labelRes)).assertExists()
        composeTestRule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun assertMenuClosed() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(GameMenuTags.MENU)
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }
}
