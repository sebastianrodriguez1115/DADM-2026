package com.edu.unal.tictactoe

import com.edu.unal.tictactoe.data.preferences.GamePreferences
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Score

/** In-memory store for instrumented tests that build the ViewModel without touching real prefs. */
class InMemoryGamePreferences : GamePreferences {
    private var score = Score()
    private var difficulty = DifficultyLevel.Expert

    override fun readScore(): Score = score

    override fun writeScore(score: Score) {
        this.score = score
    }

    override fun readDifficulty(): DifficultyLevel = difficulty

    override fun writeDifficulty(difficulty: DifficultyLevel) {
        this.difficulty = difficulty
    }
}
