package com.edu.unal.tictactoe.data.preferences

import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Score

/**
 * Boundary for durable game options only. Implementations expose no Android types; scores default
 * to zero and difficulty to [DifficultyLevel.Expert]. Session state and sound are out of scope.
 * Durability depends on the implementation and is best-effort.
 */
interface GamePreferences {
    fun readScore(): Score
    fun writeScore(score: Score)
    fun readDifficulty(): DifficultyLevel
    fun writeDifficulty(difficulty: DifficultyLevel)
}
