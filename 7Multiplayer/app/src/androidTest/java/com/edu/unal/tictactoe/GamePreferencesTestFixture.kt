package com.edu.unal.tictactoe

import android.content.Context
import android.content.SharedPreferences
import com.edu.unal.tictactoe.data.preferences.DifficultyCode
import com.edu.unal.tictactoe.data.preferences.GamePreferenceSchema

/** Per-test reset for the one durable game-preferences file; no Activity is started here. */
internal class GamePreferencesTestFixture {
    private var preferences: SharedPreferences? = null

    fun prepare(context: Context, seed: Seed = DEFAULT_SEED) {
        val appContext = context.applicationContext
        val gamePreferences = appContext.getSharedPreferences(
            GamePreferenceSchema.FILE_NAME,
            Context.MODE_PRIVATE,
        )
        // Keep the handle before mutating so @After can still clean up if clear/seed fails.
        preferences = gamePreferences
        check(gamePreferences.edit().clear().commit()) {
            "Could not clear game SharedPreferences before test"
        }
        val editor = gamePreferences.edit()
        seed.xWins?.let { editor.putInt(GamePreferenceSchema.SCORE_X_WINS, it) }
        seed.oWins?.let { editor.putInt(GamePreferenceSchema.SCORE_O_WINS, it) }
        seed.ties?.let { editor.putInt(GamePreferenceSchema.SCORE_TIES, it) }
        seed.difficultyCode?.let { editor.putInt(GamePreferenceSchema.DIFFICULTY_CODE, it) }
        check(editor.commit()) { "Could not seed game SharedPreferences before test" }
    }

    /** Clear after the test (call only after closing its ActivityScenario). */
    fun cleanup() {
        preferences?.let { prefs ->
            check(prefs.edit().clear().commit()) {
                "Could not clear game SharedPreferences after test"
            }
        }
        preferences = null
    }

    data class Seed(
        val xWins: Int? = null,
        val oWins: Int? = null,
        val ties: Int? = null,
        val difficultyCode: Int? = null,
    )

    companion object {
        /** Explicitly represents the state production uses for a fresh install. */
        val DEFAULT_SEED = Seed(
            xWins = 0,
            oWins = 0,
            ties = 0,
            difficultyCode = DifficultyCode.EXPERT,
        )
    }
}
