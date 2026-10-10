package com.edu.unal.tictactoe.data.preferences

/**
 * Stable on-disk names for game preferences. The SharedPreferences adapter and Android test
 * fixture share this schema; keep these names unchanged once persisted data exists.
 */
object GamePreferenceSchema {
    const val FILE_NAME = "game_preferences"

    const val SCORE_X_WINS = "score_x_wins"
    const val SCORE_O_WINS = "score_o_wins"
    const val SCORE_TIES = "score_ties"
    const val DIFFICULTY_CODE = "difficulty_code"
}
