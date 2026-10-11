package com.edu.unal.tictactoe

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.ui.online.lobby.WaitingForOpponentScreen
import com.edu.unal.tictactoe.ui.online.lobby.WaitingForOpponentTags
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val WAITING_BOARD_TAG = "tic_tac_toe_board"

@RunWith(AndroidJUnit4::class)
class WaitingForOpponentScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewport = FixedViewport(composeTestRule)

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private var cancels = 0

    private val waitingLabel get() = context.getString(R.string.lobby_waiting_for_opponent)
    private val cancelLabel get() = context.getString(R.string.lobby_cancel)

    private fun show(width: Int = 360, height: Int = 640, fontScale: Float = 1f) {
        viewport.setContent(width, height, fontScale) {
            TicTacToeTheme(dynamicColor = false) {
                WaitingForOpponentScreen(onCancel = { cancels++ })
            }
        }
    }

    private fun assertBothReachableInside(width: Int, height: Int) {
        listOf(waitingLabel, cancelLabel).forEach { label ->
            composeTestRule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
            val bounds = composeTestRule.onNodeWithText(label).getUnclippedBoundsInRoot()
            assertTrue("$label starts outside the viewport", bounds.left >= 0.dp && bounds.top >= 0.dp)
            assertTrue("$label ends outside the viewport", bounds.right <= width.dp && bounds.bottom <= height.dp)
        }
    }

    @Test
    fun showsOnlyTheWaitingTextAndCancelWithoutBoard() {
        show()

        assertEquals("Esperando rival…", waitingLabel)
        assertEquals("Cancelar", cancelLabel)
        composeTestRule.onNodeWithTag(WaitingForOpponentTags.MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText(waitingLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(cancelLabel).assertIsDisplayed()
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WAITING_BOARD_TAG).assertCountEquals(0)
        composeTestRule.onAllNodes(hasContentDescription("Casilla", substring = true)).assertCountEquals(0)
    }

    @Test
    fun cancelInvokesItsCallback() {
        show()

        composeTestRule.onNodeWithTag(WaitingForOpponentTags.CANCEL).performClick()

        assertEquals(1, cancels)
    }

    @Test
    fun cancelMeasuresAtLeast48Dp() {
        show()

        val bounds = composeTestRule.onNodeWithTag(WaitingForOpponentTags.CANCEL).getUnclippedBoundsInRoot()
        assertTrue("cancel is shorter than 48 dp", bounds.bottom - bounds.top >= 48.dp)
        assertTrue("cancel is narrower than 48 dp", bounds.right - bounds.left >= 48.dp)
    }

    @Test
    fun portraitKeepsBothElementsReachable() {
        show(width = 360, height = 640)

        assertBothReachableInside(360, 640)
    }

    @Test
    fun landscapeWithLargeFontKeepsBothElementsReachable() {
        show(width = 640, height = 360, fontScale = 2f)

        assertBothReachableInside(640, 360)
        composeTestRule.onNodeWithText(cancelLabel).performClick()
        assertEquals(1, cancels)
    }
}
