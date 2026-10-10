package com.edu.unal.tictactoe.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Score

/**
 * Android persistence for the three scores and difficulty only. The application context is
 * converted immediately to [SharedPreferences]; no Activity or other Context is retained.
 * Writes use [SharedPreferences.Editor.apply], which updates the in-memory preferences immediately
 * and schedules disk persistence asynchronously; it cannot guarantee a flush before abrupt death.
 *
 * [SharedPreferences.Editor.commit] is deliberately not used because it writes synchronously and
 * would block the main thread. The last write may therefore be lost if the process is killed,
 * force-stopped or loses power before the deferred flush, so "score counted once" is a best-effort
 * property under abrupt termination, not a universal guarantee.
 */
class SharedPreferencesGamePreferences(context: Context) : GamePreferences {
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        GamePreferenceSchema.FILE_NAME,
        Context.MODE_PRIVATE,
    )

    override fun readScore(): Score = Score(
        xWins = readNonNegativeInt(GamePreferenceSchema.SCORE_X_WINS),
        oWins = readNonNegativeInt(GamePreferenceSchema.SCORE_O_WINS),
        ties = readNonNegativeInt(GamePreferenceSchema.SCORE_TIES),
    )

    override fun writeScore(score: Score) {
        preferences.edit()
            .putInt(GamePreferenceSchema.SCORE_X_WINS, score.xWins.coerceAtLeast(0))
            .putInt(GamePreferenceSchema.SCORE_O_WINS, score.oWins.coerceAtLeast(0))
            .putInt(GamePreferenceSchema.SCORE_TIES, score.ties.coerceAtLeast(0))
            .apply()
    }

    override fun readDifficulty(): DifficultyLevel = DifficultyCode.decode(
        try {
            preferences.getInt(GamePreferenceSchema.DIFFICULTY_CODE, DifficultyCode.EXPERT)
        } catch (_: ClassCastException) {
            null
        },
    )

    override fun writeDifficulty(difficulty: DifficultyLevel) {
        preferences.edit()
            .putInt(GamePreferenceSchema.DIFFICULTY_CODE, DifficultyCode.encode(difficulty))
            .apply()
    }

    private fun readNonNegativeInt(key: String): Int = try {
        preferences.getInt(key, 0).takeIf { it >= 0 } ?: 0
    } catch (_: ClassCastException) {
        0
    }
}
