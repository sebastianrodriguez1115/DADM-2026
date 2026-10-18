package com.edu.unal.directorio

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.ui.list.COMPANY_LIST_TAG
import com.edu.unal.directorio.ui.list.SEARCH_FIELD_TAG
import com.edu.unal.directorio.ui.list.chipTag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Flow test of the list on the real app. Starts with the 8 sample companies (design D10). */
@RunWith(AndroidJUnit4::class)
class DirectorioListFlowTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    private val samples = listOf(
        "Andes Consultores", "Bosque Digital", "Cumbre Software", "Delta Soluciones",
        "Horizonte Ágil", "Lumbre Sistemas", "Ñandú Tecnología", "Zafiro Labs",
    )

    @Before
    fun setUp() {
        resetAppDatabase()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun visibleNames(): List<String> =
        samples.filter { name ->
            rule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }

    @Test
    fun showsTheEightSamplesInOrder() {
        rule.onNodeWithText("Directorio de empresas").assertIsDisplayed()
        rule.waitUntil(5_000) { rule.onAllNodesWithText(samples[0]).fetchSemanticsNodes().isNotEmpty() }
        val tops = samples.map { name ->
            rule.onNodeWithText(name).fetchSemanticsNode().boundsInRoot.top
        }
        assertEquals(tops.sorted(), tops)
        assertEquals(8, tops.distinct().size)
    }

    @Test
    fun filterIsKeptAfterOpeningTheDetailAndGoingBack() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText(samples[0]).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("a")
        rule.onNodeWithTag(chipTag(Classification.Consulting)).performClick()
        rule.waitForIdle()
        val before = visibleNames()
        assertTrue(before.isNotEmpty() && before.size < 8)

        rule.onNodeWithText(before.first()).performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Detalle de empresa").fetchSemanticsNodes().isNotEmpty() }
        Espresso.pressBack()

        rule.waitUntil(5_000) { rule.onAllNodesWithTag(SEARCH_FIELD_TAG).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag(SEARCH_FIELD_TAG).assertTextContains("a")
        rule.onNodeWithTag(chipTag(Classification.Consulting)).assertIsSelected()
        rule.waitUntil(5_000) { visibleNames() == before }
        assertEquals(before, visibleNames())
    }

    private fun assertTrue(c: Boolean) = org.junit.Assert.assertTrue(c)
}
