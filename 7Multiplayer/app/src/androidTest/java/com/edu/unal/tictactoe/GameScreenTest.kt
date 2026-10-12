package com.edu.unal.tictactoe

import androidx.lifecycle.SavedStateHandle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
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

    private val viewport = FixedViewport(composeTestRule)

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun portraitHasNoAppTitleAndMenuSitsBesideNewGameWithoutCoveringTheBoard() {
        setSizedHost(controlledViewModel(), width = 360, height = 640)

        assertNoTitleAndMenuBesideNewGame()
    }

    @Test
    fun landscapeHasNoAppTitleAndMenuSitsBesideNewGameWithoutCoveringTheBoard() {
        setSizedHost(controlledViewModel(), width = 640, height = 360)

        assertNoTitleAndMenuBesideNewGame()
        val board = composeTestRule.onNodeWithTag(GAME_SCREEN_BOARD_TAG).getUnclippedBoundsInRoot()
        val menu = composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .getUnclippedBoundsInRoot()
        assertTrue("in landscape the menu must be right of the board", menu.left >= board.right)
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
        awaitComputerOpening(viewModel)

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
    fun staleDifficultyChoiceAfterOpeningIsRejectedWithoutToastOrStateChange() {
        val viewModel = controlledViewModel()
        val host = HostState()
        setHost(viewModel, host)

        // Keep the difficulty dialog open over an empty-X game, then let O's
        // opening make that already-open selection stale.
        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.DIFFICULTY)
            .assertIsEnabled().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { viewModel.newGame() }
        awaitComputerOpening(viewModel)
        val stateAfterOpening = viewModel.uiState.value
        assertEquals(1, stateAfterOpening.board.count { it == Mark.O })
        assertFalse(stateAfterOpening.canChangeDifficulty)

        clickDifficultyOption(DifficultyLevel.Hard)

        assertEquals(DifficultyLevel.Expert, viewModel.uiState.value.difficulty)
        assertEquals(stateAfterOpening.board, viewModel.uiState.value.board)
        assertEquals(stateAfterOpening.result, viewModel.uiState.value.result)
        assertEquals(stateAfterOpening.score, viewModel.uiState.value.score)
        assertEquals(stateAfterOpening.turn, viewModel.uiState.value.turn)
        assertTrue(host.toasts.isEmpty())
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
    fun boardStaysSquareAndControlsAreReachableInACompactViewport() {
        val viewModel = controlledViewModel()
        setSizedHost(viewModel, width = 320, height = 360)

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .performScrollTo()
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

    @Test
    fun resetScoresEntryIsVisibleEnabledLabeledAndActionable() {
        setHost(controlledViewModel())

        openMenu()

        composeTestRule.onNodeWithTag(GameMenuTags.RESET_SCORES)
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .assert(hasText(context.getString(R.string.menu_reset_scores)))
    }

    @Test
    fun resetScoresOnScoredTerminalGameZeroesCountersImmediatelyAndKeepsBoardAndResult() {
        val preferences = InMemoryGamePreferences().apply { writeScore(Score(2, 1, 3)) }
        val viewModel = controlledViewModel(preferences)
        val host = HostState()
        setHost(viewModel, host)
        composeTestRule.runOnIdle { assertTrue(viewModel.setDifficulty(DifficultyLevel.Easy)) }

        // With the fixed randomizer and Easy, O answers 1 and 2, so X wins on column 0.
        clickCell(0)
        awaitHumanTurn(viewModel)
        clickCell(3)
        awaitHumanTurn(viewModel)
        clickCell(6)
        val terminal = viewModel.uiState.value
        assertEquals(GameResult.Won(Player.X), terminal.result)
        assertEquals(Score(3, 1, 3), terminal.score)
        assertScoreTexts(3, 1, 3)
        composeTestRule.onNodeWithText(context.getString(R.string.result_human_wins)).assertExists()

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.RESET_SCORES)
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()
        waitUntilMenuClosed()

        assertScoreTexts(0, 0, 0)
        composeTestRule.onNodeWithText(context.getString(R.string.result_human_wins)).assertExists()
        val afterReset = viewModel.uiState.value
        assertEquals(Score(), afterReset.score)
        assertEquals(Score(), preferences.readScore())
        assertEquals(terminal.board, afterReset.board)
        assertEquals(terminal.result, afterReset.result)
        assertEquals(terminal.turn, afterReset.turn)
        assertEquals(terminal.starter, afterReset.starter)
        assertEquals(terminal.nextStarter, afterReset.nextStarter)

        composeTestRule.runOnIdle { host.generation++ }
        composeTestRule.waitForIdle()
        assertScoreTexts(0, 0, 0)
        assertEquals(Score(), viewModel.uiState.value.score)
        assertEquals(Score(), preferences.readScore())
    }

    @Test
    fun resetScoresDuringActiveGameKeepsTheSessionAndPendingReplyHappensOnce() {
        val preferences = InMemoryGamePreferences().apply { writeScore(Score(2, 1, 3)) }
        // Not resumed: the reply of O stays pending, so the reset provably happens before it.
        val viewModel = controlledViewModel(preferences, resumed = false)
        setHost(viewModel)

        clickCell(4)
        val active = viewModel.uiState.value
        assertEquals(Turn.Computer, active.turn)
        assertEquals(GameResult.InProgress, active.result)
        assertEquals(0, active.board.count { it == Mark.O })

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.RESET_SCORES)
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()
        waitUntilMenuClosed()

        assertScoreTexts(0, 0, 0)
        val afterReset = viewModel.uiState.value
        assertEquals(Score(), afterReset.score)
        assertEquals(Score(), preferences.readScore())
        assertEquals(active.board, afterReset.board)
        assertEquals(Turn.Computer, afterReset.turn)
        assertEquals(GameResult.InProgress, afterReset.result)
        assertEquals(active.starter, afterReset.starter)
        assertEquals(active.nextStarter, afterReset.nextStarter)

        composeTestRule.runOnIdle { viewModel.onHostResumed() }
        awaitComputerOpening(viewModel)
        awaitHumanTurn(viewModel)
        composeTestRule.waitForIdle()
        val afterReply = viewModel.uiState.value
        assertEquals(1, afterReply.board.count { it == Mark.O })
        assertEquals(1, afterReply.board.count { it == Mark.X })
        assertEquals(Mark.X, afterReply.board[4])
        assertEquals(Score(), afterReply.score)
    }

    @Test
    fun backToStartEntryIsReachableAndInvokesItsCallbackInLandscapeAndWithLargeFont() {
        val viewModel = controlledViewModel()
        var backToStarts = 0
        viewport.setContent(640, 360, fontScale = 2f) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            TicTacToeTheme(dynamicColor = false) {
                GameScreen(
                    uiState = uiState,
                    onCellClick = viewModel::onHumanMove,
                    onNewGame = viewModel::newGame,
                    onToggleSound = viewModel::toggleSound,
                    onDifficultySelected = viewModel::setDifficulty,
                    onShowToast = {},
                    onResetScores = viewModel::resetScores,
                    onBackToStart = { backToStarts++ },
                )
            }
        }

        openMenu()
        composeTestRule.onNodeWithTag(GameMenuTags.BACK_TO_START)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()
        waitUntilMenuClosed()

        assertEquals(1, backToStarts)
    }

    private fun setSizedHost(viewModel: GameViewModel, width: Int, height: Int) {
        val host = HostState()
        viewport.setContent(width, height) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            TicTacToeTheme(dynamicColor = false) {
                GameScreen(
                    uiState = uiState,
                    onCellClick = viewModel::onHumanMove,
                    onNewGame = viewModel::newGame,
                    onToggleSound = viewModel::toggleSound,
                    onDifficultySelected = viewModel::setDifficulty,
                    onShowToast = { host.toasts += it },
                    onResetScores = viewModel::resetScores,
                    onBackToStart = {},
                )
            }
        }
    }

    private fun assertNoTitleAndMenuBesideNewGame() {
        composeTestRule.onNodeWithText(context.getString(R.string.app_name)).assertDoesNotExist()
        val menuNode = composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
        val menu = menuNode.getUnclippedBoundsInRoot()
        val newGame = composeTestRule.onNodeWithTag(GAME_SCREEN_NEW_GAME_TAG)
            .assertIsDisplayed()
            .assertIsEnabled()
            .getUnclippedBoundsInRoot()
        val board = composeTestRule.onNodeWithTag(GAME_SCREEN_BOARD_TAG).getUnclippedBoundsInRoot()

        assertTrue(
            "menu must share the controls row with New game",
            menu.top < newGame.bottom && newGame.top < menu.bottom,
        )
        assertTrue(
            "menu must not overlap New game",
            menu.left >= newGame.right || newGame.left >= menu.right,
        )
        assertTrue(
            "menu must not overlap the board",
            menu.left >= board.right || board.left >= menu.right ||
                menu.top >= board.bottom || board.top >= menu.bottom,
        )
    }

    private fun assertScoreTexts(xWins: Int, oWins: Int, ties: Int) {
        composeTestRule.onNodeWithText(context.getString(R.string.score_human_wins, xWins)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.score_computer_wins, oWins)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.score_ties, ties)).assertExists()
    }

    private fun awaitHumanTurn(viewModel: GameViewModel) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val state = viewModel.uiState.value
            state.turn == Turn.Human || state.result != GameResult.InProgress
        }
    }

    private fun controlledViewModel(
        preferences: InMemoryGamePreferences = InMemoryGamePreferences(),
        resumed: Boolean = true,
    ): GameViewModel = GameViewModel(
        preferences,
        SavedStateHandle(),
        TicTacToeGame(MoveRandomizer { 0 }),
    ).also { viewModel ->
        if (resumed) composeTestRule.runOnIdle { viewModel.onHostResumed() }
    }

    private fun awaitComputerOpening(viewModel: GameViewModel) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.board.count { it == Mark.O } == 1
        }
    }

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
                        onToggleSound = viewModel::toggleSound,
                        onDifficultySelected = viewModel::setDifficulty,
                        onShowToast = { host.toasts += it },
                        onResetScores = viewModel::resetScores,
                        onBackToStart = {},
                    )
                }
            }
        }
    }

    private fun openMenu() {
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .performScrollTo()
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
    }
}
