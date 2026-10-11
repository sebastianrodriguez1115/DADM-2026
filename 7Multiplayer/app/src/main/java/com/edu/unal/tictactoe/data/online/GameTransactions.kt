package com.edu.unal.tictactoe.data.online

import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.MoveResult
import com.edu.unal.tictactoe.game.models.Player

/** Outcome of a pure decision made over the current value of a game node. */
sealed interface TransactionDecision {
    data object Abort : TransactionDecision

    /** Commit without changing the node (used when the current value is null). */
    data object Unchanged : TransactionDecision

    data class Write(val game: OnlineGame) : TransactionDecision
}

/** Pure decisions behind the join and move transactions; no I/O and no shared state. */
object GameTransactions {
    fun join(current: OnlineGame?, guest: OnlinePlayer): TransactionDecision {
        if (current == null) {
            return TransactionDecision.Unchanged
        }
        if (current.status != OnlineStatus.Waiting || current.guest != null) {
            return TransactionDecision.Abort
        }
        return TransactionDecision.Write(
            current.copy(guest = guest, status = OnlineStatus.Playing),
        )
    }

    fun play(current: OnlineGame?, uid: String, location: Int): TransactionDecision {
        if (current == null) {
            return TransactionDecision.Unchanged
        }
        val player = when (uid) {
            current.host.uid -> Player.X
            current.guest?.uid -> Player.O
            else -> return TransactionDecision.Abort
        }
        if (current.status != OnlineStatus.Playing || current.turn != player) {
            return TransactionDecision.Abort
        }
        val game = TicTacToeGame()
        if (!game.restoreBoard(current.board) || game.setMove(player, location) != MoveResult.Applied) {
            return TransactionDecision.Abort
        }
        val result = game.checkForWinner()
        val finished = result != GameResult.InProgress
        return TransactionDecision.Write(
            current.copy(
                board = game.board,
                result = result,
                turn = if (finished) current.turn else other(player),
                status = if (finished) OnlineStatus.Finished else current.status,
            ),
        )
    }

    private fun other(player: Player): Player = when (player) {
        Player.X -> Player.O
        Player.O -> Player.X
    }
}
