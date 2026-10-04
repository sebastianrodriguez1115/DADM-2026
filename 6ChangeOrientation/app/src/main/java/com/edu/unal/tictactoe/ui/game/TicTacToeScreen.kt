package com.edu.unal.tictactoe.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.edu.unal.tictactoe.R
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.models.GameUiState

private const val STATUS_TAG = "tic_tac_toe_status"
private const val SCORE_TAG = "tic_tac_toe_score"
private const val NEW_GAME_TAG = "tic_tac_toe_new_game"

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
