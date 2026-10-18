package com.edu.unal.directorio

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.ui.detail.DELETE_DIALOG_CANCEL_TAG
import com.edu.unal.directorio.ui.detail.DELETE_DIALOG_CONFIRM_TAG
import com.edu.unal.directorio.ui.detail.DETAIL_DELETE_TAG
import com.edu.unal.directorio.ui.detail.DETAIL_EDIT_TAG
import com.edu.unal.directorio.ui.form.FORM_CLASSIFICATION_TAG
import com.edu.unal.directorio.ui.form.FORM_EMAIL_TAG
import com.edu.unal.directorio.ui.form.FORM_NAME_TAG
import com.edu.unal.directorio.ui.form.FORM_PHONE_TAG
import com.edu.unal.directorio.ui.form.FORM_SAVE_TAG
import com.edu.unal.directorio.ui.form.formClassificationTag
import com.edu.unal.directorio.ui.list.SEARCH_FIELD_TAG
import com.edu.unal.directorio.ui.list.chipTag
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Full journey on the real app. Starts with the 8 SAMPLE companies (design D10). */
@RunWith(AndroidJUnit4::class)
class DirectorioEndToEndTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    private val samples = listOf(
        "Andes Consultores", "Bosque Digital", "Cumbre Software", "Delta Soluciones",
        "Horizonte Ágil", "Lumbre Sistemas", "Ñandú Tecnología", "Zafiro Labs",
    )
    private val newName = "Galápagos Ingeniería"

    @Before
    fun setUp() {
        resetAppDatabase()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun waitForText(text: String) =
        rule.waitUntil(5_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForNoText(text: String) =
        rule.waitUntil(5_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }

    private fun waitForList() =
        rule.waitUntil(5_000) { rule.onAllNodesWithTag(SEARCH_FIELD_TAG).fetchSemanticsNodes().isNotEmpty() }

    private fun isInList(name: String) = rule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()

    /** Opens the "Clasificación" menu of the form and picks [classification]. */
    private fun chooseClassification(classification: Classification) {
        rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).performScrollTo().performClick()
        rule.onNodeWithTag(formClassificationTag(classification)).performClick()
    }

    @Test
    fun fullJourneyCreateSearchFilterEditAndDelete() {
        // 1. The 8 sample companies are listed.
        waitForList()
        waitForText("Zafiro Labs")
        rule.onNodeWithText("Directorio de empresas").assertIsDisplayed()
        samples.forEach { assertTrue("Missing sample $it", isInList(it)) }

        // 2. Create a company with a single classification chosen in the menu and an accented name.
        rule.onNodeWithContentDescription("Agregar empresa").performClick()
        waitForText("Nueva empresa")
        rule.onNodeWithTag(FORM_NAME_TAG).performTextInput(newName)
        rule.onNodeWithTag(FORM_EMAIL_TAG).performTextInput("contacto@galapagos.example.com")
        rule.onNodeWithTag(FORM_PHONE_TAG).performScrollTo().performTextInput("+57 300 555-0000")
        chooseClassification(Classification.SoftwareFactory)
        rule.onNodeWithTag(FORM_SAVE_TAG).performScrollTo().performClick()
        waitForList()
        waitForText(newName)
        samples.forEach { assertTrue(isInList(it)) }

        // 3. Search in lowercase and without accent: only the new company remains.
        rule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("galapagos")
        waitForNoText("Zafiro Labs")
        assertTrue(isInList(newName))
        assertTrue(!isInList("Andes Consultores"))

        // 4. Clear the search, then filter by several chips (union of the marked ones). The new
        // company is SoftwareFactory: absent with only Consulting marked, present once it is marked.
        rule.onNodeWithContentDescription("Borrar búsqueda").performClick()
        waitForText("Zafiro Labs")
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag(chipTag(Classification.Consulting)).performClick()
        waitForNoText("Ñandú Tecnología")
        assertTrue(isInList("Andes Consultores"))
        assertTrue(!isInList("Delta Soluciones"))
        assertTrue(!isInList(newName))
        rule.onNodeWithTag(chipTag(Classification.SoftwareFactory)).performClick()
        waitForText("Delta Soluciones")
        assertTrue(isInList(newName))
        assertTrue(isInList("Andes Consultores"))
        assertTrue(!isInList("Ñandú Tecnología"))
        // Unmark both chips to see everything again.
        rule.onNodeWithTag(chipTag(Classification.Consulting)).performClick()
        rule.onNodeWithTag(chipTag(Classification.SoftwareFactory)).performClick()
        waitForText("Ñandú Tecnología")

        // 5. Open the detail (it shows the single classification) and edit the phone and the
        // classification.
        rule.onNodeWithText(newName).performClick()
        waitForText("Detalle de empresa")
        waitForText("+57 300 555-0000")
        rule.onNodeWithText("Fábrica de software").assertIsDisplayed()
        rule.onNodeWithTag(DETAIL_EDIT_TAG).performClick()
        waitForText("Editar empresa")
        rule.waitUntil(5_000) { rule.onAllNodesWithText("+57 300 555-0000").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag(FORM_PHONE_TAG).performScrollTo().performTextClearance()
        rule.onNodeWithTag(FORM_PHONE_TAG).performTextInput("+57 311 555-0123")
        rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).performScrollTo().assertTextContains("Fábrica de software")
        chooseClassification(Classification.CustomDevelopment)
        rule.onNodeWithTag(FORM_SAVE_TAG).performScrollTo().performClick()
        waitForText("Detalle de empresa")
        waitForText("+57 311 555-0123")
        assertTrue(!isInList("+57 300 555-0000"))
        waitForText("Desarrollo a la medida")
        assertTrue(!isInList("Fábrica de software"))

        // 6. Cancel a deletion: the company stays.
        rule.onNodeWithTag(DETAIL_DELETE_TAG).performClick()
        waitForText("¿Eliminar empresa?")
        rule.onNodeWithText("Se eliminará «$newName» de forma permanente.").assertIsDisplayed()
        rule.onNodeWithTag(DELETE_DIALOG_CANCEL_TAG).performClick()
        waitForNoText("¿Eliminar empresa?")
        rule.onNodeWithText("Detalle de empresa").assertIsDisplayed()

        // 7. Delete it.
        rule.onNodeWithTag(DETAIL_DELETE_TAG).performClick()
        waitForText("¿Eliminar empresa?")
        rule.onNodeWithTag(DELETE_DIALOG_CONFIRM_TAG).performClick()

        // 8. Back in the list: the new company is gone and the 8 samples remain.
        waitForList()
        waitForText("Zafiro Labs")
        waitForNoText(newName)
        samples.forEach { assertTrue("Missing sample $it", isInList(it)) }
    }
}
