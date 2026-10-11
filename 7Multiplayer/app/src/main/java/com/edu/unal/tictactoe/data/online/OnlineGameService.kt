package com.edu.unal.tictactoe.data.online

import kotlinx.coroutines.flow.Flow

/** Failure reported by an [OnlineGameService] operation that cannot complete. */
class OnlineServiceException(message: String? = null, cause: Throwable? = null) :
    Exception(message, cause)

sealed interface JoinResult {
    data object Joined : JoinResult

    data object NoLongerAvailable : JoinResult
}

sealed interface MoveOutcome {
    data object Accepted : MoveOutcome

    data object Rejected : MoveOutcome
}

/**
 * Boundary between the online ViewModels and the backend. Implementations expose no vendor
 * types; operations that cannot complete throw [OnlineServiceException].
 */
interface OnlineGameService {
    /** Returns the uid of the current user, signing in if needed. */
    suspend fun ensureSignedIn(): String

    fun observeConnection(): Flow<Boolean>

    /** Emits the games that are waiting for a guest. */
    fun observeAvailableGames(): Flow<List<OnlineGame>>

    /** Creates a waiting game hosted by the current user and returns its id. */
    suspend fun createGame(nickname: String): String

    suspend fun cancelGame(gameId: String)

    suspend fun joinGame(gameId: String, nickname: String): JoinResult

    /** Emits null when the game no longer exists. */
    fun observeGame(gameId: String): Flow<OnlineGame?>

    suspend fun playMove(gameId: String, location: Int): MoveOutcome

    suspend fun leaveGame(gameId: String)
}
