package com.edu.unal.tictactoe.data.preferences

import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Score

/** In-memory test double for the durable score and difficulty boundary, with access counters. */
class FakeGamePreferences(
    private var score: Score = Score(),
    private var difficulty: DifficultyLevel = DifficultyLevel.Expert,
) : GamePreferences {
    /** Current stored values, readable without incrementing the access counters. */
    val storedScore: Score get() = score
    val storedDifficulty: DifficultyLevel get() = difficulty

    var scoreReads = 0
        private set
    var difficultyReads = 0
        private set
    var scoreWrites = 0
        private set
    var difficultyWrites = 0
        private set

    override fun readScore(): Score {
        scoreReads++
        return score
    }

    override fun writeScore(score: Score) {
        scoreWrites++
        this.score = score
    }

    override fun readDifficulty(): DifficultyLevel {
        difficultyReads++
        return difficulty
    }

    override fun writeDifficulty(difficulty: DifficultyLevel) {
        difficultyWrites++
        this.difficulty = difficulty
    }
}
