package com.edu.unal.tictactoe.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.edu.unal.tictactoe.R
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import kotlin.math.roundToInt

private const val STATUS_TAG = "tic_tac_toe_status"
private const val SCORE_TAG = "tic_tac_toe_score"
private const val NEW_GAME_TAG = "tic_tac_toe_new_game"

private val SCREEN_PADDING = 16.dp
private val SECTION_GAP = 12.dp
private val CONTROLS_GAP = 8.dp
private val MINIMUM_TOUCH_TARGET = 48.dp

/** Reference (maximum, not required) side of the board in the wide layout. */
private val WIDE_BOARD_REFERENCE = 270.dp

/** Horizontal space between the board and the right-hand panel in the wide layout. */
private val WIDE_PANEL_GAP = 16.dp

/**
 * Narrowest right-hand panel that still fits the "New game" button next to the menu button at
 * fontScale 1. It is multiplied by the font scale (never below 1) so that larger text asks for a
 * wider panel; when the board and this panel do not both fit, the compact layout is used.
 */
private val WIDE_PANEL_MIN_WIDTH = 200.dp

/**
 * Main game content: board, status, score and controls (the "New game" shortcut plus the
 * compact [menu] button).
 *
 * The layout depends only on the constraints available to this composable (after the window
 * insets applied by the caller), never on resources or on the configuration orientation:
 *
 * - **Wide layout** (landscape with room): used when the available width exceeds the available
 *   height and, after reserving the board and [WIDE_PANEL_GAP], the right-hand panel keeps at
 *   least [WIDE_PANEL_MIN_WIDTH] times the font scale (at least 1). The square board is on the
 *   left; status, score and controls are stacked on the right. The board side is the smallest of
 *   [WIDE_BOARD_REFERENCE] (270 dp), the available height and the width left by the panel, and it
 *   is never smaller than the board that keeps nine cells at 48 dp.
 * - **Compact layout** (portrait and narrow or short viewports, including very large font
 *   scales): a single vertical flow of board, status, score and controls. The board takes the
 *   available width, reduced to the height left by the other items but never below the 48 dp cell
 *   minimum.
 *
 * 270 dp is only a reference maximum: when the constraints do not allow it, the board is smaller,
 * and when even the minimum board does not fit next to the panel the compact layout is used.
 *
 * Content is padded by 16 dp and scrolls vertically, so the board and the controls stay reachable
 * for extreme sizes, insets or font scales; text has no fixed heights. Window insets (system bars
 * and display cutouts) are the caller's responsibility, see [GameScreen]. Interactive targets
 * (cells, "New game", menu button) are at least 48 dp, and the semantics and test tags of every
 * element are the same in both layouts.
 *
 * @param menu the compact options menu, placed next to the "New game" button.
 */
@Composable
fun TicTacToeScreen(
    uiState: GameUiState,
    onCellClick: (Int) -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
    menu: @Composable () -> Unit = {},
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val contentHeight = if (maxHeight == Dp.Infinity) {
            Dp.Infinity
        } else {
            (maxHeight - SCREEN_PADDING * 2).coerceAtLeast(0.dp)
        }
        val wideBoardSize = density.wideBoardSizePx(
            maxWidthPx = constraints.maxWidth,
            maxHeightPx = constraints.maxHeight,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SCREEN_PADDING)
                .verticalScroll(rememberScrollState()),
        ) {
            if (wideBoardSize != null) {
                WideGameContent(
                    uiState = uiState,
                    onCellClick = onCellClick,
                    onNewGame = onNewGame,
                    menu = menu,
                    boardSize = with(density) { wideBoardSize.toDp() },
                    viewportHeight = contentHeight,
                )
            } else {
                GameContent(
                    uiState = uiState,
                    onCellClick = onCellClick,
                    onNewGame = onNewGame,
                    menu = menu,
                    viewportHeight = contentHeight,
                )
            }
        }
    }
}

/**
 * Side of the board in the wide layout, in pixels, or null when the compact layout applies.
 * See [TicTacToeScreen] for the rule.
 */
private fun Density.wideBoardSizePx(maxWidthPx: Int, maxHeightPx: Int): Int? {
    if (maxWidthPx <= maxHeightPx) return null
    val padding = SCREEN_PADDING.roundToPx() * 2
    val contentWidth = (maxWidthPx - padding).coerceAtLeast(0)
    val contentHeight = (maxHeightPx - padding).coerceAtLeast(0)
    val minimumBoard = minimumGameBoardSizePx()
    val minimumPanel = (WIDE_PANEL_MIN_WIDTH.toPx() * fontScale.coerceAtLeast(1f)).roundToInt()
    val widthLeftForBoard = contentWidth - WIDE_PANEL_GAP.roundToPx() - minimumPanel
    val side = minOf(
        WIDE_BOARD_REFERENCE.roundToPx(),
        maxOf(minimumBoard, contentHeight),
        widthLeftForBoard,
    )
    return side.takeIf { it >= minimumBoard }
}

@Composable
private fun WideGameContent(
    uiState: GameUiState,
    onCellClick: (Int) -> Unit,
    onNewGame: () -> Unit,
    menu: @Composable () -> Unit,
    boardSize: Dp,
    viewportHeight: Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (viewportHeight == Dp.Infinity) Modifier else Modifier.heightIn(min = viewportHeight)),
        horizontalArrangement = Arrangement.spacedBy(WIDE_PANEL_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(boardSize)) {
            GameBoard(
                board = uiState.board,
                isGameActive = uiState.result == GameResult.InProgress,
                turn = uiState.turn,
                onCellClick = onCellClick,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
        ) {
            Text(
                text = statusText(uiState),
                modifier = Modifier.testTag(STATUS_TAG),
            )
            ScoreSummary(
                uiState = uiState,
                modifier = Modifier.testTag(SCORE_TAG),
            )
            GameControls(onNewGame = onNewGame, menu = menu)
        }
    }
}

@Composable
private fun GameControls(
    onNewGame: () -> Unit,
    menu: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CONTROLS_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onNewGame,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MINIMUM_TOUCH_TARGET)
                .testTag(NEW_GAME_TAG),
        ) {
            Text(text = stringResource(R.string.new_game))
        }
        menu()
    }
}

@Composable
private fun GameContent(
    uiState: GameUiState,
    onCellClick: (Int) -> Unit,
    onNewGame: () -> Unit,
    menu: @Composable () -> Unit,
    viewportHeight: Dp,
) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            GameBoard(
                board = uiState.board,
                isGameActive = uiState.result == GameResult.InProgress,
                turn = uiState.turn,
                onCellClick = onCellClick,
            )

            Text(
                text = statusText(uiState),
                modifier = Modifier.testTag(STATUS_TAG),
            )

            ScoreSummary(
                uiState = uiState,
                modifier = Modifier.testTag(SCORE_TAG),
            )

            GameControls(onNewGame = onNewGame, menu = menu)
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val controlConstraints = constraints.copy(minWidth = 0, maxWidth = width)
        val statusPlaceable = measurables[1].measure(controlConstraints)
        val scorePlaceable = measurables[2].measure(controlConstraints)
        val newGamePlaceable = measurables[3].measure(controlConstraints)

        val gap = SECTION_GAP.roundToPx()
        val controlsHeight = statusPlaceable.height + scorePlaceable.height + newGamePlaceable.height
        val availableBoardHeight = if (viewportHeight == Dp.Infinity) {
            Int.MAX_VALUE
        } else {
            viewportHeight.roundToPx() - controlsHeight - gap * 3
        }
        val minimumBoardSize = minimumGameBoardSizePx()
        val boardSize = minOf(
            width,
            maxOf(minimumBoardSize, availableBoardHeight),
        )
        val boardPlaceable = measurables[0].measure(Constraints.fixed(boardSize, boardSize))
        val totalHeight = boardPlaceable.height + controlsHeight + gap * 3

        layout(width, totalHeight) {
            var y = 0
            boardPlaceable.placeRelative((width - boardPlaceable.width) / 2, y)
            y += boardPlaceable.height + gap
            statusPlaceable.placeRelative(0, y)
            y += statusPlaceable.height + gap
            scorePlaceable.placeRelative(0, y)
            y += scorePlaceable.height + gap
            newGamePlaceable.placeRelative(0, y)
        }
    }
}

@Composable
private fun ScoreSummary(
    uiState: GameUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = stringResource(R.string.score_label))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = stringResource(R.string.score_human_wins, uiState.score.xWins))
            Text(text = stringResource(R.string.score_computer_wins, uiState.score.oWins))
            Text(text = stringResource(R.string.score_ties, uiState.score.ties))
        }
    }
}

@Composable
private fun statusText(uiState: GameUiState): String = when (val result = uiState.result) {
    GameResult.InProgress -> when (uiState.turn) {
        Turn.Computer -> stringResource(R.string.turn_computer)
        Turn.Human -> if (uiState.board.all { it == Mark.Empty }) {
            stringResource(R.string.first_human)
        } else {
            stringResource(R.string.turn_human)
        }
    }

    GameResult.Tie -> stringResource(R.string.result_tie)
    is GameResult.Won -> when (result.player) {
        Player.X -> stringResource(R.string.result_human_wins)
        Player.O -> stringResource(R.string.result_computer_wins)
    }
}
