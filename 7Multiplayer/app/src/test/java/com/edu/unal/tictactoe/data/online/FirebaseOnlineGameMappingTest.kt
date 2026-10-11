package com.edu.unal.tictactoe.data.online

import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirebaseOnlineGameMappingTest {
    private val host = mapOf("uid" to "host-uid", "name" to "Ana")
    private val guest = mapOf("uid" to "guest-uid", "name" to "Beto")

    private fun node(vararg overrides: Pair<String, Any?>): Map<String, Any?> {
        val base = mutableMapOf<String, Any?>(
            "status" to "waiting",
            "host" to host,
            "board" to "---------",
            "turn" to "X",
            "result" to "none",
            "createdAt" to 1_700_000_000_000L,
        )
        overrides.forEach { (key, value) -> if (value == null) base.remove(key) else base[key] = value }
        return base
    }

    @Test
    fun waitingGameWithoutGuest() {
        val game = gameFromValue("g1", node())

        assertEquals(
            OnlineGame(
                id = "g1",
                host = OnlinePlayer("host-uid", "Ana"),
                guest = null,
                board = List(9) { Mark.Empty },
                turn = Player.X,
                result = GameResult.InProgress,
                status = OnlineStatus.Waiting,
                createdAt = 1_700_000_000_000L,
            ),
            game,
        )
    }

    @Test
    fun playingGameWithGuest() {
        val game = gameFromValue("g2", node("status" to "playing", "guest" to guest, "board" to "X-O------", "turn" to "X"))!!

        assertEquals(OnlinePlayer("guest-uid", "Beto"), game.guest)
        assertEquals(OnlineStatus.Playing, game.status)
        assertEquals(Mark.O, game.board[2])
        assertEquals(Player.X, game.turn)
    }

    @Test
    fun finishedGameForEachResult() {
        val expected = mapOf(
            "X" to GameResult.Won(Player.X),
            "O" to GameResult.Won(Player.O),
            "tie" to GameResult.Tie,
        )
        expected.forEach { (text, result) ->
            val game = gameFromValue("g", node("status" to "finished", "guest" to guest, "result" to text))!!
            assertEquals(OnlineStatus.Finished, game.status)
            assertEquals(result, game.result)
        }
    }

    @Test
    fun roundTripFromGameToValueAndBack() {
        val games = listOf(
            OnlineGame("a", OnlinePlayer("h", "Ana"), null, List(9) { Mark.Empty }, Player.X, GameResult.InProgress, OnlineStatus.Waiting, 5L),
            OnlineGame(
                "b", OnlinePlayer("h", "Ana"), OnlinePlayer("g", "Beto"),
                listOf(Mark.X, Mark.X, Mark.X, Mark.O, Mark.O, Mark.Empty, Mark.Empty, Mark.Empty, Mark.Empty),
                Player.X, GameResult.Won(Player.X), OnlineStatus.Finished, 6L,
            ),
            OnlineGame(
                "c", OnlinePlayer("h", "Ana"), OnlinePlayer("g", "Beto"),
                listOf(Mark.X, Mark.O, Mark.X, Mark.X, Mark.O, Mark.O, Mark.O, Mark.X, Mark.X),
                Player.O, GameResult.Tie, OnlineStatus.Finished, 7L,
            ),
        )
        games.forEach { assertEquals(it, gameFromValue(it.id, gameToValue(it))) }
    }

    @Test
    fun gameToValueUsesTheWireNamesAndOmitsAbsentGuest() {
        val value = gameToValue(
            OnlineGame("a", OnlinePlayer("h", "Ana"), null, List(9) { Mark.Empty }, Player.X, GameResult.InProgress, OnlineStatus.Waiting, 5L),
        )

        assertEquals(
            mapOf(
                "status" to "waiting",
                "host" to mapOf("uid" to "h", "name" to "Ana"),
                "board" to "---------",
                "turn" to "X",
                "result" to "none",
                "createdAt" to 5L,
            ),
            value,
        )
    }

    @Test
    fun newGameValueIsAWaitingGameWithTheGivenTimestamp() {
        val marker = Any()

        assertEquals(
            mapOf(
                "status" to "waiting",
                "host" to mapOf("uid" to "h", "name" to "Ana"),
                "board" to "---------",
                "turn" to "X",
                "result" to "none",
                "createdAt" to marker,
            ),
            newGameValue(OnlinePlayer("h", "Ana"), marker),
        )
    }

    @Test
    fun invalidBoardMakesTheNodeInvalid() {
        assertNull(gameFromValue("g", node("board" to "--------")))
        assertNull(gameFromValue("g", node("board" to "---------X")))
        assertNull(gameFromValue("g", node("board" to "---a-----")))
        assertNull(gameFromValue("g", node("board" to 123)))
    }

    @Test
    fun missingFieldsMakeTheNodeInvalid() {
        listOf("status", "host", "board", "turn", "result", "createdAt").forEach { field ->
            assertNull(field, gameFromValue("g", node(field to null)))
        }
    }

    @Test
    fun wrongTypesOrValuesMakeTheNodeInvalid() {
        assertNull(gameFromValue("g", null))
        assertNull(gameFromValue("g", "text"))
        assertNull(gameFromValue("g", node("status" to "abandoned")))
        assertNull(gameFromValue("g", node("turn" to "Z")))
        assertNull(gameFromValue("g", node("turn" to 1L)))
        assertNull(gameFromValue("g", node("result" to "draw")))
        assertNull(gameFromValue("g", node("host" to "Ana")))
        assertNull(gameFromValue("g", node("host" to mapOf("uid" to "h"))))
        assertNull(gameFromValue("g", node("host" to mapOf("uid" to 1L, "name" to "Ana"))))
        assertNull(gameFromValue("g", node("guest" to mapOf("uid" to "g"))))
        assertNull(gameFromValue("g", node("createdAt" to "now")))
        assertNull(gameFromValue("g", node("createdAt" to 1.5)))
    }

    @Test
    fun createdAtAcceptsLongAndWholeNumbers() {
        assertEquals(1_700_000_000_000L, gameFromValue("g", node("createdAt" to 1_700_000_000_000L))!!.createdAt)
        assertEquals(42L, gameFromValue("g", node("createdAt" to 42))!!.createdAt)
        assertEquals(1_700_000_000_000L, gameFromValue("g", node("createdAt" to 1.7e12))!!.createdAt)
    }
}
