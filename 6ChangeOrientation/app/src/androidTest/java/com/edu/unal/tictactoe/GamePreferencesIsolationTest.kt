package com.edu.unal.tictactoe

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.data.preferences.DifficultyCode
import com.edu.unal.tictactoe.data.preferences.GamePreferenceSchema
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GamePreferencesIsolationTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val fixture = GamePreferencesTestFixture()
    private val gamePrefs
        get() = context.getSharedPreferences(GamePreferenceSchema.FILE_NAME, Context.MODE_PRIVATE)

    @Before
    fun clearBeforeDirectPreferencesAssertions() {
        fixture.prepare(context, GamePreferencesTestFixture.Seed())
    }

    @After
    fun clearGamePreferencesAfterAssertions() {
        try {
            fixture.cleanup()
        } finally {
            context.getSharedPreferences("unrelated_test_preferences", Context.MODE_PRIVATE)
                .edit().remove("sentinel").commit()
        }
    }

    @Test
    fun preparationRemovesResidueSeedsZerosAndExpertAndLeavesOtherFilesAlone() {
        val otherPrefs = context.getSharedPreferences("unrelated_test_preferences", Context.MODE_PRIVATE)
        otherPrefs.edit().putString("sentinel", "untouched").commit()
        gamePrefs.edit()
            .putInt(GamePreferenceSchema.SCORE_X_WINS, 29)
            .putInt(GamePreferenceSchema.SCORE_O_WINS, 31)
            .putInt(GamePreferenceSchema.SCORE_TIES, 37)
            .putInt(GamePreferenceSchema.DIFFICULTY_CODE, DifficultyCode.IMPOSSIBLE)
            .putString("stale_test_only_key", "stale")
            .commit()

        fixture.prepare(context)

        assertEquals(0, gamePrefs.getInt(GamePreferenceSchema.SCORE_X_WINS, -1))
        assertEquals(0, gamePrefs.getInt(GamePreferenceSchema.SCORE_O_WINS, -1))
        assertEquals(0, gamePrefs.getInt(GamePreferenceSchema.SCORE_TIES, -1))
        assertEquals(DifficultyCode.EXPERT, gamePrefs.getInt(GamePreferenceSchema.DIFFICULTY_CODE, -1))
        assertFalse(gamePrefs.contains("stale_test_only_key"))
        assertEquals("untouched", otherPrefs.getString("sentinel", null))
    }

    @Test
    fun preparationAppliesCustomSeedDeterministicallyAndCanLeaveKeysAbsent() {
        fixture.prepare(
            context,
            GamePreferencesTestFixture.Seed(
                xWins = 4,
                oWins = 7,
                ties = 2,
                difficultyCode = DifficultyCode.HARD,
            ),
        )
        gamePrefs.edit().putInt(GamePreferenceSchema.SCORE_X_WINS, 999).commit()

        fixture.prepare(
            context,
            GamePreferencesTestFixture.Seed(xWins = 6, difficultyCode = DifficultyCode.EASY),
        )

        assertEquals(6, gamePrefs.getInt(GamePreferenceSchema.SCORE_X_WINS, -1))
        assertEquals(DifficultyCode.EASY, gamePrefs.getInt(GamePreferenceSchema.DIFFICULTY_CODE, -1))
        assertFalse(gamePrefs.contains(GamePreferenceSchema.SCORE_O_WINS))
        assertFalse(gamePrefs.contains(GamePreferenceSchema.SCORE_TIES))
    }
}
