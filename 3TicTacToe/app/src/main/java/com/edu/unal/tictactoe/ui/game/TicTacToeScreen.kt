package com.edu.unal.tictactoe.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.unal.tictactoe.R
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import com.edu.unal.tictactoe.ui.theme.TicTacToeODark
import com.edu.unal.tictactoe.ui.theme.TicTacToeOLight
import com.edu.unal.tictactoe.ui.theme.TicTacToeXDark
import com.edu.unal.tictactoe.ui.theme.TicTacToeXLight

private const val BOARD_TAG = "tic_tac_toe_board"
private const val STATUS_TAG = "tic_tac_toe_status"
private const val SCORE_TAG = "tic_tac_toe_score"
private const val NEW_GAME_TAG = "tic_tac_toe_new_game"
private val CELL_PADDING = 2.dp
private val MINIMUM_CELL_TOUCH_TARGET = 48.dp
private val MINIMUM_BOARD_SIZE =
    (MINIMUM_CELL_TOUCH_TARGET + CELL_PADDING * 2f) * 3f

@Composable
fun TicTacToeScreen(
    uiState: GameUiState,
    onCellClick: (Int) -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val contentHeight = if (maxHeight == Dp.Infinity) {
            Dp.Infinity
        } else {
            (maxHeight - 32.dp).coerceAtLeast(0.dp)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            GameContent(
                uiState = uiState,
                onCellClick = onCellClick,
                onNewGame = onNewGame,
                viewportHeight = contentHeight,
            )
        }
    }
}

@Composable
private fun GameContent(
    uiState: GameUiState,
    onCellClick: (Int) -> Unit,
    onNewGame: () -> Unit,
    viewportHeight: Dp,
) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Board(
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

            OutlinedButton(
                onClick = onNewGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NEW_GAME_TAG),
            ) {
                Text(text = stringResource(R.string.new_game))
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val controlConstraints = constraints.copy(minWidth = 0, maxWidth = width)
        val statusPlaceable = measurables[1].measure(controlConstraints)
        val scorePlaceable = measurables[2].measure(controlConstraints)
        val newGamePlaceable = measurables[3].measure(controlConstraints)

        val gap = 12.dp.roundToPx()
        val controlsHeight = statusPlaceable.height + scorePlaceable.height + newGamePlaceable.height
        val availableBoardHeight = if (viewportHeight == Dp.Infinity) {
            Int.MAX_VALUE
        } else {
            viewportHeight.roundToPx() - controlsHeight - gap * 3
        }
        val minimumBoardSize = MINIMUM_BOARD_SIZE.roundToPx()
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
private fun Board(
    board: List<Mark>,
    isGameActive: Boolean,
    turn: Turn,
    onCellClick: (Int) -> Unit,
) {
    val boardDescription = stringResource(R.string.board_label)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag(BOARD_TAG)
            .semantics {
                contentDescription = boardDescription
            },
    ) {
        repeat(3) { row ->
            Row(modifier = Modifier.weight(1f)) {
                repeat(3) { column ->
                    val location = row * 3 + column
                    val mark = board[location]
                    Cell(
                        location = location,
                        row = row,
                        column = column,
                        mark = mark,
                        enabled = mark == Mark.Empty &&
                            isGameActive &&
                            turn == Turn.Human,
                        modifier = Modifier.weight(1f),
                        onClick = onCellClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun Cell(
    location: Int,
    row: Int,
    column: Int,
    mark: Mark,
    enabled: Boolean,
    modifier: Modifier,
    onClick: (Int) -> Unit,
) {
    val description = when (mark) {
        Mark.Empty -> stringResource(R.string.cell_empty, row + 1, column + 1)
        Mark.X -> stringResource(R.string.cell_x, row + 1, column + 1)
        Mark.O -> stringResource(R.string.cell_o, row + 1, column + 1)
    }
    val markColor = markContentColor(mark)
    val cellContainerColor = MaterialTheme.colorScheme.surfaceVariant

    Button(
        onClick = { onClick(location) },
        enabled = enabled,
        modifier = modifier
            .fillMaxSize()
            .padding(CELL_PADDING)
            .testTag("tic_tac_toe_cell_$location")
            .semantics { contentDescription = description },
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = cellContainerColor,
            contentColor = markColor,
            disabledContainerColor = cellContainerColor,
            disabledContentColor = markColor,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val fontScale = LocalDensity.current.fontScale
            val availableSize = minOf(maxWidth, maxHeight)
            val fontSize = if (availableSize == Dp.Infinity) {
                32.sp
            } else {
                (availableSize.value * 0.55f / fontScale)
                    .coerceAtLeast(1f)
                    .sp
            }
            Text(
                text = mark.symbol.toString(),
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = fontSize,
                    lineHeight = (fontSize.value * 1.1f).sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
private fun markContentColor(mark: Mark): Color {
    val usesDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (mark) {
        Mark.X -> if (usesDarkSurface) TicTacToeXDark else TicTacToeXLight
        Mark.O -> if (usesDarkSurface) TicTacToeODark else TicTacToeOLight
        Mark.Empty -> MaterialTheme.colorScheme.onSurfaceVariant
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
