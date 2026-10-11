package com.edu.unal.tictactoe.ui.online.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.ui.game.InputGameBoard
import com.edu.unal.tictactoe.ui.game.minimumGameBoardSizePx
import kotlin.math.roundToInt

internal object OnlineMatchTags {
    const val MARK = "match_mark"
    const val OPPONENT = "match_opponent"
    const val STATUS = "match_status"
    const val OFFLINE = "match_offline"
    const val LEAVE = "match_leave"
    const val BACK_TO_LOBBY = "match_back_to_lobby"
}

private val ScreenPadding = 16.dp
private val SectionGap = 12.dp
private val MinActionHeight = 48.dp

/** Reference (maximum, not required) side of the board in the wide layout. */
private val WideBoardReference = 270.dp
private val WidePanelGap = 16.dp

/** Narrowest right-hand panel of the wide layout at fontScale 1; it grows with the font scale. */
private val WidePanelMinWidth = 200.dp

/**
 * Online match screen: the mark of the person, the rival, the turn or the outcome, the neutral
 * board and one action, without score and without options menu. It is stateless and never talks
 * to the view model.
 *
 * - Playing: turn text, board and "Salir". [onLeaveDialogOpenChange] receives true when "Salir"
 *   is pressed; the "¿Abandonar la partida?" dialog is shown while [isLeaveDialogOpen] is true
 *   and the match is still playing. The flag lives in the caller, so that "Salir" and any other
 *   source (Back, handled by navigation) open the same dialog and the caller can keep it across
 *   rotation. "Cancelar" reports false; "Abandonar" reports false and then calls [onLeaveConfirmed].
 * - Finished or abandoned: the outcome, the locked board and "Volver al lobby" ([onBackToLobby]).
 * - Loading: no board, turn, result or action.
 *
 * Layout, from the constraints left after the system insets: in a landscape with room the square
 * board is on the left and the state and the action on the right; otherwise a vertical flow with
 * the board first. Content scrolls, so nothing is clipped with little space or a scaled font.
 * There is no top bar.
 */
@Composable
fun OnlineMatchScreen(
    uiState: OnlineMatchUiState,
    isLeaveDialogOpen: Boolean,
    onLeaveDialogOpenChange: (Boolean) -> Unit,
    onLeaveConfirmed: () -> Unit,
    onCellClick: (Int) -> Unit,
    onBackToLobby: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        val density = LocalDensity.current
        BoxWithConstraints(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            val contentHeight = if (maxHeight == Dp.Infinity) {
                Dp.Infinity
            } else {
                (maxHeight - ScreenPadding * 2).coerceAtLeast(0.dp)
            }
            val wideBoardSize = density.wideBoardSizePx(
                maxWidthPx = constraints.maxWidth,
                maxHeightPx = constraints.maxHeight,
            )
            val info = @Composable {
                MatchInfo(
                    uiState = uiState,
                    onLeave = { onLeaveDialogOpenChange(true) },
                    onBackToLobby = onBackToLobby,
                )
            }
            val board = @Composable {
                InputGameBoard(
                    board = uiState.board,
                    inputEnabled = uiState.isInputEnabled,
                    onCellClick = onCellClick,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(ScreenPadding),
            ) {
                when {
                    uiState.phase == MatchPhase.Loading -> info()

                    wideBoardSize != null -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (contentHeight == Dp.Infinity) {
                                    Modifier
                                } else {
                                    Modifier.heightIn(min = contentHeight)
                                },
                            ),
                        horizontalArrangement = Arrangement.spacedBy(WidePanelGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.size(with(density) { wideBoardSize.toDp() })) {
                            board()
                        }
                        Box(modifier = Modifier.weight(1f)) { info() }
                    }

                    else -> CompactContent(
                        viewportHeight = contentHeight,
                        board = board,
                        info = info,
                    )
                }
            }
        }
    }

    if (isLeaveDialogOpen && uiState.phase == MatchPhase.Playing) {
        LeaveMatchDialog(
            onConfirm = {
                onLeaveDialogOpenChange(false)
                onLeaveConfirmed()
            },
            onDismiss = { onLeaveDialogOpenChange(false) },
        )
    }
}

/**
 * Side of the board in the wide layout, in pixels, or null when the vertical flow applies: it is
 * used unless the width exceeds the height and the board (at most 270 dp, at least the board that
 * keeps nine cells at 48 dp) leaves a panel of 200 dp times the font scale (at least 1).
 */
private fun Density.wideBoardSizePx(maxWidthPx: Int, maxHeightPx: Int): Int? {
    if (maxWidthPx <= maxHeightPx) return null
    val padding = ScreenPadding.roundToPx() * 2
    val contentWidth = (maxWidthPx - padding).coerceAtLeast(0)
    val contentHeight = (maxHeightPx - padding).coerceAtLeast(0)
    val minimumBoard = minimumGameBoardSizePx()
    val minimumPanel = (WidePanelMinWidth.toPx() * fontScale.coerceAtLeast(1f)).roundToInt()
    val widthLeftForBoard = contentWidth - WidePanelGap.roundToPx() - minimumPanel
    val side = minOf(
        WideBoardReference.roundToPx(),
        maxOf(minimumBoard, contentHeight),
        widthLeftForBoard,
    )
    return side.takeIf { it >= minimumBoard }
}

/** Vertical flow: the board, as wide as the space allows, and then the information below it. */
@Composable
private fun CompactContent(
    viewportHeight: Dp,
    board: @Composable () -> Unit,
    info: @Composable () -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            board()
            info()
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val infoPlaceable = measurables[1].measure(constraints.copy(minWidth = 0, maxWidth = width))
        val gap = SectionGap.roundToPx()
        val availableBoardHeight = if (viewportHeight == Dp.Infinity) {
            Int.MAX_VALUE
        } else {
            viewportHeight.roundToPx() - infoPlaceable.height - gap
        }
        val boardSize = minOf(width, maxOf(minimumGameBoardSizePx(), availableBoardHeight))
        val boardPlaceable = measurables[0].measure(Constraints.fixed(boardSize, boardSize))

        layout(width, boardPlaceable.height + gap + infoPlaceable.height) {
            boardPlaceable.placeRelative((width - boardPlaceable.width) / 2, 0)
            infoPlaceable.placeRelative(0, boardPlaceable.height + gap)
        }
    }
}

@Composable
private fun MatchInfo(
    uiState: OnlineMatchUiState,
    onLeave: () -> Unit,
    onBackToLobby: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        uiState.myMark?.let { mark ->
            Text(
                text = stringResource(
                    when (mark) {
                        Player.X -> R.string.match_mark_x
                        Player.O -> R.string.match_mark_o
                    },
                ),
                modifier = Modifier.testTag(OnlineMatchTags.MARK),
            )
        }
        uiState.opponentName?.let { name ->
            Text(
                text = stringResource(R.string.match_opponent, name),
                modifier = Modifier.testTag(OnlineMatchTags.OPPONENT),
            )
        }
        if (uiState.isOfflineNoticeVisible) {
            Text(
                text = stringResource(R.string.lobby_offline),
                modifier = Modifier.testTag(OnlineMatchTags.OFFLINE),
            )
        }
        statusText(uiState)?.let { status ->
            Text(text = status, modifier = Modifier.testTag(OnlineMatchTags.STATUS))
        }
        when (uiState.phase) {
            MatchPhase.Playing -> OutlinedButton(
                onClick = onLeave,
                modifier = Modifier
                    .heightIn(min = MinActionHeight)
                    .testTag(OnlineMatchTags.LEAVE),
            ) {
                Text(text = stringResource(R.string.match_leave))
            }

            MatchPhase.Finished, MatchPhase.Abandoned -> Button(
                onClick = onBackToLobby,
                modifier = Modifier
                    .heightIn(min = MinActionHeight)
                    .testTag(OnlineMatchTags.BACK_TO_LOBBY),
            ) {
                Text(text = stringResource(R.string.match_back_to_lobby))
            }

            MatchPhase.Loading -> Unit
        }
    }
}

/** The turn, the result or the abandonment text; null while loading or when a name is missing. */
@Composable
private fun statusText(uiState: OnlineMatchUiState): String? {
    val name = uiState.opponentName
    return when (uiState.phase) {
        MatchPhase.Loading -> null
        MatchPhase.Playing -> when {
            uiState.isMyTurn -> stringResource(R.string.turn_human)
            name != null -> stringResource(R.string.match_turn_opponent, name)
            else -> null
        }

        MatchPhase.Finished -> when (uiState.result) {
            MatchResult.Won -> stringResource(R.string.match_result_win)
            MatchResult.Lost -> name?.let { stringResource(R.string.match_result_opponent_wins, it) }
            MatchResult.Tie -> stringResource(R.string.result_tie)
            null -> null
        }

        MatchPhase.Abandoned -> stringResource(R.string.match_opponent_left)
    }
}
