package com.edu.unal.tictactoe.ui.online.lobby

import com.edu.unal.tictactoe.game.models.Player

/** Where the lobby stands with the service; only [Connected] lets the person act on games. */
enum class LobbyConnection {
    /** The anonymous identity or the first connection is still pending. */
    Connecting,

    /** The identity could not be established; the person can retry. */
    IdentityFailed,

    /** The identity exists but the service is not reachable. */
    Offline,

    Connected,
}

/**
 * The nickname dialog being shown. [isFirstTime] is true when no nickname is stored yet, so
 * dismissing it leaves the lobby; [isRejected] marks the last submitted text as invalid.
 */
data class NicknameDialogState(
    val isFirstTime: Boolean,
    val isRejected: Boolean = false,
)

/** An available game as the lobby lists it: [hostNickname] is the nickname of its creator. */
data class LobbyGame(val id: String, val hostNickname: String)

/** Pass to the online match: the game to open and the mark [myMark] this device plays. */
data class MatchTicket(val gameId: String, val myMark: Player)

/**
 * One-shot instruction for the layer above the lobby. A signal stays in
 * [OnlineLobbyUiState.signals] until its receiver calls [OnlineLobbyViewModel.consumeSignal].
 */
sealed interface LobbySignal {
    /** The first-time nickname dialog was dismissed: return to the start screen. */
    data object BackToStart : LobbySignal

    /** A game now has both players: open the match described by [ticket]. */
    data class MatchReady(val ticket: MatchTicket) : LobbySignal

    /** The chosen game was taken or removed, or the own waiting game vanished on its own. */
    data object GameUnavailable : LobbySignal

    /** The service did not complete the creation of the own game. */
    data object CreateFailed : LobbySignal
}

/**
 * Immutable state of the online lobby. [nickname] is null until one is stored; while it is null
 * the lobby content (games, actions) must not be shown, only the first-time [nicknameDialog].
 *
 * @property nicknameDialog the dialog to show, or null when none is open.
 * @property waitingGameId id of the own game waiting for a rival, or null; while set, the screen
 * shows the waiting view (never a board).
 * @property signals pending one-shot signals, oldest first.
 */
data class OnlineLobbyUiState(
    val nickname: String? = null,
    val nicknameDialog: NicknameDialogState? = null,
    val connection: LobbyConnection = LobbyConnection.Connecting,
    val waitingGameId: String? = null,
    val signals: List<LobbySignal> = emptyList(),
) {
    /** Creating and joining games requires a nickname and a live connection. */
    val actionsEnabled: Boolean
        get() = nickname != null && connection == LobbyConnection.Connected
}
