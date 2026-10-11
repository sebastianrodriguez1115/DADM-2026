package com.edu.unal.tictactoe

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.ui.start.StartScreen
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val START_BOARD_TAG = "tic_tac_toe_board"

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class StartScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewport = FixedViewport(composeTestRule)

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var inputModeManager: InputModeManager
    private var localClicks = 0
    private var onlineClicks = 0

    private val localLabel get() = context.getString(R.string.start_play_local)
    private val onlineLabel get() = context.getString(R.string.start_play_online)

    private fun show(width: Int = 360, height: Int = 640, fontScale: Float = 1f) {
        viewport.setContent(width, height, fontScale) {
            inputModeManager = LocalInputModeManager.current
            TicTacToeTheme(dynamicColor = false) {
                StartScreen(onPlayLocal = { localClicks++ }, onPlayOnline = { onlineClicks++ })
            }
        }
    }

    private fun assertEntriesReachableAndInsideViewport(width: Int, height: Int) {
        listOf(localLabel, onlineLabel).forEach { label ->
            composeTestRule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
            val bounds = composeTestRule.onNodeWithText(label).getUnclippedBoundsInRoot()
            assertTrue("$label starts outside the viewport", bounds.left >= 0.dp && bounds.top >= 0.dp)
            assertTrue("$label ends outside the viewport", bounds.right <= width.dp && bounds.bottom <= height.dp)
        }
    }

    @Test
    fun showsExactlyTheTwoEntriesWithTheirTextsAndNoBoard() {
        show()

        assertEquals("Jugar contra la computadora", localLabel)
        assertEquals("Jugar en línea", onlineLabel)
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(2)
        composeTestRule.onNodeWithText(localLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(onlineLabel).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(START_BOARD_TAG).assertCountEquals(0)
        composeTestRule.onAllNodes(hasContentDescription("Casilla", substring = true)).assertCountEquals(0)
    }

    @Test
    fun tappingEachEntryInvokesOnlyItsCallback() {
        show()

        composeTestRule.onNodeWithText(localLabel).performClick()
        assertEquals(1, localClicks)
        assertEquals(0, onlineClicks)

        composeTestRule.onNodeWithText(onlineLabel).performClick()
        assertEquals(1, localClicks)
        assertEquals(1, onlineClicks)
    }

    @Test
    fun entriesAreActivatedFromTheKeyboard() {
        show()
        // Buttons only take focus in keyboard input mode, which a real keyboard enters.
        composeTestRule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }

        val local = composeTestRule.onNodeWithText(localLabel)
        local.requestFocus()
        local.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, localClicks)
        assertEquals(0, onlineClicks)

        val online = composeTestRule.onNodeWithText(onlineLabel)
        online.requestFocus()
        online.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, localClicks)
        assertEquals(1, onlineClicks)
    }

    @Test
    fun bothEntriesMeasureAtLeast48DpPerSide() {
        show()

        listOf(localLabel, onlineLabel).forEach { label ->
            val bounds = composeTestRule.onNodeWithText(label).getUnclippedBoundsInRoot()
            assertTrue("$label is narrower than 48 dp", bounds.right - bounds.left >= 48.dp)
            assertTrue("$label is shorter than 48 dp", bounds.bottom - bounds.top >= 48.dp)
        }
        composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            val density = viewport.density
            assertTrue("entry is narrower than 48 dp", node.size.width / density >= 48f)
            assertTrue("entry is shorter than 48 dp", node.size.height / density >= 48f)
        }
    }

    @Test
    fun entriesExposeTheirTextAndAClickAction() {
        show()

        listOf(localLabel, onlineLabel).forEach { label ->
            composeTestRule.onNode(hasClickAction() and hasText(label))
                .assertHasClickAction()
                .assertIsEnabled()
        }
    }

    @Test
    fun portraitKeepsBothEntriesReachableAndInsideTheViewport() {
        show(width = 360, height = 640)

        assertEntriesReachableAndInsideViewport(360, 640)
    }

    @Test
    fun landscapeKeepsBothEntriesReachableAndInsideTheViewport() {
        show(width = 640, height = 360)

        assertEntriesReachableAndInsideViewport(640, 360)
    }

    @Test
    fun veryLowHeightKeepsBothEntriesReachableByScrolling() {
        show(width = 640, height = 160)

        assertEntriesReachableAndInsideViewport(640, 160)
        composeTestRule.onNodeWithText(onlineLabel).performClick()
        assertEquals(1, onlineClicks)
    }

    @Test
    fun largeFontScaleKeepsBothEntriesReachableWithTheFullText() {
        show(width = 320, height = 480, fontScale = 2f)

        assertEntriesReachableAndInsideViewport(320, 480)
        composeTestRule.onAllNodesWithText(localLabel).assertCountEquals(1)
        composeTestRule.onNodeWithText(localLabel).performClick()
        assertEquals(1, localClicks)
    }

    @Test
    fun largeFontScaleInLandscapeKeepsBothEntriesReachable() {
        show(width = 640, height = 360, fontScale = 2f)

        assertEntriesReachableAndInsideViewport(640, 360)
    }
}
