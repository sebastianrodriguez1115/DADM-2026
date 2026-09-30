package com.edu.unal.tictactoe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.ui.game.DifficultyDialogTags
import com.edu.unal.tictactoe.ui.game.GameMenuTags
import com.edu.unal.tictactoe.ui.game.GameScreen
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val GAME_SCREEN_BOARD_TAG = "tic_tac_toe_board"
private const val GAME_SCREEN_NEW_GAME_TAG = "tic_tac_toe_new_game"

@RunWith(AndroidJUnit4::class)
class GameScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun topBarMenuIsAccessibleAndDoesNotCoverTheBoard() {
        setHost(controlledViewModel())

        val menuButton = composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.menu_open))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(GAME_SCREEN_BOARD_TAG).assertExists()
        val menuBottom = menuButton.getUnclippedBoundsInRoot().bottom
        val boardTop = composeTestRule
            .onNodeWithTag(GAME_SCREEN_BOARD_TAG)
            .getUnclippedBoundsInRoot()
            .top
        assertTrue("menu must stay above the board", menuBottom <= boardTop)

        composeTestRule.onNodeWithTag(GAME_SCREEN_NEW_GAME_TAG)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun menuAndShortcutNewGameRunTheSameActionPreservingScore() {
        val viewModel = controlledViewModel()
        setHost(viewModel)
        val scoreBefore = viewModel.uiState.value.score

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.NEW_GAME)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()
        waitUntilMenuClosed()

        val afterMenu = viewModel.uiState.value
        assertEquals(Player.O, afterMenu.starter)
        assertEquals(Player.X, afterMenu.nextStarter)
        assertEquals(1, afterMenu.board.count { it == Mark.O })
        assertEquals(0, afterMenu.board.count { it == Mark.X })
        assertEquals(scoreBefore, afterMenu.score)

        composeTestRule.onNodeWithTag(GAME_SCREEN_NEW_GAME_TAG)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        val afterShortcut = viewModel.uiState.value
        assertEquals(Player.X, afterShortcut.starter)
        assertEquals(Player.O, afterShortcut.nextStarter)
        assertTrue(afterShortcut.board.all { it == Mark.Empty })
        assertEquals(scoreBefore, afterShortcut.score)
    }

    @Test
    fun difficultyEntryUpdatesVmClosesWindowsAndToastsOnceEvenWhenRepeatingLevel() {
        val viewModel = controlledViewModel()
        val host = HostState()
        setHost(viewModel, host)

        assertEquals(DifficultyLevel.Expert, viewModel.uiState.value.difficulty)

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()
        waitUntilMenuClosed()
        composeTestRule.onNodeWithTag(DifficultyDialogTags.DIALOG).assertIsDisplayed()

        val boardBefore = viewModel.uiState.value.board
        val scoreBefore = viewModel.uiState.value.score

        clickDifficultyOption(DifficultyLevel.Impossible)

        assertEquals(DifficultyLevel.Impossible, viewModel.uiState.value.difficulty)
        assertEquals(boardBefore, viewModel.uiState.value.board)
        assertEquals(scoreBefore, viewModel.uiState.value.score)
        assertEquals(listOf(DifficultyLevel.Impossible), host.toasts)
        waitUntilDialogClosed()

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY)
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()
        waitUntilMenuClosed()

        clickDifficultyOption(DifficultyLevel.Impossible)

        assertEquals(
            listOf(DifficultyLevel.Impossible, DifficultyLevel.Impossible),
            host.toasts,
        )
        waitUntilDialogClosed()
    }

    @Test
    fun disabledDifficultyDuringActiveGameDoesNotOpenOrNotify() {
        val viewModel = controlledViewModel()
        val host = HostState()
        setHost(viewModel, host)

        clickCell(4)
        assertFalse(viewModel.uiState.value.canChangeDifficulty)

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY)
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY)
            .performTouchInput { click() }
        composeTestRule.waitForIdle()

        assertTrue(host.toasts.isEmpty())
        assertEquals(
            0,
            composeTestRule.onAllNodesWithTag(DifficultyDialogTags.DIALOG)
                .fetchSemanticsNodes().size,
        )
        assertEquals(DifficultyLevel.Expert, viewModel.uiState.value.difficulty)
    }

    @Test
    fun hostRecompositionAndRecreationDoNotReplayTheToast() {
        val viewModel = controlledViewModel()
        val host = HostState()
        setHost(viewModel, host)

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY)
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()
        waitUntilMenuClosed()

        clickDifficultyOption(DifficultyLevel.Hard)
        assertEquals(listOf(DifficultyLevel.Hard), host.toasts)

        composeTestRule.runOnIdle { host.revision++ }
        composeTestRule.waitForIdle()
        assertEquals(listOf(DifficultyLevel.Hard), host.toasts)
        assertEquals(DifficultyLevel.Hard, viewModel.uiState.value.difficulty)

        composeTestRule.runOnIdle { host.generation++ }
        composeTestRule.waitForIdle()
        assertEquals(listOf(DifficultyLevel.Hard), host.toasts)
        assertEquals(DifficultyLevel.Hard, viewModel.uiState.value.difficulty)
    }

    @Test
    fun boardStaysSquareAndNewGameIsReachableInsideTopBarViewport() {
        val viewModel = controlledViewModel()
        val host = HostState()
        val viewportTag = "game_screen_test_viewport"
        composeTestRule.setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            TicTacToeTheme(dynamicColor = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    GameScreen(
                        uiState = uiState,
                        onCellClick = viewModel::onHumanMove,
                        onNewGame = viewModel::newGame,
                        onDifficultySelected = viewModel::setDifficulty,
                        onShowToast = { host.toasts += it },
                        onExit = { host.exitCount++ },
                        modifier = Modifier
                            .size(320.dp, 360.dp)
                            .testTag(viewportTag),
                    )
                }
            }
        }

        val viewportBounds = composeTestRule
            .onNodeWithTag(viewportTag)
            .getUnclippedBoundsInRoot()
        val tolerance = 1f
        assertEquals(320f, (viewportBounds.right - viewportBounds.left).value, tolerance)
        assertEquals(360f, (viewportBounds.bottom - viewportBounds.top).value, tolerance)

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(GAME_SCREEN_BOARD_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        val boardBounds = composeTestRule
            .onNodeWithTag(GAME_SCREEN_BOARD_TAG)
            .getUnclippedBoundsInRoot()
        val boardWidth = boardBounds.right - boardBounds.left
        val boardHeight = boardBounds.bottom - boardBounds.top
        assertTrue("board must have positive size", boardWidth > 0.dp)
        assertTrue(
            "board must be square",
            kotlin.math.abs((boardWidth - boardHeight).value) <= 1f,
        )

        composeTestRule.onNodeWithTag(GAME_SCREEN_NEW_GAME_TAG)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    private fun controlledViewModel(): GameViewModel =
        GameViewModel(TicTacToeGame(MoveRandomizer { 0 }))

    private fun setHost(
        viewModel: GameViewModel,
        host: HostState = HostState(),
    ) {
        composeTestRule.setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            TicTacToeTheme(dynamicColor = false) {
                check(host.revision >= 0)
                key(host.generation) {
                    GameScreen(
                        uiState = uiState,
                        onCellClick = viewModel::onHumanMove,
                        onNewGame = viewModel::newGame,
                        onDifficultySelected = viewModel::setDifficulty,
                        onShowToast = { host.toasts += it },
                        onExit = { host.exitCount++ },
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

    private fun clickDifficultyOption(level: DifficultyLevel) {
        composeTestRule.onNodeWithTag(DifficultyDialogTags.option(level))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun clickCell(location: Int) {
        composeTestRule.onNodeWithTag(GAME_SCREEN_BOARD_TAG).performScrollTo()
        composeTestRule.onNodeWithTag("tic_tac_toe_cell_$location")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun waitUntilMenuClosed() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(GameMenuTags.MENU)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    private fun waitUntilDialogClosed() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(DifficultyDialogTags.DIALOG)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    private class HostState {
        var generation by mutableStateOf(0)
        var revision by mutableStateOf(0)
        val toasts = mutableListOf<DifficultyLevel>()
        var exitCount = 0
    }
}
