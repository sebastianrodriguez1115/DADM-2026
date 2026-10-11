package com.edu.unal.tictactoe

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.data.online.FirebaseOnlineGameService
import com.edu.unal.tictactoe.data.online.JoinResult
import com.edu.unal.tictactoe.data.online.MoveOutcome
import com.edu.unal.tictactoe.data.online.OnlineGame
import com.edu.unal.tictactoe.data.online.OnlineGameService
import com.edu.unal.tictactoe.data.online.OnlineStatus
import com.edu.unal.tictactoe.data.online.awaitTask
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test of [FirebaseOnlineGameService] against the real Firebase project configured by
 * `app/google-services.json`, with several independent anonymous identities in one process: the
 * default `FirebaseApp` and secondary apps built with the same options and another name, each with
 * its own `FirebaseAuth` and `FirebaseDatabase`.
 *
 * To pass it needs: network access from the device, the anonymous sign-in provider enabled in the
 * Firebase console, and the rules of `firebase/database.rules.json` published as they are in the
 * repository. Every test deletes the games it creates, also when it fails, and checks that none
 * hosted by its identities remains.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseOnlineGameServiceLiveTest {
    private class Identity(val app: FirebaseApp) {
        val auth: FirebaseAuth = FirebaseAuth.getInstance(app)
        val database: FirebaseDatabase = FirebaseDatabase.getInstance(app)
        val service: OnlineGameService = FirebaseOnlineGameService(auth, database)
        val uid: String by lazy { runBlocking { service.ensureSignedIn() } }
    }

    private val secondaryApps = mutableListOf<FirebaseApp>()
    private val created = mutableListOf<String>()
    private val identities = mutableListOf<Identity>()

    private lateinit var host: Identity
    private lateinit var guest: Identity

    @Before
    fun setUp() {
        host = Identity(FirebaseApp.getInstance())
        identities += host
        guest = secondaryIdentity("live-guest")
        // Every identity signs in up front: the service only observes games after a sign-in made by
        // someone else, so a test must never depend on a session stored by an earlier run.
        host.uid
        guest.uid
    }

    @After
    fun tearDown() {
        try {
            cleanUp()
        } finally {
            secondaryApps.forEach { app -> runCatching { app.delete() } }
        }
    }

    private fun cleanUp() {
        runBlocking {
            for (id in created) {
                for (identity in identities) {
                    withTimeoutOrNull(CLEANUP_MILLIS) { runCatching { identity.service.leaveGame(id) } }
                }
            }
        }
        created.forEach { id -> awaitGame("a created game to be deleted", host.service, id) { it == null } }
        val owners = identities.mapNotNull { it.auth.currentUser?.uid }.toSet()
        val leftovers = runBlocking {
            withTimeoutOrNull(TIMEOUT_MILLIS) { host.database.getReference("games").get().awaitTask() }
        }
        assertNotNull("Could not read the games to check for leftovers", leftovers)
        val remaining = leftovers!!.children.count { it.child("host/uid").value in owners }
        assertEquals("Games hosted by the test identities remain", 0, remaining)
    }

    private fun secondaryIdentity(name: String): Identity {
        val app = FirebaseApp.initializeApp(
            InstrumentationRegistry.getInstrumentation().targetContext,
            FirebaseApp.getInstance().options,
            name,
        )
        secondaryApps += app
        return Identity(app).also {
            identities += it
            it.uid
        }
    }

    private fun newGame(owner: Identity, nickname: String): String =
        runBlocking { owner.service.createGame(nickname) }.also { created += it }

    /** Makes a result visible in the `am instrument` output as an INSTRUMENTATION_STATUS line. */
    private fun report(key: String, value: String) {
        InstrumentationRegistry.getInstrumentation().sendStatus(
            0,
            Bundle().apply { putString("live-$key", value) },
        )
    }

    private class Found<T>(val value: T)

    /** The first emission of [flow] that satisfies [predicate]; the emission itself may be null. */
    private fun <T> awaitFlow(what: String, flow: Flow<T>, predicate: (T) -> Boolean): T {
        val found = runBlocking { withTimeoutOrNull(TIMEOUT_MILLIS) { Found(flow.first(predicate)) } }
            ?: throw AssertionError("Timed out waiting for $what")
        return found.value
    }

    private fun awaitGame(
        what: String,
        service: OnlineGameService,
        id: String,
        predicate: (OnlineGame?) -> Boolean,
    ): OnlineGame? = awaitFlow(what, service.observeGame(id), predicate)

    private fun joined(owner: Identity, guestIdentity: Identity, nickname: String = HOST_NAME): String {
        val id = newGame(owner, nickname)
        assertEquals(JoinResult.Joined, runBlocking { guestIdentity.service.joinGame(id, GUEST_NAME) })
        awaitGame("the game to be playing for the host", owner.service, id) { it?.status == OnlineStatus.Playing }
        return id
    }

    private fun playFullMatch(moves: List<Int>, expected: GameResult) {
        val id = joined(host, guest)
        moves.forEachIndexed { index, location ->
            val mover = if (index % 2 == 0) host else guest
            val observer = if (index % 2 == 0) guest else host
            val mark = if (index % 2 == 0) Mark.X else Mark.O
            assertEquals("Move $index was not accepted", MoveOutcome.Accepted, runBlocking { mover.service.playMove(id, location) })
            awaitGame("move $index to reach the other player", observer.service, id) { it?.board?.get(location) == mark }
        }
        val last = awaitGame("the game to finish", guest.service, id) { it?.status == OnlineStatus.Finished }
        assertEquals(expected, last?.result)
    }

    private fun writeBoardDirectly(who: Identity, id: String, board: String): Throwable? = runBlocking {
        who.uid
        runCatching {
            withTimeoutOrNull(TIMEOUT_MILLIS) {
                who.database.getReference("games/$id/board").setValue(board).awaitTask()
                Unit
            } ?: throw AssertionError("Timed out waiting for the server to answer a direct write")
        }.exceptionOrNull()
    }

    private fun newNodeId(): String = runBlocking { host.database.getReference("games").push().key }
        .also { id -> created += checkNotNull(id) }!!

    private fun gameNode(hostUid: String, hostName: String, status: String, guestUid: String? = null): Map<String, Any> {
        val node = mutableMapOf<String, Any>(
            "host" to mapOf("uid" to hostUid, "name" to hostName),
            "status" to status,
            "board" to "---------",
            "turn" to "X",
            "result" to "none",
            "createdAt" to ServerValue.TIMESTAMP,
        )
        if (guestUid != null) node["guest"] = mapOf("uid" to guestUid, "name" to GUEST_NAME)
        return node
    }

    private fun writeNodeDirectly(who: Identity, id: String, node: Map<String, Any>): Throwable? = runBlocking {
        who.uid
        runCatching {
            withTimeoutOrNull(TIMEOUT_MILLIS) {
                who.database.getReference("games/$id").setValue(node).awaitTask()
                Unit
            } ?: throw AssertionError("Timed out waiting for the server to answer a direct write")
        }.exceptionOrNull()
    }

    private fun writeAtDirectly(who: Identity, path: String, value: Any?): Throwable? = runBlocking {
        who.uid
        runCatching {
            withTimeoutOrNull(TIMEOUT_MILLIS) {
                who.database.getReference(path).setValue(value).awaitTask()
                Unit
            } ?: throw AssertionError("Timed out waiting for the server to answer a direct write")
        }.exceptionOrNull()
    }

    private fun removeNodeDirectly(who: Identity, id: String): Throwable? = runBlocking {
        who.uid
        runCatching {
            withTimeoutOrNull(TIMEOUT_MILLIS) {
                who.database.getReference("games/$id").removeValue().awaitTask()
                Unit
            } ?: throw AssertionError("Timed out waiting for the server to answer a direct delete")
        }.exceptionOrNull()
    }

    private fun assertNodeAbsent(id: String) {
        assertNull(awaitGame("the rejected node to be absent", host.service, id) { it == null })
    }

    private fun assertPermissionDenied(failure: Throwable?, what: String) {
        assertNotNull("$what was accepted by the server", failure)
        assertTrue(
            "$what failed for a reason other than permissions: ${failure!!::class.java.simpleName}: ${failure.message}",
            failure.message?.contains("Permission denied", ignoreCase = true) == true,
        )
    }

    @Test
    fun identitiesAreDistinctAnonymousUsers() {
        assertTrue(host.uid.isNotBlank())
        assertTrue(guest.uid.isNotBlank())
        assertNotEquals(host.uid, guest.uid)
    }

    @Test
    fun createdGameIsListedForTheOtherPlayerWithHostNicknameAndServerTimestamp() {
        val id = newGame(host, HOST_NAME)
        val listed = awaitFlow("the game in the guest's list", guest.service.observeAvailableGames()) { games ->
            games.any { it.id == id }
        }.single { it.id == id }
        assertEquals(HOST_NAME, listed.host.name)
        assertEquals(host.uid, listed.host.uid)
        assertEquals(OnlineStatus.Waiting, listed.status)
        assertTrue("createdAt is not a positive number", listed.createdAt > 0)
    }

    @Test
    fun cancelledGameLeavesTheListAndItsNodeIsRemoved() {
        val id = newGame(host, HOST_NAME)
        awaitFlow("the game in the guest's list", guest.service.observeAvailableGames()) { games -> games.any { it.id == id } }
        runBlocking { host.service.cancelGame(id) }
        awaitFlow("the game to leave the guest's list", guest.service.observeAvailableGames()) { games -> games.none { it.id == id } }
        assertNull(awaitGame("the cancelled game to be null", host.service, id) { it == null })
    }

    @Test
    fun guestJoinsAndBothSeeTheGameInProgress() {
        val id = newGame(host, HOST_NAME)
        awaitFlow("the game in the guest's list", guest.service.observeAvailableGames()) { games -> games.any { it.id == id } }
        assertEquals(JoinResult.Joined, runBlocking { guest.service.joinGame(id, GUEST_NAME) })
        for (watcher in listOf(host, guest)) {
            val game = awaitGame("the game in progress", watcher.service, id) { it?.status == OnlineStatus.Playing }
            assertEquals(guest.uid, game?.guest?.uid)
            assertEquals(GUEST_NAME, game?.guest?.name)
            assertEquals(host.uid, game?.host?.uid)
        }
        awaitFlow("the game to leave the list", guest.service.observeAvailableGames()) { games -> games.none { it.id == id } }
    }

    @Test
    fun guestJoinsWithoutHavingTheGameCached() {
        val id = newGame(host, HOST_NAME)
        assertEquals(JoinResult.Joined, runBlocking { guest.service.joinGame(id, GUEST_NAME) })
        val game = awaitGame("the game in progress", host.service, id) { it?.status == OnlineStatus.Playing }
        assertEquals(guest.uid, game?.guest?.uid)
    }

    @Test
    fun onlyOneOfTwoSimultaneousGuestsJoins() {
        val third = secondaryIdentity("live-third")
        val id = newGame(host, HOST_NAME)
        guest.uid
        third.uid
        val results = runBlocking {
            val first = async(Dispatchers.IO) { guest.service.joinGame(id, GUEST_NAME) }
            val second = async(Dispatchers.IO) { third.service.joinGame(id, THIRD_NAME) }
            listOf(first.await(), second.await())
        }
        assertEquals("Joined results", 1, results.count { it == JoinResult.Joined })
        assertEquals("NoLongerAvailable results", 1, results.count { it == JoinResult.NoLongerAvailable })
        val winner = if (results[0] == JoinResult.Joined) guest else third
        val game = awaitGame("the game in progress", host.service, id) { it?.status == OnlineStatus.Playing }
        assertEquals(winner.uid, game?.guest?.uid)
    }

    @Test
    fun aThirdIdentityCannotJoinAGameInProgressThroughTheService() {
        val third = secondaryIdentity("live-third")
        val id = joined(host, guest)
        val result = runBlocking { third.service.joinGame(id, THIRD_NAME) }
        assertEquals("Result of the third player joining a game in progress", JoinResult.NoLongerAvailable, result)
        val game = awaitGame("the game in progress", host.service, id) { it != null }
        assertEquals(guest.uid, game?.guest?.uid)
        assertEquals(GUEST_NAME, game?.guest?.name)
        assertEquals(host.uid, game?.host?.uid)
        assertEquals(OnlineStatus.Playing, game?.status)
        assertEquals(List(9) { Mark.Empty }, game?.board)
        assertEquals(Player.X, game?.turn)
        report("third-player", "result=NoLongerAvailable, guest kept=original, status=Playing, board=empty")
    }

    @Test
    fun theJoinRaceRepeated5TimesAlwaysHasExactlyOneWinner() {
        val guestB = secondaryIdentity("live-third")
        // Each racer must be able to join on its own, otherwise losing the race proves nothing.
        listOf("guest A" to guest, "guest B" to guestB).forEach { (name, racer) ->
            val own = newGame(host, HOST_NAME)
            assertEquals(
                "Precondition: $name could not join a game of its own through the service",
                JoinResult.Joined,
                runBlocking { racer.service.joinGame(own, GUEST_NAME) },
            )
        }
        val summary = mutableListOf<String>()
        for (round in 1..RACE_ROUNDS) {
            val label = "Race round $round of $RACE_ROUNDS"
            val id = newGame(host, HOST_NAME)
            val aFirst = round % 2 == 1
            val results = runBlocking {
                // results[0] is guest A's result and results[1] guest B's, whatever the launch order.
                val calls = if (aFirst) {
                    val a = async(Dispatchers.IO) { guest.service.joinGame(id, GUEST_NAME) }
                    val b = async(Dispatchers.IO) { guestB.service.joinGame(id, THIRD_NAME) }
                    listOf(a, b)
                } else {
                    val b = async(Dispatchers.IO) { guestB.service.joinGame(id, THIRD_NAME) }
                    val a = async(Dispatchers.IO) { guest.service.joinGame(id, GUEST_NAME) }
                    listOf(a, b)
                }
                calls.map { it.await() }
            }
            assertEquals("$label: Joined results ($results)", 1, results.count { it == JoinResult.Joined })
            assertEquals("$label: NoLongerAvailable results ($results)", 1, results.count { it == JoinResult.NoLongerAvailable })
            val winnerIsA = results[0] == JoinResult.Joined
            val winner = if (winnerIsA) guest else guestB
            val game = awaitGame("$label: the game in progress", host.service, id) { it?.status == OnlineStatus.Playing }
            assertEquals("$label: the guest of the game is not the one that got Joined", winner.uid, game?.guest?.uid)
            val first = if (aFirst) "guest A" else "guest B"
            val name = if (winnerIsA) "guest A" else "guest B"
            summary += "$round: first=$first winner=$name"
            report("race-round-$round", "first=$first, Joined=$name, other=NoLongerAvailable, game playing with the winner as guest")
        }
        report("race-summary", summary.joinToString("; "))
    }

    @Test
    fun aThirdIdentityObservingTheListSeesTheGameAppearAndDisappearWhenTheGuestJoins() {
        val third = secondaryIdentity("live-third")
        val id = newGame(host, HOST_NAME)
        runBlocking {
            // One subscription for the whole test: the list must change by itself, with no reload.
            val snapshots = Channel<List<OnlineGame>>(Channel.UNLIMITED)
            val collector = launch(Dispatchers.IO) {
                third.service.observeAvailableGames().collect { snapshots.send(it) }
            }
            try {
                suspend fun awaitSnapshot(what: String, predicate: (List<OnlineGame>) -> Boolean): List<OnlineGame> =
                    withTimeoutOrNull(TIMEOUT_MILLIS) {
                        var snapshot = snapshots.receive()
                        while (!predicate(snapshot)) snapshot = snapshots.receive()
                        snapshot
                    } ?: throw AssertionError("Timed out waiting for $what")

                val waiting = awaitSnapshot("the waiting game in the third identity's list") { games ->
                    games.any { it.id == id }
                }.single { it.id == id }
                assertEquals(OnlineStatus.Waiting, waiting.status)
                assertEquals(host.uid, waiting.host.uid)

                assertEquals(JoinResult.Joined, guest.service.joinGame(id, GUEST_NAME))
                awaitSnapshot("the joined game to leave the third identity's list") { games ->
                    games.none { it.id == id }
                }
            } finally {
                collector.cancel()
            }
        }
        report("third-observer", "saw the waiting game, then saw it leave on the guest joining, same subscription")
    }

    @Test
    fun joiningAGameThatDoesNotExistIsNoLongerAvailable() {
        val missing = runBlocking { host.database.getReference("games").push().key }
        assertNotNull(missing)
        assertEquals(JoinResult.NoLongerAvailable, runBlocking { guest.service.joinGame(missing!!, GUEST_NAME) })
    }

    @Test
    fun fullMatchEndsWithAWinner() {
        playFullMatch(HOST_WINS_MOVES, GameResult.Won(Player.X))
    }

    @Test
    fun fullMatchEndsInATie() {
        playFullMatch(TIE_MOVES, GameResult.Tie)
    }

    @Test
    fun moveOutOfTurnIsRejectedAndTheBoardDoesNotChange() {
        val id = joined(host, guest)
        assertEquals(MoveOutcome.Rejected, runBlocking { guest.service.playMove(id, 4) })
        val game = awaitGame("the game in progress", host.service, id) { it != null }
        assertEquals(List(9) { Mark.Empty }, game?.board)
        assertEquals(Player.X, game?.turn)
    }

    @Test
    fun rulesRejectADirectBoardWriteFromThePlayerWithoutTheTurn() {
        val id = joined(host, guest)
        assertPermissionDenied(writeBoardDirectly(guest, id, "O--------"), "A direct board write by the player without the turn")
        val game = awaitGame("the game in progress", host.service, id) { it != null }
        assertEquals(List(9) { Mark.Empty }, game?.board)
    }

    @Test
    fun rulesLetAThirdIdentityReadButNotWriteOrDeleteAGameInProgress() {
        val third = secondaryIdentity("live-third")
        val id = joined(host, guest)
        val seen = awaitGame("the third identity to read the game", third.service, id) { it != null }
        assertEquals(OnlineStatus.Playing, seen?.status)

        assertPermissionDenied(writeBoardDirectly(third, id, "X--------"), "A board write by a third identity")
        val removal = removeNodeDirectly(third, id)
        assertPermissionDenied(removal, "A delete by a third identity")

        val after = awaitGame("the game in progress", host.service, id) { it != null }
        assertEquals(List(9) { Mark.Empty }, after?.board)
        assertEquals(OnlineStatus.Playing, after?.status)
    }

    @Test
    fun rulesRejectReadingGamesWithoutASession() {
        val anonymous = secondaryIdentity("live-anonymous")
        anonymous.auth.signOut()
        assertNull(anonymous.auth.currentUser)
        val failure = runBlocking {
            runCatching {
                withTimeoutOrNull(TIMEOUT_MILLIS) { anonymous.database.getReference("games").get().awaitTask() }
                    ?: throw AssertionError("Timed out waiting for the server to answer an unauthenticated read")
            }.exceptionOrNull()
        }
        assertPermissionDenied(failure, "An unauthenticated read of the games")
        assertNull(anonymous.auth.currentUser)
    }

    @Test
    fun theOtherPlayerSeesNullWhenAPlayerLeavesAMatchInProgress() {
        val id = joined(host, guest)
        runBlocking { guest.service.leaveGame(id) }
        assertNull(awaitGame("the game to disappear for the host", host.service, id) { it == null })
    }

    @Test
    fun rulesRejectCreatingAGameDirectlyInPlayingWithoutAGuest() {
        val id = newNodeId()
        val failure = writeNodeDirectly(host, id, gameNode(host.uid, HOST_NAME, "playing"))
        assertPermissionDenied(failure, "Creating a game already playing without a guest")
        assertNodeAbsent(id)
    }

    @Test
    fun rulesRejectCreatingAWaitingGameThatAlreadyHasAGuest() {
        val id = newNodeId()
        val failure = writeNodeDirectly(host, id, gameNode(host.uid, HOST_NAME, "waiting", guest.uid))
        assertPermissionDenied(failure, "Creating a waiting game that already has a guest")
        assertNodeAbsent(id)
    }

    @Test
    fun rulesAcceptDeletingAGameNodeThatDoesNotExist() {
        val id = newNodeId()
        assertNodeAbsent(id)
        assertNull("Deleting a missing game node was rejected", removeNodeDirectly(host, id))
        assertNodeAbsent(id)
    }

    @Test
    fun rulesRejectAThirdIdentityWritingItselfAsGuestOfAGameInProgress() {
        val third = secondaryIdentity("live-third")
        val id = joined(host, guest)
        val failure = writeAtDirectly(third, "games/$id/guest", mapOf("uid" to third.uid, "name" to THIRD_NAME))
        assertPermissionDenied(failure, "A third identity writing itself as the guest of a game in progress")
        val game = awaitGame("the game in progress", host.service, id) { it != null }
        assertEquals(guest.uid, game?.guest?.uid)
        assertEquals(GUEST_NAME, game?.guest?.name)
        assertEquals(OnlineStatus.Playing, game?.status)
    }

    @Test
    fun rulesRejectANicknameOf21Characters() {
        val tooLong = newNodeId()
        val rejection = writeNodeDirectly(host, tooLong, gameNode(host.uid, "N".repeat(21), "waiting"))
        assertPermissionDenied(rejection, "Creating a game with a 21-character nickname")
        assertNodeAbsent(tooLong)
    }

    @Test
    fun rulesAcceptANicknameOf20Characters() {
        val valid = newNodeId()
        assertNull("A valid game with a 20-character nickname was rejected", writeNodeDirectly(host, valid, gameNode(host.uid, "N".repeat(20), "waiting")))
        val stored = awaitGame("the valid game", host.service, valid) { it != null }
        assertEquals(20, stored?.host?.name?.length)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 30_000L
        const val CLEANUP_MILLIS = 15_000L
        const val HOST_NAME = "Anfitrion"
        const val GUEST_NAME = "Invitado"
        const val THIRD_NAME = "Tercero"
        const val RACE_ROUNDS = 5
    }
}
