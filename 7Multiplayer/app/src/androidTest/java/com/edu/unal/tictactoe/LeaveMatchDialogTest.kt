package com.edu.unal.tictactoe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.ui.online.match.LeaveMatchDialog
import com.edu.unal.tictactoe.ui.online.match.LeaveMatchDialogTags
import com.edu.unal.tictactoe.ui.online.match.MatchPhase
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchScreen
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchTags
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchUiState
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LeaveMatchDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewport = FixedViewport(composeTestRule)

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private var leaves = 0
    private var clicked = 0
    private var externalOpen by mutableStateOf(false)
    private var phase by mutableStateOf(MatchPhase.Playing)

    private fun state() = OnlineMatchUiState(
        myMark = Player.X,
        opponentName = "Luis",
        board = List(9) { Mark.Empty },
        phase = phase,
        isMyTurn = true,
    )

    private fun showScreen(
        width: Int = 360,
        height: Int = 640,
        fontScale: Float = 1f,
    ) {
        viewport.setContent(width, height, fontScale) {
            TicTacToeTheme(dynamicColor = false) {
                OnlineMatchScreen(
                    uiState = state(),
                    isLeaveDialogOpen = externalOpen,
                    onLeaveDialogOpenChange = { externalOpen = it },
                    onLeaveConfirmed = { leaves++ },
                    onCellClick = { clicked++ },
                    onBackToLobby = {},
                )
            }
        }
    }

    private fun dialogCount(count: Int) {
        composeTestRule.onAllNodesWithTag(LeaveMatchDialogTags.DIALOG).assertCountEquals(count)
    }

    @Test
    fun leaveOpensTheDialogWithTheMatchStillVisibleBehind() {
        showScreen()
        dialogCount(0)

        composeTestRule.onNodeWithTag(OnlineMatchTags.LEAVE).performClick()

        assertEquals("¿Abandonar la partida?", context.getString(R.string.leave_match_title))
        composeTestRule.onNodeWithText("¿Abandonar la partida?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Abandonar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tic_tac_toe_board").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu turno").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rival: Luis").assertIsDisplayed()
        assertEquals(0, leaves)
    }

    @Test
    fun cancelClosesTheDialogWithoutLeavingAndTheMatchContinues() {
        showScreen()
        composeTestRule.onNodeWithTag(OnlineMatchTags.LEAVE).performClick()

        composeTestRule.onNodeWithTag(LeaveMatchDialogTags.CANCEL).performClick()

        dialogCount(0)
        assertEquals(0, leaves)
        composeTestRule.onNodeWithText("Tu turno").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tic_tac_toe_cell_0").performClick()
        assertEquals(1, clicked)
    }

    @Test
    fun leaveConfirmationInvokesTheCallbackOnceAndClosesTheDialog() {
        showScreen()
        composeTestRule.onNodeWithTag(OnlineMatchTags.LEAVE).performClick()

        composeTestRule.onNodeWithTag(LeaveMatchDialogTags.CONFIRM).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, leaves)
        dialogCount(0)
    }

    @Test
    fun openingItFromOutsideShowsTheSameDialogAndWorksTheSame() {
        showScreen()
        dialogCount(0)

        composeTestRule.runOnIdle { externalOpen = true }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("¿Abandonar la partida?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tic_tac_toe_board").assertIsDisplayed()
        composeTestRule.onNodeWithTag(LeaveMatchDialogTags.CANCEL).performClick()
        composeTestRule.runOnIdle { assertTrue("cancel reports closed", !externalOpen) }
        dialogCount(0)

        composeTestRule.runOnIdle { externalOpen = true }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(LeaveMatchDialogTags.CONFIRM).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, leaves)
        dialogCount(0)
    }

    @Test
    fun theDialogDisappearsWhenTheMatchIsNoLongerInProgress() {
        showScreen()
        composeTestRule.onNodeWithTag(OnlineMatchTags.LEAVE).performClick()
        dialogCount(1)

        composeTestRule.runOnIdle { phase = MatchPhase.Abandoned }
        composeTestRule.waitForIdle()

        dialogCount(0)
        composeTestRule.onNodeWithTag(OnlineMatchTags.BACK_TO_LOBBY).assertIsDisplayed()
    }

    @Test
    fun theOpenDialogSurvivesStateRestoration() {
        val restoration = StateRestorationTester(composeTestRule)
        restoration.setContent {
            var open by rememberSaveable { mutableStateOf(false) }
            TicTacToeTheme(dynamicColor = false) {
                OnlineMatchScreen(
                    uiState = state(),
                    isLeaveDialogOpen = open,
                    onLeaveDialogOpenChange = { open = it },
                    onLeaveConfirmed = { leaves++ },
                    onCellClick = {},
                    onBackToLobby = {},
                )
            }
        }
        composeTestRule.onNodeWithTag(OnlineMatchTags.LEAVE).performClick()
        dialogCount(1)

        restoration.emulateSavedInstanceStateRestore()

        dialogCount(1)
        composeTestRule.onNodeWithText("¿Abandonar la partida?").assertIsDisplayed()
        composeTestRule.onNodeWithTag(LeaveMatchDialogTags.CANCEL).performClick()
        dialogCount(0)

        restoration.emulateSavedInstanceStateRestore()

        dialogCount(0)
        assertEquals(0, leaves)
    }

    @Test
    fun standaloneDialogCallsOnlyTheActionPressed() {
        var confirmed = 0
        var dismissed = 0
        composeTestRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                LeaveMatchDialog(onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
            }
        }

        composeTestRule.onNodeWithTag(LeaveMatchDialogTags.CANCEL).performClick()
        assertEquals(0 to 1, confirmed to dismissed)

        composeTestRule.onNodeWithTag(LeaveMatchDialogTags.CONFIRM).performClick()
        assertEquals(1 to 1, confirmed to dismissed)
    }

    @Test
    fun actionsMeasureAtLeast48DpAndCarryTheirResourceLabel() {
        showScreen()
        composeTestRule.onNodeWithTag(OnlineMatchTags.LEAVE).performClick()

        listOf(
            LeaveMatchDialogTags.CONFIRM to R.string.leave_match_confirm,
            LeaveMatchDialogTags.CANCEL to R.string.lobby_cancel,
        ).forEach { (tag, label) ->
            val node = composeTestRule.onNodeWithTag(tag)
            node.assert(hasText(context.getString(label)))
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue("$tag is shorter than 48 dp", bounds.bottom - bounds.top >= 48.dp - 1.dp)
            assertTrue("$tag is narrower than 48 dp", bounds.right - bounds.left >= 48.dp - 1.dp)
        }
    }

    @Test
    fun theDialogIsOperableInLandscapeWithLargeFont() {
        showScreen(width = 640, height = 360, fontScale = 2f)

        composeTestRule.onNodeWithTag(OnlineMatchTags.LEAVE).performClick()

        composeTestRule.onNodeWithTag(LeaveMatchDialogTags.CANCEL).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LeaveMatchDialogTags.CONFIRM).assertIsDisplayed().performClick()
        assertEquals(1, leaves)
    }
}
