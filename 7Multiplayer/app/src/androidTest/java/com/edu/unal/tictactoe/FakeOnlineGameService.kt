package com.edu.unal.tictactoe

import com.edu.unal.tictactoe.data.online.GameTransactions
import com.edu.unal.tictactoe.data.online.JoinResult
import com.edu.unal.tictactoe.data.online.MoveOutcome
import com.edu.unal.tictactoe.data.online.OnlineGame
import com.edu.unal.tictactoe.data.online.OnlineGameService
import com.edu.unal.tictactoe.data.online.OnlinePlayer
import com.edu.unal.tictactoe.data.online.OnlineServiceException
import com.edu.unal.tictactoe.data.online.OnlineStatus
import com.edu.unal.tictactoe.data.online.TransactionDecision
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory backend shared by the fake services of one test. It reproduces the semantics of the
 * real service: single-threaded, deterministic ids and timestamps, no clock and no real delays.
 */
class FakeOnlineBackend {
    private val games = MutableStateFlow<Map<String, OnlineGame>>(emptyMap())
    private val connections = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val services = mutableMapOf<String, FakeOnlineGameService>()
    private var counter = 0L

    /** Number of games created successfully. */
    var createCount = 0
        private set

    /** Number of calls to [OnlineGameService.cancelGame], whether or not they removed a game. */
    var cancelCount = 0
        private set

    /** Returns the service seen by [uid]; repeated calls return the same instance. */
    fun serviceFor(uid: String): FakeOnlineGameService =
        services.getOrPut(uid) { FakeOnlineGameService(this, uid) }

    /** Current state of the game [gameId], or null if it does not exist. */
    fun gameById(gameId: String): OnlineGame? = games.value[gameId]

    /** Makes [uid] observe a lost connection: its connection flow emits false. */
    fun disconnect(uid: String) {
        connection(uid).value = false
    }

    /** Makes [uid] observe a restored connection: its connection flow emits true. */
    fun reconnect(uid: String) {
        connection(uid).value = true
    }

    /** Simulates the server noticing that [uid] dropped: every game it takes part in is deleted. */
    fun loseConnection(uid: String) {
        games.update { all -> all.filterValues { !it.isParticipant(uid) } }
    }

    /** Deletes the game [gameId] from outside, as if a third party removed the node. */
    fun deleteGame(gameId: String) {
        games.update { it - gameId }
    }

    internal fun connection(uid: String): MutableStateFlow<Boolean> =
        connections.getOrPut(uid) { MutableStateFlow(true) }

    internal fun observeAvailable(): Flow<List<OnlineGame>> =
        games.map { all -> all.values.filter { it.status == OnlineStatus.Waiting } }.distinctUntilChanged()

    internal fun observe(gameId: String): Flow<OnlineGame?> =
        games.map { it[gameId] }.distinctUntilChanged()

    internal fun create(uid: String, nickname: String): String {
        counter++
        val id = "game-$counter"
        val game = OnlineGame(
            id = id,
            host = OnlinePlayer(uid, nickname),
            guest = null,
            board = List(BOARD_SIZE) { Mark.Empty },
            turn = Player.X,
            result = GameResult.InProgress,
            status = OnlineStatus.Waiting,
            createdAt = counter,
        )
        games.update { it + (id to game) }
        createCount++
        return id
    }

    internal fun cancel(uid: String, gameId: String) {
        cancelCount++
        remove(uid, gameId)
    }

    internal fun remove(uid: String, gameId: String) {
        games.update { all ->
            if (all[gameId]?.isParticipant(uid) == true) all - gameId else all
        }
    }

    /** Applies [decide] to the current value of [gameId]; returns true when a write was committed. */
    internal fun transact(gameId: String, decide: (OnlineGame?) -> TransactionDecision): Boolean {
        var committed = false
        games.update { all ->
            when (val decision = decide(all[gameId])) {
                is TransactionDecision.Write -> {
                    committed = true
                    all + (gameId to decision.game)
                }
                TransactionDecision.Abort, TransactionDecision.Unchanged -> {
                    committed = false
                    all
                }
            }
        }
        return committed
    }

    private fun OnlineGame.isParticipant(uid: String): Boolean = host.uid == uid || guest?.uid == uid

    private companion object {
        const val BOARD_SIZE = 9
    }
}

/** [OnlineGameService] of one uid over a [FakeOnlineBackend]; the switches below affect only this uid. */
class FakeOnlineGameService internal constructor(
    private val backend: FakeOnlineBackend,
    val uid: String,
) : OnlineGameService {
    private var signInGate: CompletableDeferred<Unit>? = null
    private var moveGate = CompletableDeferred<Unit>()
    private var moveHeld = false
    private var createFails = false
    private var moveRejected = false
    private var moveFails = false

    /** While true, [ensureSignedIn] throws [OnlineServiceException]; set it back to false to recover. */
    var signInFails = false

    /** Makes [ensureSignedIn] suspend until [releaseSignIn] is called. */
    fun holdSignIn() {
        signInGate = CompletableDeferred()
    }

    /** Resumes the [ensureSignedIn] calls held by [holdSignIn] and stops holding new ones. */
    fun releaseSignIn() {
        signInGate?.complete(Unit)
        signInGate = null
    }

    /** The next [createGame] throws [OnlineServiceException] and leaves no game behind. */
    fun failNextCreate() {
        createFails = true
    }

    /** The next [playMove] returns [MoveOutcome.Rejected] without changing the game. */
    fun rejectNextMove() {
        moveRejected = true
    }

    /** The next [playMove] throws [OnlineServiceException] without changing the game. */
    fun failNextMove() {
        moveFails = true
    }

    /** The next [playMove] suspends until [releaseHeldMove] is called, then proceeds normally. */
    fun holdNextMove() {
        moveGate = CompletableDeferred()
        moveHeld = true
    }

    /** Resumes the [playMove] held by [holdNextMove]. */
    fun releaseHeldMove() {
        moveGate.complete(Unit)
    }

    override suspend fun ensureSignedIn(): String {
        signInGate?.await()
        if (signInFails) throw OnlineServiceException("Sign-in failed")
        return uid
    }

    override fun observeConnection(): Flow<Boolean> = backend.connection(uid)

    override fun observeAvailableGames(): Flow<List<OnlineGame>> = backend.observeAvailable()

    override suspend fun createGame(nickname: String): String {
        if (createFails) {
            createFails = false
            throw OnlineServiceException("Create failed")
        }
        return backend.create(uid, nickname)
    }

    override suspend fun cancelGame(gameId: String) {
        backend.cancel(uid, gameId)
    }

    override suspend fun joinGame(gameId: String, nickname: String): JoinResult {
        val joined = backend.transact(gameId) { GameTransactions.join(it, OnlinePlayer(uid, nickname)) }
        return if (joined) JoinResult.Joined else JoinResult.NoLongerAvailable
    }

    override fun observeGame(gameId: String): Flow<OnlineGame?> = backend.observe(gameId)

    override suspend fun playMove(gameId: String, location: Int): MoveOutcome {
        if (moveHeld) {
            moveHeld = false
            moveGate.await()
        }
        if (moveFails) {
            moveFails = false
            throw OnlineServiceException("Move failed")
        }
        if (moveRejected) {
            moveRejected = false
            return MoveOutcome.Rejected
        }
        val accepted = backend.transact(gameId) { GameTransactions.play(it, uid, location) }
        return if (accepted) MoveOutcome.Accepted else MoveOutcome.Rejected
    }

    override suspend fun leaveGame(gameId: String) {
        backend.remove(uid, gameId)
    }
}
