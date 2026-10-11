package com.edu.unal.tictactoe.data.online

import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTransactionsTest {
    private val host = OnlinePlayer("host-uid", "Ana")
    private val guest = OnlinePlayer("guest-uid", "Beto")

    private fun board(text: String): List<Mark> = BoardCodec.decode(text)!!

    private fun game(
        status: OnlineStatus = OnlineStatus.Playing,
        guest: OnlinePlayer? = this.guest,
        board: String = "---------",
        turn: Player = Player.X,
    ) = OnlineGame(
        id = "g1",
        host = host,
        guest = guest,
        board = board(board),
        turn = turn,
        result = GameResult.InProgress,
        status = status,
        createdAt = 1L,
    )

    private fun written(decision: TransactionDecision): OnlineGame {
        assertTrue(decision is TransactionDecision.Write)
        return (decision as TransactionDecision.Write).game
    }

    @Test
    fun joinWithNullIsUnchanged() {
        assertEquals(TransactionDecision.Unchanged, GameTransactions.join(null, guest))
    }

    @Test
    fun joinWaitingGameWithoutGuestWritesGuestAndPlaying() {
        val current = game(status = OnlineStatus.Waiting, guest = null)

        val result = written(GameTransactions.join(current, guest))

        assertEquals(current.copy(guest = guest, status = OnlineStatus.Playing), result)
    }

    @Test
    fun joinWaitingGameThatAlreadyHasGuestAborts() {
        val current = game(status = OnlineStatus.Waiting, guest = OnlinePlayer("other", "Eve"))

        assertEquals(TransactionDecision.Abort, GameTransactions.join(current, guest))
    }

    @Test
    fun joinPlayingOrFinishedGameAborts() {
        assertEquals(
            TransactionDecision.Abort,
            GameTransactions.join(game(status = OnlineStatus.Playing, guest = null), guest),
        )
        assertEquals(
            TransactionDecision.Abort,
            GameTransactions.join(game(status = OnlineStatus.Finished, guest = null), guest),
        )
    }

    @Test
    fun playWithNullIsUnchanged() {
        assertEquals(TransactionDecision.Unchanged, GameTransactions.play(null, host.uid, 0))
    }

    @Test
    fun playByNonParticipantAborts() {
        assertEquals(TransactionDecision.Abort, GameTransactions.play(game(), "stranger", 0))
    }

    @Test
    fun playWithoutGuestByUnknownUidAborts() {
        val current = game(status = OnlineStatus.Waiting, guest = null)

        assertEquals(TransactionDecision.Abort, GameTransactions.play(current, "stranger", 0))
    }

    @Test
    fun playWhenNotPlayingAborts() {
        assertEquals(
            TransactionDecision.Abort,
            GameTransactions.play(game(status = OnlineStatus.Waiting), host.uid, 0),
        )
        assertEquals(
            TransactionDecision.Abort,
            GameTransactions.play(game(status = OnlineStatus.Finished), host.uid, 0),
        )
    }

    @Test
    fun playOutOfTurnAborts() {
        assertEquals(TransactionDecision.Abort, GameTransactions.play(game(turn = Player.X), guest.uid, 0))
        assertEquals(TransactionDecision.Abort, GameTransactions.play(game(turn = Player.O), host.uid, 0))
    }

    @Test
    fun playOnOccupiedCellAborts() {
        val current = game(board = "X--------", turn = Player.O)

        assertEquals(TransactionDecision.Abort, GameTransactions.play(current, guest.uid, 0))
    }

    @Test
    fun playOnOutOfRangeCellAborts() {
        assertEquals(TransactionDecision.Abort, GameTransactions.play(game(), host.uid, 9))
        assertEquals(TransactionDecision.Abort, GameTransactions.play(game(), host.uid, -1))
    }

    @Test
    fun playOnIllegalBoardAborts() {
        val current = game(board = "XXX-----O", turn = Player.X)

        assertEquals(TransactionDecision.Abort, GameTransactions.play(current, host.uid, 4))
    }

    @Test
    fun intermediateMoveWritesBoardAndPassesTurn() {
        val current = game(board = "X--------", turn = Player.O)

        val result = written(GameTransactions.play(current, guest.uid, 4))

        assertEquals(board("X---O----"), result.board)
        assertEquals(Player.X, result.turn)
        assertEquals(GameResult.InProgress, result.result)
        assertEquals(OnlineStatus.Playing, result.status)
        assertEquals(current.host, result.host)
        assertEquals(current.guest, result.guest)
    }

    @Test
    fun hostMovesAsXAndPassesTurnToO() {
        val result = written(GameTransactions.play(game(), host.uid, 4))

        assertEquals(board("----X----"), result.board)
        assertEquals(Player.O, result.turn)
    }

    @Test
    fun winningMoveFinishesGameAndKeepsTurn() {
        val current = game(board = "XX-OO----", turn = Player.X)

        val result = written(GameTransactions.play(current, host.uid, 2))

        assertEquals(board("XXXOO----"), result.board)
        assertEquals(GameResult.Won(Player.X), result.result)
        assertEquals(OnlineStatus.Finished, result.status)
        assertEquals(Player.X, result.turn)
    }

    @Test
    fun guestWinningMoveFinishesGameAsO() {
        val current = game(board = "XX-OO-X--", turn = Player.O)

        val result = written(GameTransactions.play(current, guest.uid, 5))

        assertEquals(GameResult.Won(Player.O), result.result)
        assertEquals(OnlineStatus.Finished, result.status)
        assertEquals(Player.O, result.turn)
    }

    @Test
    fun lastMoveWithoutWinnerIsATie() {
        val current = game(board = "XOXXOOOX-", turn = Player.X)

        val result = written(GameTransactions.play(current, host.uid, 8))

        assertEquals(board("XOXXOOOXX"), result.board)
        assertEquals(GameResult.Tie, result.result)
        assertEquals(OnlineStatus.Finished, result.status)
        assertEquals(Player.X, result.turn)
    }
}
