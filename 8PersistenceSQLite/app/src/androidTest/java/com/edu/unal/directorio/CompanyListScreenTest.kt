package com.edu.unal.directorio

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag as tagNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.domain.Company
import com.edu.unal.directorio.ui.list.COMPANY_LIST_TAG
import com.edu.unal.directorio.ui.list.CompanyListResults
import com.edu.unal.directorio.ui.list.CompanyListScreen
import com.edu.unal.directorio.ui.list.CompanyListUiState
import com.edu.unal.directorio.ui.list.SEARCH_FIELD_TAG
import com.edu.unal.directorio.ui.list.chipTag
import com.edu.unal.directorio.ui.list.companyItemTag
import com.edu.unal.directorio.ui.theme.DirectorioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Screen tests of the list (task 5.2): state and callbacks are parameters. */
@RunWith(AndroidJUnit4::class)
class CompanyListScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun company(id: Long, name: String, c: Classification) = Company(
        id = id, name = name, email = "e$id@x.com", url = "", phone = "", products = "",
        classification = c,
    )

    private val companies = listOf(
        company(1, "Acme Software", Classification.SoftwareFactory),
        company(2, "Beta Labs", Classification.CustomDevelopment),
    )

    private var state by mutableStateOf(CompanyListUiState())
    private val events = mutableListOf<String>()

    private fun show(initial: CompanyListUiState) {
        state = initial
        rule.setContent {
            DirectorioTheme {
                CompanyListScreen(
                    state = state,
                    onQueryChange = { events += "query:$it"; state = state.copy(query = it) },
                    onClearQuery = { events += "clear"; state = state.copy(query = "") },
                    onFilterToggled = {
                        events += "toggle:${it.code}"
                        state = state.copy(
                            selected = if (it in state.selected) state.selected - it else state.selected + it,
                        )
                    },
                    onCompanyClick = { events += "open:$it" },
                    onAddClick = { events += "add" },
                )
            }
        }
    }

    private fun withCompanies() = CompanyListUiState(
        results = CompanyListResults(companies, hasAnyCompany = true, loaded = true),
    )

    @Test
    fun showsTitleSearchChipsAndItems() {
        show(withCompanies())
        rule.onNodeWithText("Directorio de empresas").assertIsDisplayed()
        rule.onNodeWithText("Buscar por nombre").assertIsDisplayed()
        Classification.entries.forEach { rule.onNodeWithTag(chipTag(it)).assertIsDisplayed() }
        rule.onNodeWithTag(companyItemTag(1)).assertIsDisplayed()
        rule.onNodeWithText("Acme Software").assertIsDisplayed()
        // Each item shows its single classification, with no comma-separated list.
        rule.onNodeWithTag(companyItemTag(1)).assertTextContains("Fábrica de software")
        rule.onNodeWithTag(companyItemTag(2)).assertTextContains("Desarrollo a la medida")
        rule.onAllNodesWithText(",", substring = true).assertCountEquals(0)
        rule.onNodeWithContentDescription("Agregar empresa").assertIsDisplayed()
    }

    @Test
    fun emptyDirectoryShowsOnlyItsMessageAndHint() {
        show(CompanyListUiState(results = CompanyListResults(loaded = true)))
        rule.onNodeWithText("Aún no hay empresas registradas").assertIsDisplayed()
        rule.onNodeWithText("Toca + para agregar la primera.").assertIsDisplayed()
        rule.onNodeWithText("Ninguna empresa coincide con la búsqueda").assertDoesNotExist()
    }

    @Test
    fun noMatchesShowsOnlyItsMessage() {
        show(CompanyListUiState(query = "zzzz", results = CompanyListResults(hasAnyCompany = true, loaded = true)))
        rule.onNodeWithText("Ninguna empresa coincide con la búsqueda").assertIsDisplayed()
        rule.onNodeWithText("Aún no hay empresas registradas").assertDoesNotExist()
        rule.onNodeWithText("Toca + para agregar la primera.").assertDoesNotExist()
    }

    @Test
    fun typingAndClearingTheSearch() {
        show(withCompanies())
        rule.onNodeWithContentDescription("Borrar búsqueda").assertDoesNotExist()
        rule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("acme")
        assertEquals(listOf("query:acme"), events)
        rule.onNodeWithContentDescription("Borrar búsqueda").assertIsDisplayed().performClick()
        assertEquals("clear", events.last())
        rule.onNodeWithContentDescription("Borrar búsqueda").assertDoesNotExist()
        assertEquals("", state.query)
    }

    @Test
    fun chipsAreMultiSelect() {
        show(withCompanies())
        val consulting = rule.onNodeWithTag(chipTag(Classification.Consulting))
        val factory = rule.onNodeWithTag(chipTag(Classification.SoftwareFactory))
        consulting.performClick()
        factory.performClick()
        consulting.assertIsSelected()
        factory.assertIsSelected()
        rule.onNodeWithTag(chipTag(Classification.CustomDevelopment)).assertIsNotSelected()
        assertEquals(setOf(Classification.Consulting, Classification.SoftwareFactory), state.selected)
    }

    @Test
    fun checkingAndUncheckingChips() {
        show(withCompanies())
        val chip = rule.onNodeWithTag(chipTag(Classification.Consulting))
        chip.assertIsNotSelected().performClick()
        chip.assertIsSelected()
        chip.performClick()
        chip.assertIsNotSelected()
        assertEquals(listOf("toggle:CONSULTING", "toggle:CONSULTING"), events)
    }

    @Test
    fun tappingACompanyAndTheAddButtonInvokeCallbacks() {
        show(withCompanies())
        rule.onNodeWithText("Beta Labs").performClick()
        rule.onNodeWithContentDescription("Agregar empresa").performClick()
        assertEquals(listOf("open:2", "add"), events)
    }

    @Test
    fun touchTargetsAreAtLeast48dp() {
        show(withCompanies().copy(query = "a"))
        val min = 48.dp
        listOf(
            rule.onNodeWithContentDescription("Agregar empresa"),
            rule.onNodeWithContentDescription("Borrar búsqueda"),
            rule.onNodeWithTag(chipTag(Classification.Consulting)),
            rule.onNodeWithTag(companyItemTag(1)),
        ).forEach {
            // touchBoundsInRoot includes the minimum interactive size Material adds around small controls.
            val b = it.fetchSemanticsNode().touchBoundsInRoot
            val density = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
            assertTrue("height ${b.height / density}", b.height / density >= min.value - 0.5f)
            assertTrue("width ${b.width / density}", b.width / density >= min.value - 0.5f)
        }
    }
}
