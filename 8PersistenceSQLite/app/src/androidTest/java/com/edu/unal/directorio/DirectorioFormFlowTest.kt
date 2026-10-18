package com.edu.unal.directorio

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.ui.form.FORM_EMAIL_TAG
import com.edu.unal.directorio.ui.form.FORM_NAME_TAG
import com.edu.unal.directorio.ui.form.FORM_SAVE_TAG
import com.edu.unal.directorio.ui.form.formClassificationTag
import com.edu.unal.directorio.ui.list.SEARCH_FIELD_TAG
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Flow test of the create form on the real app. Starts with an EMPTY database (design D10). */
@RunWith(AndroidJUnit4::class)
class DirectorioFormFlowTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        resetAppDatabaseEmpty()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun waitForText(text: String) =
        rule.waitUntil(5_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun openCreateForm() {
        waitForText("Aún no hay empresas registradas")
        rule.onNodeWithContentDescription("Agregar empresa").performClick()
        waitForText("Nueva empresa")
    }

    @Test
    fun createsACompanyAndSeesItInTheList() {
        openCreateForm()
        rule.onNodeWithTag(FORM_NAME_TAG).performTextInput("Nueva Empresa SAS")
        rule.onNodeWithTag(FORM_EMAIL_TAG).performTextInput("info@nueva.example.com")
        rule.onNodeWithTag(formClassificationTag(Classification.CustomDevelopment))
            .performScrollTo().performClick()
        rule.onNodeWithTag(FORM_SAVE_TAG).performScrollTo().performClick()

        rule.waitUntil(5_000) { rule.onAllNodesWithTag(SEARCH_FIELD_TAG).fetchSemanticsNodes().isNotEmpty() }
        waitForText("Nueva Empresa SAS")
        // Two matches: the filter chip and the classification line of the new list item.
        rule.onAllNodesWithText("Desarrollo a la medida").assertCountEquals(2)
    }

    @Test
    fun backFromTheCreateFormReturnsToTheListWithoutCreating() {
        openCreateForm()
        rule.onNodeWithTag(FORM_NAME_TAG).performTextInput("No se guarda")
        rule.onNodeWithTag(FORM_EMAIL_TAG).performTextInput("no@guarda.example.com")
        // With the keyboard open the first Back only closes it, so close it first.
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()

        rule.waitUntil(5_000) { rule.onAllNodesWithTag(SEARCH_FIELD_TAG).fetchSemanticsNodes().isNotEmpty() }
        waitForText("Aún no hay empresas registradas")
        assert(rule.onAllNodesWithText("No se guarda").fetchSemanticsNodes().isEmpty())
    }
}
