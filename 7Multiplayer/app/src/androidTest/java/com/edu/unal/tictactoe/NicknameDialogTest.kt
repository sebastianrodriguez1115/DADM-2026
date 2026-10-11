package com.edu.unal.tictactoe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.ui.online.lobby.NicknameDialog
import com.edu.unal.tictactoe.ui.online.lobby.NicknameDialogTags
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NicknameDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewport = FixedViewport(composeTestRule)

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val accepted = mutableListOf<String>()
    @Volatile
    private var dismissals = 0
    private var rejected by mutableStateOf(false)
    private var keyboard: SoftwareKeyboardController? = null

    // Typing opens the soft keyboard, which outlives the test and would take the next Back press.
    @After
    fun hideKeyboard() {
        composeTestRule.runOnUiThread { keyboard?.hide() }
        composeTestRule.waitForIdle()
    }

    private fun show(
        nickname: String? = null,
        isRejected: Boolean = false,
        width: Int = 360,
        height: Int = 640,
        fontScale: Float = 1f,
    ) {
        rejected = isRejected
        viewport.setContent(width, height, fontScale) {
            keyboard = LocalSoftwareKeyboardController.current
            TicTacToeTheme(dynamicColor = false) {
                NicknameDialog(
                    nickname = nickname,
                    isRejected = rejected,
                    onAccept = { accepted += it },
                    onDismiss = { dismissals++ },
                )
            }
        }
    }

    private val fieldText: String
        get() = composeTestRule.onNodeWithTag(NicknameDialogTags.FIELD)
            .fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()

    @Test
    fun firstTimeShowsTitleLabelActionsAndAnEmptyField() {
        show()

        assertEquals("Tu apodo", context.getString(R.string.nickname_dialog_title))
        composeTestRule.onNodeWithText(context.getString(R.string.nickname_dialog_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.nickname_field_label)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.about_accept)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.lobby_cancel)).assertIsDisplayed()
        assertEquals("", fieldText)
        composeTestRule.onAllNodesWithText(context.getString(R.string.nickname_error)).assertCountEquals(0)
    }

    @Test
    fun changeNicknamePreloadsTheCurrentNickname() {
        show(nickname = "Ana")

        assertEquals("Ana", fieldText)
    }

    @Test
    fun rejectionShowsTheErrorAndKeepsTheDialogOpen() {
        show(nickname = "Ana", isRejected = true)

        composeTestRule.onNodeWithText(context.getString(R.string.nickname_error)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NicknameDialogTags.DIALOG).assertIsDisplayed()
        assertEquals("Escribe un apodo de 1 a 20 caracteres", context.getString(R.string.nickname_error))
        assertEquals(0, dismissals)
        assertTrue(accepted.isEmpty())
    }

    @Test
    fun theErrorAppearsWhenTheStateTurnsRejected() {
        show()
        composeTestRule.onAllNodesWithText(context.getString(R.string.nickname_error)).assertCountEquals(0)

        composeTestRule.runOnIdle { rejected = true }

        composeTestRule.onNodeWithText(context.getString(R.string.nickname_error)).assertIsDisplayed()
    }

    @Test
    fun acceptDeliversTheTextExactlyAsTyped() {
        show()

        composeTestRule.onNodeWithTag(NicknameDialogTags.FIELD).performTextInput("  Ana  ")
        composeTestRule.onNodeWithTag(NicknameDialogTags.ACCEPT).performClick()

        assertEquals(listOf("  Ana  "), accepted)
        assertEquals(0, dismissals)
    }

    @Test
    fun acceptDeliversAnEmptyFieldToo() {
        show(nickname = "Ana")

        composeTestRule.onNodeWithTag(NicknameDialogTags.FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(NicknameDialogTags.ACCEPT).performClick()

        assertEquals(listOf(""), accepted)
    }

    @Test
    fun twentyOneCharactersCanBeTypedAndAccepted() {
        val text = "a".repeat(21)
        show()

        composeTestRule.onNodeWithTag(NicknameDialogTags.FIELD).performTextInput(text)
        assertEquals(text, fieldText)
        composeTestRule.onNodeWithTag(NicknameDialogTags.ACCEPT).performClick()

        assertEquals(listOf(text), accepted)
    }

    @Test
    fun cancelInvokesTheDismissCallback() {
        show()

        composeTestRule.onNodeWithTag(NicknameDialogTags.CANCEL).performClick()

        assertEquals(1, dismissals)
        assertTrue(accepted.isEmpty())
    }

    @Test
    fun backInvokesTheDismissCallback() {
        show()

        composeTestRule.onNodeWithTag(NicknameDialogTags.DIALOG).assertIsDisplayed()
        composeTestRule.waitForIdle()
        pressBackAsUser()
        // The global action returns before the app has handled the key.
        composeTestRule.waitUntil(timeoutMillis = 5_000) { dismissals > 0 }

        assertEquals(1, dismissals)
        assertTrue(accepted.isEmpty())
    }

    @Test
    fun theTextBeingEditedSurvivesRestoration() {
        val restoration = StateRestorationTester(composeTestRule)
        restoration.setContent {
            keyboard = LocalSoftwareKeyboardController.current
            TicTacToeTheme(dynamicColor = false) {
                NicknameDialog(
                    nickname = "Ana",
                    isRejected = false,
                    onAccept = {},
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onNodeWithTag(NicknameDialogTags.FIELD).performTextInput("Luis")
        assertEquals("AnaLuis", fieldText)

        restoration.emulateSavedInstanceStateRestore()

        assertEquals("AnaLuis", fieldText)
    }

    @Test
    fun actionsMeasureAtLeast48DpAndExposeTheirResourceLabel() {
        show()

        listOf(
            NicknameDialogTags.ACCEPT to R.string.about_accept,
            NicknameDialogTags.CANCEL to R.string.lobby_cancel,
        ).forEach { (tag, label) ->
            val node = composeTestRule.onNodeWithTag(tag)
            node.assert(hasText(context.getString(label)))
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue("$tag is shorter than 48 dp", bounds.bottom - bounds.top >= 48.dp)
            assertTrue("$tag is narrower than 48 dp", bounds.right - bounds.left >= 48.dp)
        }
    }

    @Test
    fun landscapeWithLargeFontKeepsEverythingOperable() {
        show(isRejected = true, width = 640, height = 360, fontScale = 2f)

        composeTestRule.onNodeWithTag(NicknameDialogTags.FIELD).performScrollTo().performTextInput("Ana")
        composeTestRule.onNodeWithText(context.getString(R.string.nickname_error)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(NicknameDialogTags.CANCEL).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NicknameDialogTags.ACCEPT).assertIsDisplayed().performClick()

        assertEquals(listOf("Ana"), accepted)
    }
}
