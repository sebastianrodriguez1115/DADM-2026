package com.edu.unal.tictactoe.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.edu.unal.tictactoe.ui.game.DifficultyLabels
import com.edu.unal.tictactoe.ui.game.GameScreen
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundSink
import com.edu.unal.tictactoe.ui.online.lobby.LobbyMessage
import com.edu.unal.tictactoe.ui.online.lobby.LobbySignal
import com.edu.unal.tictactoe.ui.online.lobby.MatchTicket
import com.edu.unal.tictactoe.ui.online.lobby.NicknameDialog
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyScreen
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyViewModel
import com.edu.unal.tictactoe.ui.online.lobby.WaitingForOpponentScreen
import com.edu.unal.tictactoe.ui.online.match.MatchPhase
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchScreen
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchViewModel
import com.edu.unal.tictactoe.ui.share.ShareAppDialog
import com.edu.unal.tictactoe.ui.start.StartScreen

/**
 * Root composable: draws the screen held by [navigationViewModel] and declares the Back handlers.
 *
 * Back goes from [AppScreen.Local] to [AppScreen.Start]; in [AppScreen.Lobby] it cancels the own
 * waiting game, if any, and otherwise goes to [AppScreen.Start]. [AppScreen.Start] declares no
 * handler, so the system applies its normal Back behavior. A menu or dialog is its own window and
 * consumes Back before these handlers; the nickname dialog's Back is the lobby ViewModel's call.
 *
 * The lobby is driven by [lobbyViewModel]. Its one-shot signals are consumed here, one at a time:
 * a game that became [LobbySignal.MatchReady] opens the match in [matchViewModel], once, and then
 * [AppScreen.Match]. The signal is consumed before it is handled, so a recomposition or a rotation
 * never opens the match again, and the Activity-scoped [matchViewModel] keeps its state meanwhile.
 *
 * In [AppScreen.Match] Back opens the "leave the match" confirmation while the game is in
 * progress and, in any other phase (finished, abandoned or connection lost), leaves without
 * asking. "Abandonar" and "Volver al lobby" leave the match and return to [AppScreen.Lobby].
 * Whether the confirmation is open lives here, saved across rotation. The match's accepted moves
 * are played on [onlineMoveSound], collected only while the lifecycle is RESUMED. A
 * [AppScreen.Match] without an open match goes back to [AppScreen.Lobby].
 *
 * [shareLink] is the install link the lobby offers as a QR code ("Compartir app"); empty means
 * there is none and the lobby shows no such action. Whether the QR dialog is open lives in the
 * lobby destination, saved across rotation; it is its own window, so it consumes Back.
 *
 * Every screen's state holder is a parameter, so a test can mount this with fakes and without the
 * real Activity.
 */
@Composable
fun AppNavigation(
    navigationViewModel: NavigationViewModel,
    gameViewModel: GameViewModel,
    lobbyViewModel: OnlineLobbyViewModel,
    matchViewModel: OnlineMatchViewModel,
    onlineMoveSound: MoveSoundSink,
    modifier: Modifier = Modifier,
    shareLink: String = "",
) {
    val currentScreen by navigationViewModel.currentScreen.collectAsStateWithLifecycle()
    var isLeaveDialogOpen by rememberSaveable { mutableStateOf(false) }

    when (currentScreen) {
        AppScreen.Start -> StartScreen(
            onPlayLocal = { navigationViewModel.navigateTo(AppScreen.Local) },
            onPlayOnline = { navigationViewModel.navigateTo(AppScreen.Lobby) },
            modifier = modifier,
        )

        AppScreen.Local -> {
            BackHandler { navigationViewModel.navigateTo(AppScreen.Start) }
            val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current.applicationContext
            GameScreen(
                uiState = uiState,
                onCellClick = gameViewModel::onHumanMove,
                onNewGame = gameViewModel::newGame,
                onToggleSound = gameViewModel::toggleSound,
                onDifficultySelected = gameViewModel::setDifficulty,
                onShowToast = { level ->
                    Toast.makeText(
                        context,
                        DifficultyLabels.labelRes(level),
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                onResetScores = gameViewModel::resetScores,
                onBackToStart = { navigationViewModel.navigateTo(AppScreen.Start) },
                modifier = modifier,
            )
        }

        AppScreen.Lobby -> LobbyDestination(
            lobbyViewModel = lobbyViewModel,
            shareLink = shareLink,
            onBackToStart = { navigationViewModel.navigateTo(AppScreen.Start) },
            onMatchReady = { ticket ->
                isLeaveDialogOpen = false
                matchViewModel.openMatch(ticket.gameId, ticket.myMark)
                navigationViewModel.navigateTo(AppScreen.Match)
            },
            modifier = modifier,
        )

        AppScreen.Match -> MatchDestination(
            matchViewModel = matchViewModel,
            moveSound = onlineMoveSound,
            isLeaveDialogOpen = isLeaveDialogOpen,
            onLeaveDialogOpenChange = { isLeaveDialogOpen = it },
            onLeave = {
                isLeaveDialogOpen = false
                matchViewModel.leave()
                navigationViewModel.navigateTo(AppScreen.Lobby)
            },
            modifier = modifier,
        )
    }
}

/**
 * The lobby: nickname dialog, share dialog, games list or waiting view, Back and the lobby's
 * signals. "Share app" exists only while [shareLink] is not blank.
 */
@Composable
private fun LobbyDestination(
    lobbyViewModel: OnlineLobbyViewModel,
    shareLink: String,
    onBackToStart: () -> Unit,
    onMatchReady: (MatchTicket) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by lobbyViewModel.uiState.collectAsStateWithLifecycle()
    val games by lobbyViewModel.games.collectAsStateWithLifecycle()
    var message by rememberSaveable { mutableStateOf<LobbyMessage?>(null) }
    var isShareDialogOpen by rememberSaveable { mutableStateOf(false) }
    val isWaiting = state.waitingGameId != null

    LaunchedEffect(lobbyViewModel) { lobbyViewModel.onLobbyEntered() }

    // Keyed by the whole list so that a second, equal signal is handled after the first.
    LaunchedEffect(state.signals) {
        val signal = state.signals.firstOrNull() ?: return@LaunchedEffect
        lobbyViewModel.consumeSignal(signal)
        when (signal) {
            LobbySignal.BackToStart -> onBackToStart()
            LobbySignal.GameUnavailable -> message = LobbyMessage.GameUnavailable
            LobbySignal.CreateFailed -> message = LobbyMessage.CreateFailed
            is LobbySignal.MatchReady -> onMatchReady(signal.ticket)
        }
    }

    BackHandler {
        if (isWaiting) lobbyViewModel.onCancelWaiting() else onBackToStart()
    }

    // Without a nickname only the first-time dialog is shown, never the lobby content.
    if (state.nickname != null) {
        if (isWaiting) {
            WaitingForOpponentScreen(onCancel = lobbyViewModel::onCancelWaiting, modifier = modifier)
        } else {
            OnlineLobbyScreen(
                connection = state.connection,
                games = games,
                message = message,
                onMessageShown = { message = null },
                onRetry = lobbyViewModel::onRetryConnection,
                onCreateGame = lobbyViewModel::onCreateGame,
                onJoinGame = lobbyViewModel::onJoinGame,
                onChangeNickname = lobbyViewModel::onChangeNicknameRequested,
                onShareApp = if (shareLink.isBlank()) null else ({ isShareDialogOpen = true }),
                onBackToStart = onBackToStart,
                modifier = modifier,
            )
        }
    }
    if (isShareDialogOpen && shareLink.isNotBlank()) {
        ShareAppDialog(link = shareLink, onDismiss = { isShareDialogOpen = false })
    }
    state.nicknameDialog?.let { dialog ->
        NicknameDialog(
            nickname = state.nickname,
            isRejected = dialog.isRejected,
            onAccept = lobbyViewModel::onNicknameSubmitted,
            onDismiss = lobbyViewModel::onNicknameDialogDismissed,
        )
    }
}

/** The online match: its screen, Back and the way out; without an open match it returns to the lobby. */
@Composable
private fun MatchDestination(
    matchViewModel: OnlineMatchViewModel,
    moveSound: MoveSoundSink,
    isLeaveDialogOpen: Boolean,
    onLeaveDialogOpenChange: (Boolean) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by matchViewModel.uiState.collectAsStateWithLifecycle()
    // Only while RESUMED: what the match emits meanwhile is dropped, so nothing sounds in the
    // background or on returning.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(matchViewModel, moveSound, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            matchViewModel.acceptedMoves.collect { moveSound.play(it) }
        }
    }
    // Nothing is open when the process was recreated or the match was already left.
    if (uiState.myMark == null) {
        LaunchedEffect(Unit) { onLeave() }
        return
    }

    BackHandler {
        if (uiState.phase == MatchPhase.Playing) onLeaveDialogOpenChange(true) else onLeave()
    }

    OnlineMatchScreen(
        uiState = uiState,
        isLeaveDialogOpen = isLeaveDialogOpen,
        onLeaveDialogOpenChange = onLeaveDialogOpenChange,
        onLeaveConfirmed = onLeave,
        onCellClick = matchViewModel::play,
        onBackToLobby = onLeave,
        modifier = modifier,
    )
}
