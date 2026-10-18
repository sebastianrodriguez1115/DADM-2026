package com.edu.unal.directorio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.domain.Company
import com.edu.unal.directorio.ui.detail.CompanyDetailContent
import com.edu.unal.directorio.ui.detail.CompanyDetailScreen
import com.edu.unal.directorio.ui.detail.CompanyDetailUiState
import com.edu.unal.directorio.ui.detail.DELETE_DIALOG_CANCEL_TAG
import com.edu.unal.directorio.ui.detail.DELETE_DIALOG_CONFIRM_TAG
import com.edu.unal.directorio.ui.detail.DETAIL_DELETE_TAG
import com.edu.unal.directorio.ui.detail.DETAIL_EDIT_TAG
import com.edu.unal.directorio.ui.theme.DirectorioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Screen tests of the detail and the delete dialog (task 7.2): state and callbacks are parameters. */
@RunWith(AndroidJUnit4::class)
class CompanyDetailScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private var edits = 0
    private var backs = 0
    private var deletes = 0

    private val full = Company(
        id = 1,
        name = "Acme Software",
        email = "ventas@acme.example.com",
        url = "https://acme.example.com",
        phone = "601 555 0123",
        products = "Linea uno\nLinea dos\nLinea tres",
        classification = Classification.SoftwareFactory,
    )

    /** The screen owns the dialog flag like the ViewModel would; [deletes] counts real deletions. */
    private fun show(company: Company, failed: Boolean = false) {
        rule.setContent {
            var dialog by remember { mutableStateOf(false) }
            DirectorioTheme {
                CompanyDetailScreen(
                    state = CompanyDetailUiState(
                        content = CompanyDetailContent.Loaded(company),
                        deleteDialogVisible = dialog,
                        deleteFailed = failed,
                    ),
                    onBack = { backs++ },
                    onEdit = { edits++ },
                    onDelete = { dialog = true },
                    onConfirmDelete = { deletes++; dialog = false },
                    onDismissDelete = { dialog = false },
                )
            }
        }
    }

    private fun dialogNode(text: String) =
        rule.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    @Test
    fun showsTheCompleteCompany() {
        show(full)
        rule.onNodeWithText("Detalle de empresa").assertIsDisplayed()
        rule.onNodeWithContentDescription("Volver").assertIsDisplayed()
        rule.onNodeWithText("Acme Software").assertIsDisplayed()
        rule.onNodeWithText("Clasificación").assertIsDisplayed()
        // Only the single classification is visible, with no comma-separated list.
        rule.onNodeWithText("Fábrica de software").assertIsDisplayed()
        rule.onNodeWithText("Consultoría").assertDoesNotExist()
        rule.onAllNodesWithText(",", substring = true).assertCountEquals(0)
        rule.onNodeWithText("Correo electrónico").assertIsDisplayed()
        rule.onNodeWithText("ventas@acme.example.com").assertIsDisplayed()
        rule.onNodeWithText("Sitio web").assertIsDisplayed()
        rule.onNodeWithText("https://acme.example.com").assertIsDisplayed()
        rule.onNodeWithText("Teléfono").assertIsDisplayed()
        rule.onNodeWithText("601 555 0123").assertIsDisplayed()
        rule.onNodeWithText("Productos y servicios").assertIsDisplayed()
        rule.onNodeWithTag(DETAIL_EDIT_TAG).assertIsDisplayed()
        rule.onNodeWithTag(DETAIL_DELETE_TAG).assertIsDisplayed()
        rule.onAllNodesWithText("No indicado").assertCountEquals(0)
    }

    @Test
    fun emptyOptionalFieldsShowNotProvided() {
        show(full.copy(url = "", phone = "", products = ""))
        rule.onAllNodesWithText("No indicado").assertCountEquals(3)
    }

    @Test
    fun productsKeepTheirThreeLines() {
        show(full)
        rule.onNodeWithText("Linea uno\nLinea dos\nLinea tres").assertIsDisplayed()
    }

    @Test
    fun backAndEditInvokeTheirCallbacks() {
        show(full)
        rule.onNodeWithContentDescription("Volver").performClick()
        rule.onNodeWithTag(DETAIL_EDIT_TAG).performClick()
        assertEquals(1, backs)
        assertEquals(1, edits)
    }

    @Test
    fun deleteOpensTheDialogWithTheCompanyNameAndDeletesNothing() {
        show(full)
        rule.onNodeWithTag(DETAIL_DELETE_TAG).performClick()
        dialogNode("¿Eliminar empresa?").assertIsDisplayed()
        dialogNode("Se eliminará «Acme Software» de forma permanente.").assertIsDisplayed()
        rule.onNodeWithTag(DELETE_DIALOG_CANCEL_TAG).assertIsDisplayed()
        rule.onNodeWithTag(DELETE_DIALOG_CONFIRM_TAG).assertIsDisplayed()
        assertEquals(0, deletes)
    }

    @Test
    fun cancelClosesTheDialogWithoutDeleting() {
        show(full)
        rule.onNodeWithTag(DETAIL_DELETE_TAG).performClick()
        rule.onNodeWithTag(DELETE_DIALOG_CANCEL_TAG).performClick()
        rule.onAllNodesWithText("¿Eliminar empresa?").assertCountEquals(0)
        rule.onNodeWithText("Acme Software").assertIsDisplayed()
        assertEquals(0, deletes)
    }

    @Test
    fun backClosesTheDialogWithoutDeleting() {
        show(full)
        rule.onNodeWithTag(DETAIL_DELETE_TAG).performClick()
        rule.onNodeWithTag(DELETE_DIALOG_CONFIRM_TAG).assertIsDisplayed()
        Espresso.pressBack()
        rule.waitForIdle()
        rule.onAllNodesWithText("¿Eliminar empresa?").assertCountEquals(0)
        assertEquals(0, deletes)
        assertEquals(0, backs)
    }

    @Test
    fun confirmInTheDialogInvokesTheDeletion() {
        show(full)
        rule.onNodeWithTag(DETAIL_DELETE_TAG).performClick()
        rule.onNodeWithTag(DELETE_DIALOG_CONFIRM_TAG).performClick()
        assertEquals(1, deletes)
    }

    @Test
    fun showsTheFailureMessage() {
        show(full, failed = true)
        rule.onNodeWithText("No se pudo eliminar la empresa. Inténtalo de nuevo.").assertIsDisplayed()
    }
}
