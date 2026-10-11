package com.edu.unal.tictactoe.data.online

import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player

/** A participant of an online game, identified by the service uid. */
data class OnlinePlayer(val uid: String, val name: String)

enum class OnlineStatus {
    Waiting,
    Playing,
    Finished,
}

/**
 * Snapshot of an online game. The host plays X and the guest plays O; [board] always has nine
 * cells and [guest] is null while the game is [OnlineStatus.Waiting].
 */
data class OnlineGame(
    val id: String,
    val host: OnlinePlayer,
    val guest: OnlinePlayer?,
    val board: List<Mark>,
    val turn: Player,
    val result: GameResult,
    val status: OnlineStatus,
    val createdAt: Long,
)
