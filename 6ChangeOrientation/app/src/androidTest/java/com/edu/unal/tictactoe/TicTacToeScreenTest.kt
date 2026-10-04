package com.edu.unal.tictactoe

import androidx.lifecycle.SavedStateHandle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import com.edu.unal.tictactoe.ui.game.TicTacToeScreen
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BOARD_TAG = "tic_tac_toe_board"
private const val STATUS_TAG = "tic_tac_toe_status"
private const val SCORE_TAG = "tic_tac_toe_score"
private const val NEW_GAME_TAG = "tic_tac_toe_new_game"
private const val MENU_STUB_TAG = "tic_tac_toe_test_menu"
private const val UI_STATE_TIMEOUT_MILLIS = 5_000L

@RunWith(AndroidJUnit4::class)
class TicTacToeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewport = FixedViewport(composeTestRule)

    private val context
        get() = androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation()
            .targetContext

    @Test
    fun rendersBoardStatusScoreAndNewGameInOrder() {
        val state = GameUiState(
            board = listOf(
                Mark.Empty,
                Mark.X,
                Mark.O,
                Mark.Empty,
                Mark.X,
                Mark.O,
                Mark.O,
                Mark.Empty,
                Mark.X,
            ),
            turn = Turn.Human,
            result = GameResult.Won(Player.X),
            score = Score(xWins = 2, oWins = 3, ties = 4),
        )
        viewport.setContent(360, 640) {
            TicTacToeTheme(dynamicColor = false) {
                Surface {
                    TicTacToeScreen(uiState = state, onCellClick = {}, onNewGame = {})
                }
            }
        }

        composeTestRule.onNodeWithTag(BOARD_TAG).assertExists()
        composeTestRule.onNodeWithTag(STATUS_TAG).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.result_human_wins)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.score_human_wins, 2)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.score_computer_wins, 3)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.score_ties, 4)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.new_game)).assertExists()

        val boardBottom = composeTestRule.onNodeWithTag(BOARD_TAG).getUnclippedBoundsInRoot().bottom
        val statusTop = composeTestRule.onNodeWithTag(STATUS_TAG).getUnclippedBoundsInRoot().top
        val scoreTop = composeTestRule.onNodeWithTag(SCORE_TAG).getUnclippedBoundsInRoot().top
        val newGameTop = composeTestRule.onNodeWithTag(NEW_GAME_TAG).getUnclippedBoundsInRoot().top
        assertTrue(boardBottom <= statusTop)
        assertTrue(statusTop <= scoreTop)
        assertTrue(scoreTop <= newGameTop)
    }

    @Test
    fun rendersNineCellsAndLocalizedCellDescription() {
        setScreen(GameUiState())

        (0 until 9).forEach { location ->
            composeTestRule.onNodeWithTag("tic_tac_toe_cell_$location")
                .assertExists()
                .assertIsEnabled()
        }
        composeTestRule.onAllNodes(
            hasAnyAncestor(hasTestTag(BOARD_TAG)) and hasClickAction(),
        )
            .assertCountEquals(9)
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.cell_empty, 1, 1),
        ).assertExists()
    }

    @Test
    fun rendersActiveTurnStatesFromResources() {
        setScreen(GameUiState())
        composeTestRule.onNodeWithText(context.getString(R.string.first_human)).assertExists()
    }

    @Test
    fun rendersComputerTurnFromResources() {
        setScreen(GameUiState(turn = Turn.Computer))
        composeTestRule.onNodeWithText(context.getString(R.string.turn_computer)).assertExists()
    }

    @Test
    fun rendersTieFromResources() {
        setScreen(GameUiState(result = GameResult.Tie))
        composeTestRule.onNodeWithText(context.getString(R.string.result_tie)).assertExists()
    }

    @Test
    fun rendersComputerResultFromResources() {
        setScreen(GameUiState(result = GameResult.Won(Player.O)))
        composeTestRule.onNodeWithText(context.getString(R.string.result_computer_wins)).assertExists()
    }

    @Test
    fun cellAndNewGameCallbacksAreOnlyInvokedByActions() {
        var clickedLocation: Int? = null
        var newGameClicks = 0
        setScreen(
            state = GameUiState(),
            onCellClick = { clickedLocation = it },
            onNewGame = { newGameClicks++ },
        )

        assertEquals(null, clickedLocation)
        assertEquals(0, newGameClicks)
        composeTestRule.onNodeWithTag("tic_tac_toe_cell_4").performScrollTo().performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.new_game))
            .performScrollTo()
            .performClick()
        assertEquals(4, clickedLocation)
        assertEquals(1, newGameClicks)
    }

    @Test
    fun recompositionDoesNotInvokeCallbacksOrChangeSnapshot() {
        val initialState = GameUiState()
        var renderedState by mutableStateOf(initialState)
        var callbackCount = 0

        composeTestRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TicTacToeScreen(
                        uiState = renderedState,
                        onCellClick = { callbackCount++ },
                        onNewGame = { callbackCount++ },
                    )
                }
            }
        }

        composeTestRule.runOnIdle {
            renderedState = GameUiState()
        }
        composeTestRule.waitForIdle()

        assertEquals(0, callbackCount)
        composeTestRule.onNodeWithText(context.getString(R.string.first_human)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.score_human_wins, 0)).assertExists()
    }

    @Test
    fun cellsArePositiveSquareAndStayInsideTheBoard() {
        viewport.setContent(320, 480) {
            TicTacToeTheme(dynamicColor = false) {
                Surface {
                    TicTacToeScreen(
                        uiState = GameUiState(),
                        onCellClick = {},
                        onNewGame = {},
                    )
                }
            }
        }

        val boardBounds = composeTestRule.onNodeWithTag(BOARD_TAG).getUnclippedBoundsInRoot()
        val tolerance = 1.dp
        (0 until 9).forEach { location ->
            val cellBounds = composeTestRule
                .onNodeWithTag("tic_tac_toe_cell_$location")
                .getUnclippedBoundsInRoot()
            val cellWidth = cellBounds.right - cellBounds.left
            val cellHeight = cellBounds.bottom - cellBounds.top

            assertTrue("cell $location must have positive bounds", cellWidth > 0.dp)
            assertTrue("cell $location must have positive bounds", cellHeight > 0.dp)
            assertTrue(
                "cell $location must be square",
                kotlin.math.abs((cellWidth - cellHeight).value) <= tolerance.value,
            )
            assertTrue(cellBounds.left >= boardBounds.left - tolerance)
            assertTrue(cellBounds.top >= boardBounds.top - tolerance)
            assertTrue(cellBounds.right <= boardBounds.right + tolerance)
            assertTrue(cellBounds.bottom <= boardBounds.bottom + tolerance)
        }
    }

    @Test
    fun verticalFlowAdaptsToPortraitAndShortLandscapeConstraints() {
        var newGameClicks = 0
        viewport.setContent(320, 480) {
            TicTacToeTheme(dynamicColor = false) {
                Surface {
                    TicTacToeScreen(
                        uiState = GameUiState(),
                        onCellClick = {},
                        onNewGame = { newGameClicks++ },
                    )
                }
            }
        }

        val boardWidths = mutableListOf<Dp>()
        listOf(320 to 480, 360 to 260).forEach { (width, height) ->
            viewport.show(width, height)

            val boardBeforeScroll = composeTestRule
                .onNodeWithTag(BOARD_TAG)
                .getUnclippedBoundsInRoot()
            val statusTop = composeTestRule.onNodeWithTag(STATUS_TAG).getUnclippedBoundsInRoot().top
            val scoreTop = composeTestRule.onNodeWithTag(SCORE_TAG).getUnclippedBoundsInRoot().top
            val newGameTop = composeTestRule
                .onNodeWithTag(NEW_GAME_TAG)
                .getUnclippedBoundsInRoot()
                .top
            assertTrue(boardBeforeScroll.bottom <= statusTop)
            assertTrue(statusTop <= scoreTop)
            assertTrue(scoreTop <= newGameTop)

            boardWidths += assertDisplayedSquareBoard(viewportWidth = width)

            composeTestRule.onNodeWithTag(STATUS_TAG)
                .performScrollTo()
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag(SCORE_TAG)
                .performScrollTo()
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag(NEW_GAME_TAG)
                .performScrollTo()
                .assertIsDisplayed()
                .assertIsEnabled()
                .performClick()
            assertEquals(boardWidths.size, newGameClicks)
        }
        assertTrue("landscape board should use the reduced height", boardWidths[1] < boardWidths[0])
    }

    @Test
    fun portraitViewportsKeepVerticalOrderWithMenuBesideNewGameAndEverythingReachable() {
        setMenuScreen(320, 480)

        listOf(ViewportCase(320, 480), ViewportCase(360, 640)).forEach { next ->
            viewport.show(next.width, next.height)

            val board = bounds(BOARD_TAG)
            val status = bounds(STATUS_TAG)
            val score = bounds(SCORE_TAG)
            val newGame = bounds(NEW_GAME_TAG)
            val menu = bounds(MENU_STUB_TAG)
            assertTrue("board above status", board.bottom <= status.top)
            assertTrue("status above score", status.bottom <= score.top)
            assertTrue("score above controls", score.bottom <= minOf(newGame.top, menu.top))
            assertTrue("menu shares the row of New game", menu.top < newGame.bottom && newGame.top < menu.bottom)
            assertTrue("menu right of New game", menu.left >= newGame.right)

            assertDisplayedSquareBoard(viewportWidth = next.width)
            assertControlsReachable(next)
        }
    }

    @Test
    fun extremeViewportsAndFontScalesKeepBoardAndControlsReachableAndNeverShrinkThePanel() {
        setMenuScreen(320, 192)

        // Compact height, narrow landscape at fontScale 2 (panel does not fit: vertical flow),
        // wide landscape at fontScale 2 (panel fits at its enlarged minimum) and the worst case.
        listOf(
            ViewportCase(320, 192, wide = false),
            ViewportCase(560, 320, fontScale = 2f, wide = false),
            ViewportCase(640, 360, fontScale = 2f, wide = true),
            ViewportCase(320, 192, fontScale = 2f, wide = false),
        ).forEach { next ->
            viewport.show(next.width, next.height, next.fontScale)

            assertDisplayedSquareBoard(viewportWidth = next.width)
            assertControlsReachable(next)

            val board = bounds(BOARD_TAG)
            val status = bounds(STATUS_TAG)
            val score = bounds(SCORE_TAG)
            val newGame = bounds(NEW_GAME_TAG)
            val menu = bounds(MENU_STUB_TAG)
            val tolerance = 1f
            if (next.wide) {
                assertTrue("wide: board left of the panel", board.right.value <= status.left.value + tolerance)
                assertTrue("wide: board left of the panel", board.right.value <= newGame.left.value + tolerance)
                val panelWidth = menu.right.value - newGame.left.value
                assertTrue(
                    "wide panel must keep its minimum ($panelWidth dp)",
                    panelWidth >= 200f * next.fontScale - tolerance,
                )
            } else {
                assertTrue("compact: vertical flow", board.bottom <= status.top)
                assertTrue("compact: vertical flow", status.bottom <= score.top)
                assertTrue("compact: vertical flow", score.bottom <= newGame.top)
            }
        }
    }

    @Test
    fun occupiedCellsKeepTheirMarksAndAreDisabled() {
        var cellCallbacks = 0
        setScreen(
            GameUiState(
                board = listOf(
                    Mark.X,
                    Mark.O,
                    Mark.Empty,
                    Mark.Empty,
                    Mark.Empty,
                    Mark.Empty,
                    Mark.Empty,
                    Mark.Empty,
                    Mark.Empty,
                ),
            ),
            onCellClick = { cellCallbacks++ },
        )

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.cell_x, 1, 1)).assertExists()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.cell_o, 1, 2)).assertExists()
        composeTestRule.onAllNodes(
            hasAnyAncestor(hasTestTag(BOARD_TAG)) and
                (hasText(Mark.X.symbol.toString()) or hasText(Mark.O.symbol.toString())),
        ).assertCountEquals(0)
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.cell_x, 1, 1))
            .assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.cell_o, 1, 2))
            .assertIsNotEnabled()
            .performTouchInput { click() }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.cell_x, 1, 1))
            .performTouchInput { click() }
        assertEquals(0, cellCallbacks)
    }

    @Test
    fun largeFontKeepsTerminalMarksVisibleAndControlsReachableInBothThemes() {
        var darkTheme by mutableStateOf(false)
        var newGameClicks = 0
        viewport.setContent(320, 260, fontScale = 2f) {
            TicTacToeTheme(darkTheme = darkTheme, dynamicColor = false) {
                Surface {
                    TicTacToeScreen(
                        uiState = GameUiState(
                            board = listOf(
                                Mark.X,
                                Mark.O,
                                Mark.Empty,
                                Mark.Empty,
                                Mark.Empty,
                                Mark.Empty,
                                Mark.Empty,
                                Mark.Empty,
                                Mark.Empty,
                            ),
                            result = GameResult.Won(Player.X),
                        ),
                        onCellClick = {},
                        onNewGame = { newGameClicks++ },
                    )
                }
            }
        }

        listOf(false, true).forEach { isDark ->
            composeTestRule.runOnIdle { darkTheme = isDark }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag(BOARD_TAG)
                .performScrollTo()
                .assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.cell_x, 1, 1),
            ).assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.cell_o, 1, 2),
            ).assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription(context.getString(R.string.cell_x, 1, 1))
                .assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription(context.getString(R.string.cell_o, 1, 2))
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag(NEW_GAME_TAG)
                .performScrollTo()
                .assertIsDisplayed()
                .performClick()
        }
        assertEquals(2, newGameClicks)
    }

    @Test
    fun realViewModelUiFlowCoversHumanComputerAndTieResults() {
        val viewModel = controlledViewModel()
        setViewModelScreen(viewModel)

        listOf(0, 3, 4, 8).forEach { clickCellAndAwaitComputerResponse(viewModel, it) }
        assertResultAndScore(R.string.result_human_wins, xWins = 1, oWins = 0, ties = 0)
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.cell_x, 1, 1))
            .assertExists()

        clickNewGame()
        awaitComputerTurn(viewModel)
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.cell_o, 1, 1))
            .assertExists()
        assertScore(xWins = 1, oWins = 0, ties = 0)
        composeTestRule.onNodeWithText(context.getString(R.string.turn_human))
            .performScrollTo()
            .assertIsDisplayed()

        listOf(1, 3, 5).forEach { clickCellAndAwaitComputerResponse(viewModel, it) }
        assertResultAndScore(R.string.result_computer_wins, xWins = 1, oWins = 1, ties = 0)
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.cell_o, 1, 1))
            .assertExists()

        clickNewGame()
        (0 until 9).forEach { location ->
            composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.cell_empty, location / 3 + 1, location % 3 + 1),
            ).assertExists()
        }
        composeTestRule.onNodeWithText(context.getString(R.string.first_human))
            .performScrollTo()
            .assertIsDisplayed()

        listOf(0, 4, 2, 7, 5).forEach { clickCellAndAwaitComputerResponse(viewModel, it) }
        assertResultAndScore(R.string.result_tie, xWins = 1, oWins = 1, ties = 1)
    }

    @Test
    fun realViewModelUiFlowAlternatesAfterAbandonmentWithoutScoringOrDuplicateOpening() {
        val viewModel = controlledViewModel()
        setViewModelScreen(viewModel)

        clickCell(0)
        composeTestRule.waitUntil(timeoutMillis = UI_STATE_TIMEOUT_MILLIS) {
            viewModel.uiState.value.turn == Turn.Computer
        }
        val beforeRejectedTouch = viewModel.uiState.value
        composeTestRule.onNodeWithTag("tic_tac_toe_cell_4")
            .assertIsNotEnabled()
            .performTouchInput { click() }
        composeTestRule.waitForIdle()
        val afterRejectedTouch = viewModel.uiState.value
        assertEquals(beforeRejectedTouch.board, afterRejectedTouch.board)
        assertEquals(beforeRejectedTouch.turn, afterRejectedTouch.turn)
        assertEquals(beforeRejectedTouch.result, afterRejectedTouch.result)
        assertEquals(beforeRejectedTouch.score, afterRejectedTouch.score)
        composeTestRule.waitUntil(timeoutMillis = UI_STATE_TIMEOUT_MILLIS) {
            viewModel.uiState.value.turn == Turn.Human ||
                viewModel.uiState.value.result != GameResult.InProgress
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.cell_x, 1, 1))
            .assertExists()
        assertScore(xWins = 0, oWins = 0, ties = 0)

        clickCellAndAwaitComputerResponse(viewModel, 4)
        assertScore(xWins = 0, oWins = 0, ties = 0)

        clickNewGame()
        awaitComputerTurn(viewModel)
        assertBoardHasExactly(xCount = 0, oCount = 1, emptyLocations = 1..8)
        assertScore(xWins = 0, oWins = 0, ties = 0)

        clickNewGame()
        assertBoardHasExactly(xCount = 0, oCount = 0, emptyLocations = 0..8)
        composeTestRule.onNodeWithText(context.getString(R.string.first_human))
            .performScrollTo()
            .assertIsDisplayed()
        assertScore(xWins = 0, oWins = 0, ties = 0)
    }

    @Test
    fun terminalResultsAndComputerTurnRejectCellTouchesButAllowNewGame() {
        val board = listOf(
            Mark.X,
            Mark.O,
            Mark.X,
            Mark.O,
            Mark.X,
            Mark.Empty,
            Mark.Empty,
            Mark.Empty,
            Mark.O,
        )
        var cellCallbacks = 0
        var renderedState by mutableStateOf(
            GameUiState(board = board, result = GameResult.Won(Player.X)),
        )
        var newGameCallbacks = 0
        composeTestRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TicTacToeScreen(
                        uiState = renderedState,
                        onCellClick = { cellCallbacks++ },
                        onNewGame = { newGameCallbacks++ },
                    )
                }
            }
        }

        val terminalResults = listOf(
            GameResult.Won(Player.X),
            GameResult.Won(Player.O),
            GameResult.Tie,
        )
        terminalResults.forEach { result ->
            composeTestRule.runOnIdle {
                renderedState = GameUiState(board = board, result = result)
            }
            composeTestRule.waitForIdle()
            (0 until 9).forEach { location ->
                composeTestRule.onNodeWithTag("tic_tac_toe_cell_$location")
                    .assertIsNotEnabled()
                    .performTouchInput { click() }
            }
        }
        assertEquals(0, cellCallbacks)

        composeTestRule.runOnIdle {
            renderedState = GameUiState(turn = Turn.Computer)
        }
        composeTestRule.waitForIdle()
        (0 until 9).forEach { location ->
            composeTestRule.onNodeWithTag("tic_tac_toe_cell_$location")
                .assertIsNotEnabled()
                .performTouchInput { click() }
        }
        assertEquals(0, cellCallbacks)

        composeTestRule.runOnIdle {
            renderedState = GameUiState(result = GameResult.Tie)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(NEW_GAME_TAG).performScrollTo().performClick()
        assertEquals(1, newGameCallbacks)
    }

    private fun setScreen(
        state: GameUiState,
        onCellClick: (Int) -> Unit = {},
        onNewGame: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TicTacToeScreen(
                        uiState = state,
                        onCellClick = onCellClick,
                        onNewGame = onNewGame,
                    )
                }
            }
        }
    }

    private fun controlledViewModel(): GameViewModel = GameViewModel(
        InMemoryGamePreferences(),
        SavedStateHandle(),
        TicTacToeGame(MoveRandomizer { 0 }),
    )

    private fun setViewModelScreen(viewModel: GameViewModel) {
        // This test host explicitly represents a RESUMED Activity; the ViewModel defaults paused.
        composeTestRule.runOnIdle { viewModel.onHostResumed() }
        composeTestRule.setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            TicTacToeTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TicTacToeScreen(
                        uiState = uiState,
                        onCellClick = viewModel::onHumanMove,
                        onNewGame = viewModel::newGame,
                    )
                }
            }
        }
    }

    private fun clickCell(location: Int) {
        composeTestRule.onNodeWithTag(BOARD_TAG).performScrollTo()
        composeTestRule.onNodeWithTag("tic_tac_toe_cell_$location")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun clickCellAndAwaitComputerResponse(viewModel: GameViewModel, location: Int) {
        clickCell(location)
        composeTestRule.waitUntil(timeoutMillis = UI_STATE_TIMEOUT_MILLIS) {
            val state = viewModel.uiState.value
            state.board[location] == Mark.X &&
                (state.turn == Turn.Human || state.result != GameResult.InProgress)
        }
        composeTestRule.waitForIdle()
    }

    private fun clickNewGame() {
        composeTestRule.onNodeWithTag(NEW_GAME_TAG)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun awaitComputerTurn(viewModel: GameViewModel) {
        composeTestRule.waitUntil(timeoutMillis = UI_STATE_TIMEOUT_MILLIS) {
            val state = viewModel.uiState.value
            state.turn == Turn.Human || state.result != GameResult.InProgress
        }
        composeTestRule.waitForIdle()
    }

    private fun assertResultAndScore(result: Int, xWins: Int, oWins: Int, ties: Int) {
        composeTestRule.onNodeWithText(context.getString(result))
            .performScrollTo()
            .assertIsDisplayed()
        assertScore(xWins, oWins, ties)
    }

    private fun assertScore(xWins: Int, oWins: Int, ties: Int) {
        composeTestRule.onNodeWithText(context.getString(R.string.score_human_wins, xWins))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.score_computer_wins, oWins))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.score_ties, ties))
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun assertBoardHasExactly(xCount: Int, oCount: Int, emptyLocations: IntRange) {
        var actualX = 0
        var actualO = 0
        var actualEmpty = 0
        (0 until 9).forEach { location ->
            val row = location / 3 + 1
            val column = location % 3 + 1
            val mark = when {
                composeTestRule.onAllNodes(hasContentDescription(context.getString(R.string.cell_x, row, column))).fetchSemanticsNodes().isNotEmpty() -> Mark.X
                composeTestRule.onAllNodes(hasContentDescription(context.getString(R.string.cell_o, row, column))).fetchSemanticsNodes().isNotEmpty() -> Mark.O
                else -> Mark.Empty
            }
            when (mark) {
                Mark.X -> actualX++
                Mark.O -> actualO++
                Mark.Empty -> actualEmpty++
            }
            if (location in emptyLocations) assertEquals(Mark.Empty, mark)
        }
        assertEquals(xCount, actualX)
        assertEquals(oCount, actualO)
        assertEquals(9 - xCount - oCount, actualEmpty)
        emptyLocations.forEach { location ->
            composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.cell_empty, location / 3 + 1, location % 3 + 1),
            ).assertExists()
        }
    }

    private data class ViewportCase(
        val width: Int,
        val height: Int,
        val fontScale: Float = 1f,
        val wide: Boolean = false,
    )

    private fun setMenuScreen(width: Int, height: Int) {
        viewport.setContent(width, height) {
            TicTacToeTheme(dynamicColor = false) {
                Surface {
                    TicTacToeScreen(
                        uiState = GameUiState(),
                        onCellClick = {},
                        onNewGame = {},
                        menu = { Box(Modifier.size(48.dp).testTag(MENU_STUB_TAG)) },
                    )
                }
            }
        }
    }

    private fun bounds(tag: String) = composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun assertControlsReachable(spec: ViewportCase) {
        listOf(STATUS_TAG, SCORE_TAG, NEW_GAME_TAG, MENU_STUB_TAG).forEach { tag ->
            composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
            val box = bounds(tag)
            assertTrue("$tag must not be clipped horizontally", box.left >= 0.dp - 1.dp)
            assertTrue("$tag must not be clipped horizontally", box.right <= spec.width.dp + 1.dp)
        }
        composeTestRule.onNodeWithTag(NEW_GAME_TAG).assertIsEnabled()
        val newGame = bounds(NEW_GAME_TAG)
        assertTrue("New game must be at least 48 dp tall", newGame.bottom - newGame.top >= 48.dp - 1.dp)
    }

    private fun assertDisplayedSquareBoard(viewportWidth: Int): Dp {
        composeTestRule.onNodeWithTag(BOARD_TAG)
            .performScrollTo()
            .assertIsDisplayed()

        val boardBounds = composeTestRule.onNodeWithTag(BOARD_TAG).getUnclippedBoundsInRoot()
        val tolerance = 1.dp
        val boardWidth = boardBounds.right - boardBounds.left
        val boardHeight = boardBounds.bottom - boardBounds.top
        assertTrue(boardWidth > 0.dp)
        assertTrue(boardWidth <= viewportWidth.dp + tolerance)
        assertTrue(boardBounds.left >= 0.dp - tolerance)
        assertTrue(boardBounds.right <= viewportWidth.dp + tolerance)
        assertTrue(
            "board must be square",
            kotlin.math.abs((boardWidth - boardHeight).value) <= tolerance.value,
        )

        (0 until 9).forEach { location ->
            val cellInteraction = composeTestRule.onNodeWithTag("tic_tac_toe_cell_$location")
            cellInteraction.assertIsDisplayed()
            val cellBounds = cellInteraction.getUnclippedBoundsInRoot()
            val cellWidth = cellBounds.right - cellBounds.left
            val cellHeight = cellBounds.bottom - cellBounds.top
            assertTrue("cell $location must have a usable minimum size", cellWidth >= 48.dp - tolerance)
            assertTrue("cell $location must have a usable minimum size", cellHeight >= 48.dp - tolerance)
            assertTrue(
                "cell $location must be square",
                kotlin.math.abs((cellWidth - cellHeight).value) <= tolerance.value,
            )
            assertTrue(cellBounds.left >= boardBounds.left - tolerance)
            assertTrue(cellBounds.top >= boardBounds.top - tolerance)
            assertTrue(cellBounds.right <= boardBounds.right + tolerance)
            assertTrue(cellBounds.bottom <= boardBounds.bottom + tolerance)
        }
        return boardWidth
    }

}
