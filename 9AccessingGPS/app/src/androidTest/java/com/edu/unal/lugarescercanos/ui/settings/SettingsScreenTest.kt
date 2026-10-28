package com.edu.unal.lugarescercanos.ui.settings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.lugarescercanos.ui.theme.LugaresCercanosTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val changes = mutableListOf<Double>()
    private var finished = 0
    private var backs = 0

    private fun show(state: SettingsUiState) {
        rule.setContent {
            LugaresCercanosTheme {
                SettingsScreen(
                    state = state,
                    onRadiusChange = { changes += it },
                    onRadiusChangeFinished = { finished++ },
                    onBack = { backs++ },
                )
            }
        }
    }

    @Test
    fun defaultRadiusShowsOneKm() {
        show(SettingsUiState.forRadius(1.0))

        rule.onNodeWithText("Ajustes").assertIsDisplayed()
        rule.onNodeWithText("1 km").assertIsDisplayed()
    }

    @Test
    fun sliderRangeIsHalfKmToTenKmWithEighteenSteps() {
        show(SettingsUiState.forRadius(1.0))

        val range = rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(0.5f..10f, range.range)
        assertEquals(18, range.steps)
    }

    @Test
    fun sliderReachesHalfKmAndTenKm() {
        show(SettingsUiState.forRadius(1.0))
        val slider = rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))

        slider.performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0f) }
        slider.performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(100f) }

        assertEquals(listOf(0.5, 10.0), changes.takeLast(2))
    }

    @Test
    fun backButtonCallsOnBack() {
        show(SettingsUiState.forRadius(1.0))

        rule.onNodeWithContentDescription("Volver").performClick()

        assertEquals(1, backs)
    }
}
