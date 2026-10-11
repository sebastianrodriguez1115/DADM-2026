package com.edu.unal.tictactoe.data.online

import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player

/** Child names and values of a game node; they must match `firebase/database.rules.json`. */
internal object GameFields {
    const val STATUS = "status"
    const val HOST = "host"
    const val GUEST = "guest"
    const val UID = "uid"
    const val NAME = "name"
    const val BOARD = "board"
    const val TURN = "turn"
    const val RESULT = "result"
    const val CREATED_AT = "createdAt"

    const val WAITING = "waiting"
    const val PLAYING = "playing"
    const val FINISHED = "finished"
    const val NONE = "none"
    const val TIE = "tie"
}

/**
 * Converts the value stored at a game node (a map of children, as the database reports it) into
 * an [OnlineGame]. Returns null when [value] is not a well-formed game, which callers treat as a
 * missing node.
 */
internal fun gameFromValue(id: String, value: Any?): OnlineGame? {
    val fields = value as? Map<*, *> ?: return null
    val host = playerFromValue(fields[GameFields.HOST]) ?: return null
    val guest = if (fields.containsKey(GameFields.GUEST)) {
        playerFromValue(fields[GameFields.GUEST]) ?: return null
    } else {
        null
    }
    val board = BoardCodec.decode(fields[GameFields.BOARD] as? String ?: return null) ?: return null
    return OnlineGame(
        id = id,
        host = host,
        guest = guest,
        board = board,
        turn = turnFromText(fields[GameFields.TURN] as? String) ?: return null,
        result = resultFromText(fields[GameFields.RESULT] as? String) ?: return null,
        status = statusFromText(fields[GameFields.STATUS] as? String) ?: return null,
        createdAt = longFromValue(fields[GameFields.CREATED_AT]) ?: return null,
    )
}

/** Converts [game] into the map of children written to its node; the guest is omitted while absent. */
internal fun gameToValue(game: OnlineGame): Map<String, Any> {
    val value = newGameValue(game.host, game.createdAt) + mapOf(
        GameFields.STATUS to statusToText(game.status),
        GameFields.BOARD to BoardCodec.encode(game.board),
        GameFields.TURN to game.turn.symbol.toString(),
        GameFields.RESULT to resultToText(game.result),
    )
    val guest = game.guest ?: return value
    return value + (GameFields.GUEST to playerToValue(guest))
}

/** Children of a freshly created game; [createdAt] is the server timestamp placeholder when writing. */
internal fun newGameValue(host: OnlinePlayer, createdAt: Any): Map<String, Any> = mapOf(
    GameFields.STATUS to GameFields.WAITING,
    GameFields.HOST to playerToValue(host),
    GameFields.BOARD to BoardCodec.encode(List(BOARD_CELLS) { Mark.Empty }),
    GameFields.TURN to Player.X.symbol.toString(),
    GameFields.RESULT to GameFields.NONE,
    GameFields.CREATED_AT to createdAt,
)

private const val BOARD_CELLS = 9

private fun playerToValue(player: OnlinePlayer): Map<String, Any> =
    mapOf(GameFields.UID to player.uid, GameFields.NAME to player.name)

private fun playerFromValue(value: Any?): OnlinePlayer? {
    val fields = value as? Map<*, *> ?: return null
    val uid = fields[GameFields.UID] as? String ?: return null
    val name = fields[GameFields.NAME] as? String ?: return null
    return OnlinePlayer(uid, name)
}

private fun statusFromText(text: String?): OnlineStatus? = when (text) {
    GameFields.WAITING -> OnlineStatus.Waiting
    GameFields.PLAYING -> OnlineStatus.Playing
    GameFields.FINISHED -> OnlineStatus.Finished
    else -> null
}

private fun statusToText(status: OnlineStatus): String = when (status) {
    OnlineStatus.Waiting -> GameFields.WAITING
    OnlineStatus.Playing -> GameFields.PLAYING
    OnlineStatus.Finished -> GameFields.FINISHED
}

private fun turnFromText(text: String?): Player? = Player.entries.firstOrNull { it.symbol.toString() == text }

private fun resultFromText(text: String?): GameResult? = when (text) {
    GameFields.NONE -> GameResult.InProgress
    GameFields.TIE -> GameResult.Tie
    else -> turnFromText(text)?.let { GameResult.Won(it) }
}

private fun resultToText(result: GameResult): String = when (result) {
    GameResult.InProgress -> GameFields.NONE
    GameResult.Tie -> GameFields.TIE
    is GameResult.Won -> result.player.symbol.toString()
}

/** The database reports whole numbers as Long, or as Double when they do not fit; both are accepted when integral. */
private fun longFromValue(value: Any?): Long? = when (value) {
    is Long -> value
    is Int -> value.toLong()
    is Double -> value.takeIf { it % 1.0 == 0.0 }?.toLong()
    else -> null
}
