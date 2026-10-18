package com.edu.unal.directorio

import androidx.compose.ui.test.assertCountEquals
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
import com.edu.unal.directorio.ui.detail.DELETE_DIALOG_CANCEL_TAG
import com.edu.unal.directorio.ui.detail.DELETE_DIALOG_CONFIRM_TAG
import com.edu.unal.directorio.ui.detail.DETAIL_DELETE_TAG
import com.edu.unal.directorio.ui.detail.DETAIL_EDIT_TAG
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.ui.form.FORM_CLASSIFICATION_TAG
import com.edu.unal.directorio.ui.form.FORM_PHONE_TAG
import com.edu.unal.directorio.ui.form.formClassificationTag
import com.edu.unal.directorio.ui.form.FORM_SAVE_TAG
import com.edu.unal.directorio.ui.list.SEARCH_FIELD_TAG
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Flow test of detail, delete and edit on the real app. Starts with the 8 SAMPLE companies (design D10). */
@RunWith(AndroidJUnit4::class)
class DirectorioDetailFlowTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

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

    private fun waitForList() =
        rule.waitUntil(5_000) { rule.onAllNodesWithTag(SEARCH_FIELD_TAG).fetchSemanticsNodes().isNotEmpty() }

    private fun openDetailOf(name: String) {
        waitForText(name)
        rule.onNodeWithText(name).performClick()
        waitForText("Detalle de empresa")
    }

    private fun isInList(name: String) = rule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun cancellingTheDeletionKeepsTheCompanyInTheList() {
        openDetailOf("Bosque Digital")
        rule.onNodeWithTag(DETAIL_DELETE_TAG).performClick()
        waitForText("¿Eliminar empresa?")
        rule.onNodeWithText("Se eliminará «Bosque Digital» de forma permanente.").assertIsDisplayed()
        rule.onNodeWithTag(DELETE_DIALOG_CANCEL_TAG).performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("¿Eliminar empresa?").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("Detalle de empresa").assertIsDisplayed()

        rule.onNodeWithContentDescription("Volver").performClick()
        waitForList()
        waitForText("Bosque Digital")
        assertTrue(isInList("Bosque Digital"))
    }

    @Test
    fun confirmingTheDeletionReturnsToTheListWithoutTheCompany() {
        openDetailOf("Delta Soluciones")
        rule.onNodeWithTag(DETAIL_DELETE_TAG).performClick()
        rule.onNodeWithTag(DELETE_DIALOG_CONFIRM_TAG).performClick()

        waitForList()
        waitForText("Zafiro Labs")
        assertTrue(!isInList("Delta Soluciones"))
        assertTrue(isInList("Andes Consultores"))
    }

    @Test
    fun theDetailShowsTheSingleClassification() {
        openDetailOf("Cumbre Software")
        rule.onNodeWithText("Clasificación").assertIsDisplayed()
        rule.onNodeWithText("Fábrica de software").assertIsDisplayed()
        rule.onAllNodesWithText("Consultoría").assertCountEquals(0)
        rule.onAllNodesWithText("Desarrollo a la medida").assertCountEquals(0)
        rule.onAllNodesWithText(",", substring = true).assertCountEquals(0)
    }

    @Test
    fun editingTheClassificationFromTheMenuShowsItInTheDetail() {
        openDetailOf("Cumbre Software")
        rule.onNodeWithTag(DETAIL_EDIT_TAG).performClick()
        waitForText("Editar empresa")
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Cumbre Software").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).performScrollTo().assertTextContains("Fábrica de software")
        rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).performClick()
        rule.onNodeWithTag(formClassificationTag(Classification.Consulting)).performClick()
        rule.onNodeWithTag(FORM_SAVE_TAG).performScrollTo().performClick()

        waitForText("Detalle de empresa")
        waitForText("Consultoría")
        rule.onAllNodesWithText("Fábrica de software").assertCountEquals(0)
    }

    @Test
    fun editingThePhoneShowsTheNewValueInTheDetail() {
        openDetailOf("Andes Consultores")
        rule.onNodeWithTag(DETAIL_EDIT_TAG).performClick()
        waitForText("Editar empresa")
        rule.waitUntil(5_000) { rule.onAllNodesWithText("+57 300 555-0101").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag(FORM_PHONE_TAG).performScrollTo().performTextClearance()
        rule.onNodeWithTag(FORM_PHONE_TAG).performTextInput("+57 310 555-0199")
        rule.onNodeWithTag(FORM_SAVE_TAG).performScrollTo().performClick()

        waitForText("Detalle de empresa")
        waitForText("+57 310 555-0199")
        assertTrue(!isInList("+57 300 555-0101"))
    }

    @Test
    fun backFromTheDetailReturnsToTheList() {
        openDetailOf("Zafiro Labs")
        Espresso.pressBack()
        waitForList()
        waitForText("Zafiro Labs")
    }
}
