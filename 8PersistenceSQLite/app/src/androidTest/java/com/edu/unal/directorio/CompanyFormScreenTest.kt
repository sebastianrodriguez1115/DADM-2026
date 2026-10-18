package com.edu.unal.directorio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.domain.ClassificationError
import com.edu.unal.directorio.domain.EmailError
import com.edu.unal.directorio.domain.NameError
import com.edu.unal.directorio.domain.ValidationErrors
import com.edu.unal.directorio.ui.form.CompanyFormErrors
import com.edu.unal.directorio.ui.form.CompanyFormScreen
import com.edu.unal.directorio.ui.form.CompanyFormUiState
import com.edu.unal.directorio.ui.form.FORM_CANCEL_TAG
import com.edu.unal.directorio.ui.form.FORM_CLASSIFICATION_TAG
import com.edu.unal.directorio.ui.form.FORM_EMAIL_TAG
import com.edu.unal.directorio.ui.form.FORM_NAME_TAG
import com.edu.unal.directorio.ui.form.FORM_PRODUCTS_TAG
import com.edu.unal.directorio.ui.form.FORM_SAVE_TAG
import com.edu.unal.directorio.ui.form.formClassificationTag
import com.edu.unal.directorio.ui.theme.DirectorioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Screen tests of the form (task 6.3): state and callbacks are parameters. */
@RunWith(AndroidJUnit4::class)
class CompanyFormScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private var saves = 0
    private var cancels = 0

    private fun show(initial: CompanyFormUiState, boxHeight: Boolean = false) {
        rule.setContent {
            var state by remember { mutableStateOf(initial) }
            DirectorioTheme {
                val screen: @Composable () -> Unit = {
                    CompanyFormScreen(
                        state = state,
                        onNameChange = { state = state.copy(name = it) },
                        onEmailChange = { state = state.copy(email = it) },
                        onUrlChange = { state = state.copy(url = it) },
                        onPhoneChange = { state = state.copy(phone = it) },
                        onProductsChange = { state = state.copy(products = it) },
                        onClassificationChosen = { c -> state = state.copy(classification = c) },
                        onSave = { saves++ },
                        onCancel = { cancels++ },
                    )
                }
                if (boxHeight) {
                    Box(Modifier.height(280.dp)) { screen() }
                } else {
                    screen()
                }
            }
        }
    }

    @Test
    fun createTitle() {
        show(CompanyFormUiState())
        rule.onNodeWithText("Nueva empresa").assertIsDisplayed()
    }

    @Test
    fun editTitleAndValues() {
        show(
            CompanyFormUiState(
                isEditing = true,
                name = "Acme Software",
                classification = Classification.SoftwareFactory,
            ),
        )
        rule.onNodeWithText("Editar empresa").assertIsDisplayed()
        rule.onNodeWithTag(FORM_NAME_TAG).assertTextContains("Acme Software")
        rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).performScrollTo().assertTextContains("Fábrica de software")
    }

    @Test
    fun createFormHasNoClassificationSelected() {
        show(CompanyFormUiState())
        val field = rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).performScrollTo()
        field.assertIsDisplayed().assertTextContains("Clasificación")
        // Empty value: none of the three labels is shown while the menu is closed.
        listOf("Consultoría", "Desarrollo a la medida", "Fábrica de software").forEach {
            rule.onNodeWithText(it).assertDoesNotExist()
        }
    }

    @Test
    fun showsAllLabelsAndSection() {
        show(CompanyFormUiState())
        listOf(
            "Nombre de la empresa", "Correo electrónico", "Sitio web (opcional)",
            "Teléfono (opcional)", "Productos y servicios (opcional)",
        ).forEach { rule.onNodeWithText(it).assertExists() }
        rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun menuOffersTheThreeOptions() {
        show(CompanyFormUiState())
        rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).performScrollTo().performClick()
        listOf("Consultoría", "Desarrollo a la medida", "Fábrica de software").forEach {
            rule.onNodeWithText(it).assertIsDisplayed()
        }
        Classification.entries.forEach {
            rule.onNodeWithTag(formClassificationTag(it)).assertIsDisplayed()
        }
    }

    @Test
    fun errorsAppearBelowTheirField() {
        show(
            CompanyFormUiState(
                errors = CompanyFormErrors(
                    validation = ValidationErrors(
                        name = NameError.Empty,
                        email = EmailError.Empty,
                        classification = ClassificationError.NoneSelected,
                    ),
                ),
            ),
        )
        val nameBounds = rule.onNodeWithTag(FORM_NAME_TAG).fetchSemanticsNode().boundsInRoot
        val emailBounds = rule.onNodeWithTag(FORM_EMAIL_TAG).fetchSemanticsNode().boundsInRoot
        val nameError = rule.onNodeWithText("Escribe el nombre de la empresa").fetchSemanticsNode().boundsInRoot
        val emailError = rule.onNodeWithText("Escribe el correo electrónico").fetchSemanticsNode().boundsInRoot
        assertTrue(nameError.top >= nameBounds.top && nameError.bottom <= emailBounds.top + 1f)
        assertTrue(emailError.top >= emailBounds.top && emailError.top < emailBounds.bottom + 1f)
        rule.onNodeWithText("Selecciona una clasificación").performScrollTo().assertIsDisplayed()
        // The error is under the menu field.
        val menuBounds = rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).fetchSemanticsNode().boundsInRoot
        val classError = rule.onNodeWithText("Selecciona una clasificación").fetchSemanticsNode().boundsInRoot
        assertTrue(classError.top >= menuBounds.top && classError.top < menuBounds.bottom + 1f)
    }

    @Test
    fun takenErrorsHaveTheirOwnText() {
        show(CompanyFormUiState(errors = CompanyFormErrors(nameTaken = true, emailTaken = true)))
        rule.onNodeWithText("Ya existe una empresa con ese nombre").assertIsDisplayed()
        rule.onNodeWithText("Ya existe una empresa con ese correo electrónico").assertExists()
    }

    @Test
    fun choosingAnOptionClosesTheMenuAndShowsIt() {
        show(CompanyFormUiState())
        rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).performScrollTo().performClick()
        rule.onNodeWithTag(formClassificationTag(Classification.Consulting)).performClick()

        // The menu closed: the other options are gone and the field shows the choice.
        rule.onNodeWithTag(formClassificationTag(Classification.CustomDevelopment)).assertDoesNotExist()
        rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).assertTextContains("Consultoría")

        // It can be changed to another of the three options.
        rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).performClick()
        rule.onNodeWithTag(formClassificationTag(Classification.CustomDevelopment)).performClick()
        rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).assertTextContains("Desarrollo a la medida")
    }

    @Test
    fun classificationFieldAcceptsNoFreeText() {
        show(CompanyFormUiState())
        rule.onNodeWithTag(FORM_CLASSIFICATION_TAG).performScrollTo()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
    }

    @Test
    fun thereAreNoCheckboxes() {
        show(CompanyFormUiState())
        rule.onAllNodes(isToggleable()).assertCountEquals(0)
    }

    @Test
    fun productsAcceptMultipleLines() {
        show(CompanyFormUiState())
        rule.onNodeWithTag(FORM_PRODUCTS_TAG).performScrollTo().performTextInput("uno\ndos\ntres")
        rule.onNodeWithTag(FORM_PRODUCTS_TAG).assertTextContains("uno\ndos\ntres")
    }

    @Test
    fun saveAndCancelInvokeCallbacks() {
        show(CompanyFormUiState())
        rule.onNodeWithTag(FORM_SAVE_TAG).performScrollTo().performClick()
        rule.onNodeWithTag(FORM_CANCEL_TAG).performScrollTo().performClick()
        assertEquals(1, saves)
        assertEquals(1, cancels)
    }

    @Test
    fun saveIsDisabledWhileSaving() {
        show(CompanyFormUiState(isSaving = true))
        rule.onNodeWithTag(FORM_SAVE_TAG).performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag(FORM_CANCEL_TAG).assertIsEnabled()
    }

    @Test
    fun saveFailureMessageIsVisible() {
        show(CompanyFormUiState(saveFailed = true))
        rule.onNodeWithText("No se pudo guardar la empresa. Inténtalo de nuevo.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun smallScreenCanScrollToSave() {
        show(CompanyFormUiState(), boxHeight = true)
        // With 280 dp the button starts out of view; scrolling brings it in.
        rule.onNodeWithTag(FORM_SAVE_TAG).performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, saves)
    }
}
