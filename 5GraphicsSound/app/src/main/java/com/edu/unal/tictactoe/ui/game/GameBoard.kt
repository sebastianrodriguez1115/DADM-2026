package com.edu.unal.tictactoe.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.edu.unal.tictactoe.R
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.theme.TicTacToeODark
import com.edu.unal.tictactoe.ui.theme.TicTacToeOLight
import com.edu.unal.tictactoe.ui.theme.TicTacToeXDark
import com.edu.unal.tictactoe.ui.theme.TicTacToeXLight
import kotlin.math.ceil

private const val BOARD_TAG = "tic_tac_toe_board"
private val CELL_PADDING = 2.dp
private val MINIMUM_CELL_TARGET = 48.dp

internal fun Density.minimumGameBoardSizePx(): Int {
    val minimumCellContent = ceil(MINIMUM_CELL_TARGET.value * density).toInt()
    val cellPadding = CELL_PADDING.roundToPx()
    return (minimumCellContent + cellPadding * 2) * 3
}

@Composable
internal fun GameBoard(
    board: List<Mark>,
    isGameActive: Boolean,
    turn: Turn,
    onCellClick: (Int) -> Unit,
) {
    val boardDescription = stringResource(R.string.board_label)
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag(BOARD_TAG)
            .semantics { contentDescription = boardDescription },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            repeat(3) { row ->
                Row(modifier = Modifier.weight(1f)) {
                    repeat(3) { column ->
                        val location = row * 3 + column
                        val mark = board[location]
                        GameCell(
                            location = location,
                            row = row,
                            column = column,
                            mark = mark,
                            enabled = mark == Mark.Empty && isGameActive && turn == Turn.Human,
                            modifier = Modifier.weight(1f),
                            onClick = onCellClick,
                        )
                    }
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            for (division in 1..2) {
                val positionX = size.width * division / 3f
                val positionY = size.height * division / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(positionX, 0f),
                    end = Offset(positionX, size.height),
                    strokeWidth = strokeWidth,
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, positionY),
                    end = Offset(size.width, positionY),
                    strokeWidth = strokeWidth,
                )
            }
        }
    }
}

@Composable
private fun GameCell(
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(CELL_PADDING)
            .testTag("tic_tac_toe_cell_$location")
            .then(
                if (enabled) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = { onClick(location) },
                    ).semantics { contentDescription = description }
                } else {
                    Modifier.semantics {
                        contentDescription = description
                        role = Role.Button
                        disabled()
                    }
                },
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.085f
            val inset = size.minDimension * 0.25f
            when (mark) {
                Mark.X -> {
                    drawLine(
                        markColor,
                        Offset(inset, inset),
                        Offset(size.width - inset, size.height - inset),
                        strokeWidth,
                    )
                    drawLine(
                        markColor,
                        Offset(size.width - inset, inset),
                        Offset(inset, size.height - inset),
                        strokeWidth,
                    )
                }
                Mark.O -> drawCircle(
                    color = markColor,
                    radius = size.minDimension * 0.25f,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = strokeWidth),
                )
                Mark.Empty -> Unit
            }
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
