package com.edu.unal.tictactoe.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edu.unal.tictactoe.R
import com.edu.unal.tictactoe.ui.game.DifficultyLabels
import com.edu.unal.tictactoe.ui.game.GameScreen
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.online.lobby.LobbyMessage
import com.edu.unal.tictactoe.ui.online.lobby.LobbySignal
import com.edu.unal.tictactoe.ui.online.lobby.MatchTicket
import com.edu.unal.tictactoe.ui.online.lobby.NicknameDialog
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyScreen
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyViewModel
import com.edu.unal.tictactoe.ui.online.lobby.WaitingForOpponentScreen
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
 * a game that became [LobbySignal.MatchReady] stores its ticket and opens [AppScreen.Match].
 *
 * Every screen's state holder is a parameter, so a test can mount this with fakes and without the
 * real Activity.
 */
@Composable
fun AppNavigation(
    navigationViewModel: NavigationViewModel,
    gameViewModel: GameViewModel,
    lobbyViewModel: OnlineLobbyViewModel,
    modifier: Modifier = Modifier,
) {
    val currentScreen by navigationViewModel.currentScreen.collectAsStateWithLifecycle()
    // Survives rotation. Provisional: the match screen (task 5.9) reads it to open the match.
    var matchTicket by rememberSaveable(stateSaver = MatchTicketSaver) { mutableStateOf<MatchTicket?>(null) }

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
                modifier = modifier,
            )
        }

        AppScreen.Lobby -> LobbyDestination(
            lobbyViewModel = lobbyViewModel,
            onBackToStart = { navigationViewModel.navigateTo(AppScreen.Start) },
            onMatchReady = { ticket ->
                matchTicket = ticket
                navigationViewModel.navigateTo(AppScreen.Match)
            },
            modifier = modifier,
        )

        AppScreen.Match -> ProvisionalOnlineScreen(modifier)
    }
}

/** The lobby: nickname dialog, games list or waiting view, Back and the lobby's signals. */
@Composable
private fun LobbyDestination(
    lobbyViewModel: OnlineLobbyViewModel,
    onBackToStart: () -> Unit,
    onMatchReady: (MatchTicket) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by lobbyViewModel.uiState.collectAsStateWithLifecycle()
    val games by lobbyViewModel.games.collectAsStateWithLifecycle()
    var message by rememberSaveable { mutableStateOf<LobbyMessage?>(null) }
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
                modifier = modifier,
            )
        }
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

private val MatchTicketSaver = Saver<MatchTicket?, List<String>>(
    save = { ticket -> ticket?.let { listOf(it.gameId, it.myMark.name) } },
    restore = { saved -> MatchTicket(saved[0], Player.valueOf(saved[1])) },
)

/** Provisional stand-in for the online screens; the lobby and match tasks replace and remove it. */
@Composable
private fun ProvisionalOnlineScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.online_provisional_placeholder))
    }
}
