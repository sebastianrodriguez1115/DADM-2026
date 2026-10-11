package com.edu.unal.tictactoe.data.online

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/**
 * The only production wiring point of the online service and the real nickname store.
 *
 * PROVISIONAL: until task 7.6 of the online-multiplayer change, [gameService] is a private
 * [UnavailableOnlineGameService] whose identity always fails, so the lobby shows its connection
 * failure with "retry". Task 7.6 replaces it with the Firebase service and deletes the provisional
 * class.
 */
object OnlineServices {
    val gameService: OnlineGameService = UnavailableOnlineGameService

    fun nicknameStore(context: Context): NicknameStore = SharedPreferencesNicknameStore(context)
}

/** Provisional service of [OnlineServices]: nothing can be done with it, so every operation fails. */
private object UnavailableOnlineGameService : OnlineGameService {
    override suspend fun ensureSignedIn(): String = unavailable()

    override fun observeConnection(): Flow<Boolean> = emptyFlow()

    override fun observeAvailableGames(): Flow<List<OnlineGame>> = flowOf(emptyList())

    override suspend fun createGame(nickname: String): String = unavailable()

    override suspend fun cancelGame(gameId: String): Unit = unavailable()

    override suspend fun joinGame(gameId: String, nickname: String): JoinResult = unavailable()

    override fun observeGame(gameId: String): Flow<OnlineGame?> = emptyFlow()

    override suspend fun playMove(gameId: String, location: Int): MoveOutcome = unavailable()

    override suspend fun leaveGame(gameId: String): Unit = unavailable()

    private fun unavailable(): Nothing = throw OnlineServiceException("The online service is not available yet")
}
