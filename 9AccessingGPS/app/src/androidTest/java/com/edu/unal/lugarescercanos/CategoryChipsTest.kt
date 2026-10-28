package com.edu.unal.lugarescercanos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.lugarescercanos.domain.PlaceCategory
import com.edu.unal.lugarescercanos.ui.map.CategoryChips
import com.edu.unal.lugarescercanos.ui.theme.LugaresCercanosTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Selector de categoría: selección única y la categoría inicial de una pantalla nueva. */
@RunWith(AndroidJUnit4::class)
class CategoryChipsTest {
    @get:Rule
    val rule = createComposeRule()

    /** Pantalla de prueba: guarda la categoría elegida solo en memoria, como el ViewModel. */
    @Composable
    private fun Host() {
        var selected by remember { mutableStateOf(PlaceCategory.DEFAULT) }
        CategoryChips(selected = selected, onSelect = { selected = it })
    }

    @Test
    fun choosingRestaurantsLeavesOnlyItSelected() {
        rule.setContent { LugaresCercanosTheme { Host() } }

        rule.onNodeWithText("Restaurantes").performScrollTo().performClick()

        rule.onNodeWithText("Restaurantes").assertIsSelected()
        rule.onNodeWithText("Hospitales y clínicas").assertIsNotSelected()
        rule.onNodeWithText("Farmacias").assertIsNotSelected()
        rule.onNodeWithText("Sitios turísticos").assertIsNotSelected()
        rule.onNodeWithText("Parques").assertIsNotSelected()
    }

    @Test
    fun newScreenStartsWithHospitalsAndClinicsSelected() {
        rule.setContent { LugaresCercanosTheme { Host() } }

        rule.onNodeWithText("Hospitales y clínicas").assertIsSelected()
        rule.onNodeWithText("Restaurantes").assertIsNotSelected()
    }

    @Test
    fun categoriesShowTheSpecNames() {
        rule.setContent {
            LugaresCercanosTheme {
                CategoryChips(selected = PlaceCategory.DEFAULT, onSelect = {})
            }
        }

        listOf("Hospitales y clínicas", "Farmacias", "Restaurantes", "Sitios turísticos", "Parques")
            .forEach { rule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
    }
}
