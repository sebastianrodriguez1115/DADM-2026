package com.edu.unal.tictactoe.data.preferences

import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Score
import org.junit.Assert.assertEquals
import org.junit.Test

class GamePreferencesTest {
    @Test
    fun difficultyCodesAreStableAndRoundTrip() {
        val expected = listOf(
            DifficultyCode.EASY to DifficultyLevel.Easy,
            DifficultyCode.HARD to DifficultyLevel.Hard,
            DifficultyCode.EXPERT to DifficultyLevel.Expert,
            DifficultyCode.IMPOSSIBLE to DifficultyLevel.Impossible,
        )

        assertEquals(listOf(0, 1, 2, 3), expected.map { it.first })
        expected.forEach { (code, level) ->
            assertEquals(code, DifficultyCode.encode(level))
            assertEquals(level, DifficultyCode.decode(code))
        }
    }

    @Test
    fun missingOrUnknownDifficultyUsesExpert() {
        assertEquals(DifficultyLevel.Expert, DifficultyCode.decode(null))
        assertEquals(DifficultyLevel.Expert, DifficultyCode.decode(-1))
        assertEquals(DifficultyLevel.Expert, DifficultyCode.decode(4))
        assertEquals(DifficultyLevel.Expert, DifficultyCode.decode(Int.MIN_VALUE))
    }

    @Test
    fun fakeDefaultsAndStoresScoreAndDifficultyIndependently() {
        val preferences = FakeGamePreferences()
        assertEquals(Score(), preferences.readScore())
        assertEquals(DifficultyLevel.Expert, preferences.readDifficulty())

        val score = Score(xWins = 3, oWins = 5, ties = 2)
        preferences.writeScore(score)
        assertEquals(score, preferences.readScore())
        assertEquals(DifficultyLevel.Expert, preferences.readDifficulty())

        preferences.writeDifficulty(DifficultyLevel.Impossible)
        assertEquals(score, preferences.readScore())
        assertEquals(DifficultyLevel.Impossible, preferences.readDifficulty())
    }
}
