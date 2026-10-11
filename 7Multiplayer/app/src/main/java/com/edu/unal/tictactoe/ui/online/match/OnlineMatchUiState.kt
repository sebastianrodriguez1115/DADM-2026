package com.edu.unal.tictactoe.ui.online.match

import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player

/**
 * Where the observed game stands; [Loading] lasts until the first game value arrives. [Abandoned]
 * means the game vanished before it finished: there is no winner and no result.
 */
enum class MatchPhase {
    Loading,
    Playing,
    Finished,
    Abandoned,
}

/** Outcome of a finished game from the point of view of the person holding the device. */
enum class MatchResult {
    Won,
    Lost,
    Tie,
}

/**
 * Everything the online match screen shows. [myMark] is null only before [OnlineMatchViewModel.openMatch].
 * [opponentName] is the text for "Rival: <name>", "Turno de <name>" and "Ganó <name>". [result] is
 * null until the game is [MatchPhase.Finished]. [isMovePending] is true from the tap until the
 * confirmed mark reaches [board].
 */
data class OnlineMatchUiState(
    val myMark: Player? = null,
    val opponentName: String? = null,
    val board: List<Mark> = List(BOARD_SIZE) { Mark.Empty },
    val phase: MatchPhase = MatchPhase.Loading,
    val isMyTurn: Boolean = false,
    val result: MatchResult? = null,
    val isConnected: Boolean = true,
    val isMovePending: Boolean = false,
) {
    /** The cells accept input only while playing, on my turn, connected and with no move pending. */
    val isInputEnabled: Boolean
        get() = phase == MatchPhase.Playing && isMyTurn && isConnected && !isMovePending

    /** The "Sin conexión" notice shows only while a game in progress has no connection. */
    val isOfflineNoticeVisible: Boolean
        get() = phase == MatchPhase.Playing && !isConnected

    private companion object {
        const val BOARD_SIZE = 9
    }
}
