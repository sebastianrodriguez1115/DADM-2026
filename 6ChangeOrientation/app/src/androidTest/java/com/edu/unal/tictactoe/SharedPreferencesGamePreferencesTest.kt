package com.edu.unal.tictactoe

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.data.preferences.DifficultyCode
import com.edu.unal.tictactoe.data.preferences.GamePreferenceSchema
import com.edu.unal.tictactoe.data.preferences.SharedPreferencesGamePreferences
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Score
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesGamePreferencesTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val fixture = GamePreferencesTestFixture()
    private val rawPreferences
        get() = context.getSharedPreferences(GamePreferenceSchema.FILE_NAME, Context.MODE_PRIVATE)

    @Before
    fun clearGamePreferences() {
        fixture.prepare(context, GamePreferencesTestFixture.Seed())
    }

    @After
    fun cleanupGamePreferences() {
        fixture.cleanup()
    }

    @Test
    fun absentKeysDefaultToZeroAndExpert() {
        fixture.prepare(context, GamePreferencesTestFixture.Seed())
        val store = SharedPreferencesGamePreferences(context)

        assertEquals(Score(), store.readScore())
        assertEquals(DifficultyLevel.Expert, store.readDifficulty())
    }

    @Test
    fun eachInvalidScoreIsNormalizedIndependentlyAndValidScoresSurvive() {
        val cases = listOf(
            Triple(GamePreferenceSchema.SCORE_X_WINS, -8, Score(0, 12, 13)),
            Triple(GamePreferenceSchema.SCORE_O_WINS, -9, Score(11, 0, 13)),
            Triple(GamePreferenceSchema.SCORE_TIES, -10, Score(11, 12, 0)),
        )
        for ((invalidKey, invalidValue, expected) in cases) {
            fixture.prepare(context, GamePreferencesTestFixture.Seed(xWins = 11, oWins = 12, ties = 13))
            rawPreferences.edit().putInt(invalidKey, invalidValue).commit()
            assertEquals(expected, SharedPreferencesGamePreferences(context).readScore())
            assertTrue("Reads must not rewrite invalid value", rawPreferences.getInt(invalidKey, 0) < 0)
        }
    }

    @Test
    fun wrongTypedScoresAndDifficultyFallbackPerKey() {
        val wrongTypedWriters = listOf<(android.content.SharedPreferences.Editor, String) -> Unit>(
            { editor, key -> editor.putString(key, "wrong") },
            { editor, key -> editor.putLong(key, 42L) },
            { editor, key -> editor.putBoolean(key, true) },
        )
        val scoreKeys = listOf(
            GamePreferenceSchema.SCORE_X_WINS,
            GamePreferenceSchema.SCORE_O_WINS,
            GamePreferenceSchema.SCORE_TIES,
        )
        val initialScore = Score(11, 12, 13)
        val scoreValues = mapOf(
            GamePreferenceSchema.SCORE_X_WINS to 11,
            GamePreferenceSchema.SCORE_O_WINS to 12,
            GamePreferenceSchema.SCORE_TIES to 13,
        )
        for (key in scoreKeys) {
            val expected = when (key) {
                GamePreferenceSchema.SCORE_X_WINS -> Score(0, 12, 13)
                GamePreferenceSchema.SCORE_O_WINS -> Score(11, 0, 13)
                else -> Score(11, 12, 0)
            }
            for (writer in wrongTypedWriters) {
                fixture.prepare(context, GamePreferencesTestFixture.Seed(
                    xWins = initialScore.xWins, oWins = initialScore.oWins, ties = initialScore.ties,
                ))
                val editor = rawPreferences.edit()
                writer(editor, key)
                editor.commit()
                val rawValue = rawPreferences.all[key]

                assertEquals(expected, SharedPreferencesGamePreferences(context).readScore())
                assertEquals("Wrong-typed value must remain untouched", rawValue, rawPreferences.all[key])
                scoreKeys.filterNot { it == key }.forEach { validKey ->
                    assertEquals(scoreValues.getValue(validKey), rawPreferences.getInt(validKey, -1))
                }
            }
        }

        val absentSeeds = listOf(
            GamePreferencesTestFixture.Seed(xWins = null, oWins = 12, ties = 13) to Score(0, 12, 13),
            GamePreferencesTestFixture.Seed(xWins = 11, oWins = null, ties = 13) to Score(11, 0, 13),
            GamePreferencesTestFixture.Seed(xWins = 11, oWins = 12, ties = null) to Score(11, 12, 0),
        )
        for ((seed, expected) in absentSeeds) {
            fixture.prepare(context, seed)
            assertEquals(expected, SharedPreferencesGamePreferences(context).readScore())
        }

        val difficultyWrongTypeValues = listOf("wrong", 42L, true)
        for ((index, wrongValue) in difficultyWrongTypeValues.withIndex()) {
            fixture.prepare(context, GamePreferencesTestFixture.Seed(
                xWins = 11, oWins = 12, ties = 13, difficultyCode = DifficultyCode.IMPOSSIBLE,
            ))
            val editor = rawPreferences.edit()
            wrongTypedWriters[index](editor, GamePreferenceSchema.DIFFICULTY_CODE)
            editor.commit()
            assertEquals(DifficultyLevel.Expert, SharedPreferencesGamePreferences(context).readDifficulty())
            assertEquals(wrongValue, rawPreferences.all[GamePreferenceSchema.DIFFICULTY_CODE])
            assertEquals(Score(11, 12, 13), SharedPreferencesGamePreferences(context).readScore())
        }

        for (code in listOf(-1, 4, Int.MIN_VALUE)) {
            rawPreferences.edit().putInt(GamePreferenceSchema.DIFFICULTY_CODE, code).commit()
            assertEquals(DifficultyLevel.Expert, SharedPreferencesGamePreferences(context).readDifficulty())
            assertEquals(code, rawPreferences.getInt(GamePreferenceSchema.DIFFICULTY_CODE, 0))
        }
        fixture.prepare(context, GamePreferencesTestFixture.Seed(difficultyCode = null))
        assertEquals(DifficultyLevel.Expert, SharedPreferencesGamePreferences(context).readDifficulty())
    }

    @Test
    fun stableDifficultyCodesRoundTripAndWritesRestoreFromFreshAdapter() {
        val expected = listOf(
            DifficultyLevel.Easy to DifficultyCode.EASY,
            DifficultyLevel.Hard to DifficultyCode.HARD,
            DifficultyLevel.Expert to DifficultyCode.EXPERT,
            DifficultyLevel.Impossible to DifficultyCode.IMPOSSIBLE,
        )
        for ((level, code) in expected) {
            SharedPreferencesGamePreferences(context).writeDifficulty(level)
            assertEquals(code, rawPreferences.getInt(GamePreferenceSchema.DIFFICULTY_CODE, -1))
            assertEquals(level, SharedPreferencesGamePreferences(context).readDifficulty())
        }

        SharedPreferencesGamePreferences(context).writeScore(Score(21, 22, 23))
        SharedPreferencesGamePreferences(context).writeDifficulty(DifficultyLevel.Hard)
        val reloaded = SharedPreferencesGamePreferences(context)
        assertEquals(Score(21, 22, 23), reloaded.readScore())
        assertEquals(DifficultyLevel.Hard, reloaded.readDifficulty())
    }

    @Test
    fun difficultyCodesAreTheStableLiteralsZeroToThree() {
        val literals = listOf(
            DifficultyLevel.Easy to 0,
            DifficultyLevel.Hard to 1,
            DifficultyLevel.Expert to 2,
            DifficultyLevel.Impossible to 3,
        )
        for ((level, code) in literals) {
            SharedPreferencesGamePreferences(context).writeDifficulty(level)
            assertEquals(code, rawPreferences.getInt(GamePreferenceSchema.DIFFICULTY_CODE, -1))

            fixture.prepare(context, GamePreferencesTestFixture.Seed(difficultyCode = code))
            assertEquals(level, SharedPreferencesGamePreferences(context).readDifficulty())
        }
    }

    @Test
    fun writesAreImmediatelyVisibleAndPreserveUnrelatedPreference() {
        rawPreferences.edit().putString("unrelated", "keep").commit()
        val store = SharedPreferencesGamePreferences(context)
        store.writeScore(Score(3, 4, 5))
        store.writeDifficulty(DifficultyLevel.Impossible)

        assertEquals(Score(3, 4, 5), store.readScore())
        assertEquals(DifficultyLevel.Impossible, store.readDifficulty())
        assertEquals("keep", rawPreferences.getString("unrelated", null))
        assertEquals(
            setOf("unrelated", GamePreferenceSchema.SCORE_X_WINS, GamePreferenceSchema.SCORE_O_WINS,
                GamePreferenceSchema.SCORE_TIES, GamePreferenceSchema.DIFFICULTY_CODE),
            rawPreferences.all.keys,
        )
        assertFalse(rawPreferences.contains("sound_enabled"))
    }

    @Test
    fun obtainsPreferencesFromApplicationContextRatherThanSuppliedWrapper() {
        val wrapper = object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = context

            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences =
                error("Must use applicationContext")
        }

        assertEquals(Score(), SharedPreferencesGamePreferences(wrapper).readScore())
    }
}
