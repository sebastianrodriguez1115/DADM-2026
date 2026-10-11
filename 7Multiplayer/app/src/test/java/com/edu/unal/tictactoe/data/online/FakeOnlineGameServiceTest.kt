package com.edu.unal.tictactoe.data.online

import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeOnlineGameServiceTest {
    private val backend = FakeOnlineBackend()
    private val ana = backend.serviceFor("ana")
    private val luis = backend.serviceFor("luis")

    private fun <T> TestScope.collect(flow: Flow<T>): List<T> {
        val values = mutableListOf<T>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect { values += it } }
        return values
    }

    private suspend fun startedGame(): String {
        val id = ana.createGame("Ana")
        luis.joinGame(id, "Luis")
        return id
    }

    @Test
    fun createdGameIsWaitingAndListedForAnotherUid() = runTest {
        val available = collect(luis.observeAvailableGames())
        assertEquals(listOf(emptyList<OnlineGame>()), available)

        val id = ana.createGame("Ana")

        val game = backend.gameById(id)!!
        assertEquals(OnlinePlayer("ana", "Ana"), game.host)
        assertNull(game.guest)
        assertEquals(List(9) { Mark.Empty }, game.board)
        assertEquals(Player.X, game.turn)
        assertEquals(GameResult.InProgress, game.result)
        assertEquals(OnlineStatus.Waiting, game.status)
        assertEquals(listOf(emptyList(), listOf(game)), available)
    }

    @Test
    fun idsAndTimestampsAreDeterministic() = runTest {
        val first = ana.createGame("Ana")
        val second = luis.createGame("Luis")

        assertEquals("game-1", first)
        assertEquals("game-2", second)
        assertEquals(1L, backend.gameById(first)!!.createdAt)
        assertEquals(2L, backend.gameById(second)!!.createdAt)
    }

    @Test
    fun secondJoinIsNoLongerAvailable() = runTest {
        val id = ana.createGame("Ana")

        assertEquals(JoinResult.Joined, luis.joinGame(id, "Luis"))
        assertEquals(JoinResult.NoLongerAvailable, backend.serviceFor("eva").joinGame(id, "Eva"))
        assertEquals("luis", backend.gameById(id)!!.guest!!.uid)
    }

    @Test
    fun joiningMissingGameIsNoLongerAvailable() = runTest {
        assertEquals(JoinResult.NoLongerAvailable, luis.joinGame("missing", "Luis"))
        assertNull(backend.gameById("missing"))
    }

    @Test
    fun hostSeesPlayingWhenRivalJoinsAndGameLeavesTheList() = runTest {
        val id = ana.createGame("Ana")
        val hostView = collect(ana.observeGame(id))
        val available = collect(luis.observeAvailableGames())

        luis.joinGame(id, "Luis")

        assertEquals(OnlineStatus.Waiting, hostView.first()!!.status)
        val playing = hostView.last()!!
        assertEquals(OnlineStatus.Playing, playing.status)
        assertEquals(OnlinePlayer("luis", "Luis"), playing.guest)
        assertTrue(available.last().isEmpty())
    }

    @Test
    fun cancelRemovesFromListAndObserverGetsNull() = runTest {
        val id = ana.createGame("Ana")
        val available = collect(luis.observeAvailableGames())
        val view = collect(luis.observeGame(id))

        ana.cancelGame(id)

        assertTrue(available.last().isEmpty())
        assertNull(view.last())
        assertNull(backend.gameById(id))
    }

    @Test
    fun cancelOrLeaveByNonParticipantDoesNotDeleteAndNeverThrows() = runTest {
        val id = ana.createGame("Ana")

        luis.cancelGame(id)
        luis.leaveGame(id)
        luis.cancelGame("missing")
        luis.leaveGame("missing")

        assertEquals(OnlineStatus.Waiting, backend.gameById(id)!!.status)
    }

    @Test
    fun leavingEmitsNullToTheRival() = runTest {
        val id = startedGame()
        val hostView = collect(ana.observeGame(id))

        luis.leaveGame(id)

        assertEquals(OnlineStatus.Playing, hostView.first()!!.status)
        assertNull(hostView.last())
    }

    @Test
    fun observingMissingGameEmitsNull() = runTest {
        assertEquals(listOf(null), collect(ana.observeGame("missing")))
    }

    @Test
    fun validMoveIsAcceptedAndVisibleToBoth() = runTest {
        val id = startedGame()
        val hostView = collect(ana.observeGame(id))
        val guestView = collect(luis.observeGame(id))

        assertEquals(MoveOutcome.Accepted, ana.playMove(id, 4))

        for (view in listOf(hostView, guestView)) {
            val game = view.last()!!
            assertEquals(Mark.X, game.board[4])
            assertEquals(Player.O, game.turn)
        }
    }

    @Test
    fun moveOutOfTurnIsRejectedAndLeavesTheGame() = runTest {
        val id = startedGame()
        val before = backend.gameById(id)

        assertEquals(MoveOutcome.Rejected, luis.playMove(id, 0))

        assertEquals(before, backend.gameById(id))
    }

    @Test
    fun winningMoveFinishesTheGame() = runTest {
        val id = startedGame()
        ana.playMove(id, 0)
        luis.playMove(id, 3)
        ana.playMove(id, 1)
        luis.playMove(id, 4)

        assertEquals(MoveOutcome.Accepted, ana.playMove(id, 2))

        val game = backend.gameById(id)!!
        assertEquals(OnlineStatus.Finished, game.status)
        assertEquals(GameResult.Won(Player.X), game.result)
    }

    @Test
    fun signInFailsThenWorks() = runTest {
        ana.signInFails = true
        try {
            ana.ensureSignedIn()
            fail("expected OnlineServiceException")
        } catch (expected: OnlineServiceException) {
        }

        ana.signInFails = false

        assertEquals("ana", ana.ensureSignedIn())
    }

    @Test
    fun heldSignInCompletesOnlyAfterRelease() = runTest {
        ana.holdSignIn()
        var uid: String? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { uid = ana.ensureSignedIn() }

        assertNull(uid)
        ana.releaseSignIn()
        runCurrent()

        assertEquals("ana", uid)
        assertEquals("ana", ana.ensureSignedIn())
    }

    @Test
    fun connectionCanBeCutAndRestoredPerUid() = runTest {
        val anaConnection = collect(ana.observeConnection())
        val luisConnection = collect(luis.observeConnection())

        backend.disconnect("ana")
        backend.reconnect("ana")

        assertEquals(listOf(true, false, true), anaConnection)
        assertEquals(listOf(true), luisConnection)
    }

    @Test
    fun connectionLossDeletesOnlyTheGamesOfThatUid() = runTest {
        val own = ana.createGame("Ana")
        val joined = luis.createGame("Luis").also { backend.serviceFor("eva").joinGame(it, "Eva") }
        val other = luis.createGame("Luis")
        val ownView = collect(luis.observeGame(own))
        val available = collect(luis.observeAvailableGames())

        backend.loseConnection("ana")
        backend.loseConnection("eva")

        assertNull(ownView.last())
        assertNull(backend.gameById(own))
        assertNull(backend.gameById(joined))
        assertEquals(listOf(other), available.last().map { it.id })
    }

    @Test
    fun externalDeleteEmitsNullAndRemovesFromList() = runTest {
        val id = ana.createGame("Ana")
        val view = collect(ana.observeGame(id))
        val available = collect(ana.observeAvailableGames())

        backend.deleteGame(id)

        assertNull(view.last())
        assertTrue(available.last().isEmpty())
    }

    @Test
    fun failedCreateLeavesNoGameAndNextCreateWorks() = runTest {
        val available = collect(luis.observeAvailableGames())
        ana.failNextCreate()

        try {
            ana.createGame("Ana")
            fail("expected OnlineServiceException")
        } catch (expected: OnlineServiceException) {
        }

        assertTrue(available.last().isEmpty())
        assertEquals(0, backend.createCount)
        assertEquals("game-1", ana.createGame("Ana"))
        assertEquals(1, available.last().size)
    }

    @Test
    fun rejectedMoveLeavesGameAndOnlyAffectsTheNextMove() = runTest {
        val id = startedGame()
        val before = backend.gameById(id)
        ana.rejectNextMove()

        assertEquals(MoveOutcome.Rejected, ana.playMove(id, 4))
        assertEquals(before, backend.gameById(id))
        assertEquals(MoveOutcome.Accepted, ana.playMove(id, 4))
    }

    @Test
    fun failedMoveThrowsLeavesGameAndOnlyAffectsTheNextMove() = runTest {
        val id = startedGame()
        val before = backend.gameById(id)
        ana.failNextMove()

        try {
            ana.playMove(id, 4)
            fail("expected OnlineServiceException")
        } catch (expected: OnlineServiceException) {
        }

        assertEquals(before, backend.gameById(id))
        assertEquals(MoveOutcome.Accepted, ana.playMove(id, 4))
    }

    @Test
    fun heldMoveAppliesOnlyAfterRelease() = runTest {
        val id = startedGame()
        val before = backend.gameById(id)
        ana.holdNextMove()
        var outcome: MoveOutcome? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { outcome = ana.playMove(id, 4) }

        assertNull(outcome)
        assertEquals(before, backend.gameById(id))
        ana.releaseHeldMove()
        runCurrent()

        assertEquals(MoveOutcome.Accepted, outcome)
        assertEquals(Mark.X, backend.gameById(id)!!.board[4])
        assertEquals(MoveOutcome.Accepted, luis.playMove(id, 0))
    }

    @Test
    fun countersTrackCreationsAndCancellations() = runTest {
        val first = ana.createGame("Ana")
        luis.createGame("Luis")
        ana.cancelGame(first)
        ana.cancelGame(first)

        assertEquals(2, backend.createCount)
        assertEquals(2, backend.cancelCount)
        assertFalse(backend.gameById(first) != null)
    }

    @Test
    fun serviceForReturnsTheSameInstancePerUid() {
        assertTrue(backend.serviceFor("ana") === ana)
        assertEquals("luis", luis.uid)
    }
}
