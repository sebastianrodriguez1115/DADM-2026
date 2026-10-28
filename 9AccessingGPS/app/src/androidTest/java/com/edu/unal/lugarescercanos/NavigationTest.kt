package com.edu.unal.lugarescercanos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** El engranaje del mapa abre Ajustes y Back vuelve al mapa. */
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun gearOpensSettingsAndBackReturnsToMap() {
        rule.onNodeWithText("Lugares cercanos").assertIsDisplayed()

        rule.onNodeWithContentDescription("Ajustes").performClick()
        rule.onNodeWithContentDescription("Volver").assertIsDisplayed()
        rule.onNodeWithText("Radio de búsqueda").assertIsDisplayed()

        Espresso.pressBack()

        rule.onNodeWithText("Lugares cercanos").assertIsDisplayed()
        rule.onNodeWithContentDescription("Ajustes").assertIsDisplayed()
    }
}
