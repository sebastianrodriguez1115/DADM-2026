package com.edu.unal.tictactoe.data.online

import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * [OnlineGameService] over Firebase Realtime Database and anonymous Firebase Auth. Games live at
 * `/games/{gameId}` (see [GameFields]); persistence on disk is never enabled.
 *
 * Suspend operations report any failure as [OnlineServiceException], except [joinGame] (any
 * failure is [JoinResult.NoLongerAvailable]) and [playMove] (any failure is
 * [MoveOutcome.Rejected]). Cancellation always propagates untouched. Flows never fail: when the
 * server cancels a listener (for example, permission denied) they emit their "nothing" value and
 * complete normally.
 */
class FirebaseOnlineGameService(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : OnlineGameService {
    private val games: DatabaseReference get() = database.getReference(GAMES_PATH)

    override suspend fun ensureSignedIn(): String = translating("Could not sign in") {
        auth.currentUser?.uid
            ?: auth.signInAnonymously().awaitTask().user?.uid
            ?: throw OnlineServiceException("Sign-in returned no user")
    }

    override fun observeConnection(): Flow<Boolean> = callbackFlow {
        val reference = database.getReference(CONNECTED_PATH)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.value == true)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(false)
                close()
            }
        }
        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }.catch { emit(false) }

    override fun observeAvailableGames(): Flow<List<OnlineGame>> = callbackFlow {
        val query = games.orderByChild(GameFields.STATUS).equalTo(GameFields.WAITING)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val available = snapshot.children
                    .mapNotNull { child -> child.key?.let { id -> gameFromValue(id, child.value) } }
                    .sortedByDescending { it.createdAt }
                trySend(available)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
                close()
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }.catch { emit(emptyList()) }

    override fun observeGame(gameId: String): Flow<OnlineGame?> = callbackFlow {
        val reference = games.child(gameId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(gameFromValue(gameId, snapshot.value))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
                close()
            }
        }
        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }.catch { emit(null) }

    /**
     * Order: sign in, `push()` for the key, `onDisconnect().removeValue()` and wait for its
     * confirmation (a failure here means the game is never written), then `setValue` of the node.
     * If the write fails or is cancelled, the node and the registered operation are withdrawn.
     */
    override suspend fun createGame(nickname: String): String = translating("Could not create the game") {
        val uid = ensureSignedIn()
        val reference = games.push()
        val id = reference.key ?: throw OnlineServiceException("The database returned no game key")
        reference.onDisconnect().removeValue().awaitTask()
        try {
            reference.setValue(newGameValue(OnlinePlayer(uid, nickname), ServerValue.TIMESTAMP)).awaitTask()
        } catch (failure: Throwable) {
            withdraw(reference)
            throw failure
        }
        id
    }

    /** Order: `removeValue()` of the node, then `onDisconnect().cancel()`. */
    override suspend fun cancelGame(gameId: String): Unit = removeGame(gameId)

    /** Order: sign in, a transaction on the node with [GameTransactions.join], then (if joined) `onDisconnect().removeValue()`. */
    override suspend fun joinGame(gameId: String, nickname: String): JoinResult {
        val reference: DatabaseReference
        val uid: String
        try {
            uid = ensureSignedIn()
            reference = games.child(gameId)
            val guest = OnlinePlayer(uid, nickname)
            val committed = transact(reference, fireLocalEvents = true) { current ->
                GameTransactions.join(gameFromValue(gameId, current), guest)
            }
            val game = committed?.let { gameFromValue(gameId, it.value) }
            if (game?.guest?.uid != uid || game.status != OnlineStatus.Playing) {
                return JoinResult.NoLongerAvailable
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return JoinResult.NoLongerAvailable
        }
        try {
            reference.onDisconnect().removeValue().awaitTask()
        } catch (failure: Throwable) {
            // Joined but not protected against a dropped connection: end the game instead of leaving it stuck.
            runCatching { reference.removeValue() }
            if (failure is CancellationException) throw failure
            return JoinResult.NoLongerAvailable
        }
        return JoinResult.Joined
    }

    /** Order: sign in, a transaction on the node with [GameTransactions.play] and local events off. */
    override suspend fun playMove(gameId: String, location: Int): MoveOutcome {
        try {
            val uid = ensureSignedIn()
            val committed = transact(games.child(gameId), fireLocalEvents = false) { current ->
                GameTransactions.play(gameFromValue(gameId, current), uid, location)
            }
            val game = committed?.let { gameFromValue(gameId, it.value) } ?: return MoveOutcome.Rejected
            val myMark = when (uid) {
                game.host.uid -> Mark.X
                game.guest?.uid -> Mark.O
                else -> return MoveOutcome.Rejected
            }
            return if (game.board.getOrNull(location) == myMark) MoveOutcome.Accepted else MoveOutcome.Rejected
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return MoveOutcome.Rejected
        }
    }

    /** Order: `removeValue()` of the node, then `onDisconnect().cancel()`. */
    override suspend fun leaveGame(gameId: String): Unit = removeGame(gameId)

    private suspend fun removeGame(gameId: String): Unit = translating("Could not remove the game") {
        val reference = games.child(gameId)
        reference.removeValue().awaitTask()
        reference.onDisconnect().cancel().awaitTask()
        Unit
    }

    /** Best effort, without waiting: removes the node and the operation registered for it. */
    private fun withdraw(reference: DatabaseReference) {
        runCatching { reference.removeValue() }
        runCatching { reference.onDisconnect().cancel() }
    }

    /**
     * Runs a transaction whose decision is made by [decide] over the raw node value. Returns the
     * confirmed snapshot, or null when the transaction was aborted; fails when the database reports an error.
     */
    private suspend fun transact(
        reference: DatabaseReference,
        fireLocalEvents: Boolean,
        decide: (Any?) -> TransactionDecision,
    ): DataSnapshot? = suspendCancellableCoroutine { continuation ->
        reference.runTransaction(
            object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result =
                    when (val decision = decide(currentData.value)) {
                        TransactionDecision.Abort -> Transaction.abort()
                        TransactionDecision.Unchanged -> Transaction.success(currentData)
                        is TransactionDecision.Write -> {
                            currentData.value = gameToValue(decision.game)
                            Transaction.success(currentData)
                        }
                    }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    when {
                        error != null -> continuation.resumeWithException(error.toException())
                        committed -> continuation.resume(currentData)
                        else -> continuation.resume(null)
                    }
                }
            },
            fireLocalEvents,
        )
    }

    private suspend fun <T> translating(message: String, block: suspend () -> T): T = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: OnlineServiceException) {
        throw failure
    } catch (failure: Exception) {
        throw OnlineServiceException(message, failure)
    }

    private companion object {
        const val GAMES_PATH = "games"
        const val CONNECTED_PATH = ".info/connected"
    }
}
