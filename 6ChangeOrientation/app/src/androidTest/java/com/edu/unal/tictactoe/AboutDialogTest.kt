package com.edu.unal.tictactoe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.ui.game.AboutDialog
import com.edu.unal.tictactoe.ui.game.AboutDialogTags
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun showsTitleImageAndExactAuthor() {
        setAbout()
        composeTestRule.onNodeWithTag(AboutDialogTags.DIALOG).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.about_title)).assertExists()

        composeTestRule.onNodeWithTag(AboutDialogTags.IMAGE)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.about_image_description),
        ).assertIsDisplayed()

        composeTestRule.onNodeWithText(context.getString(R.string.about_author)).assertExists()
        composeTestRule.onNodeWithText("Juan Sebastian Rodriguez").assertExists()
    }

    @Test
    fun acceptClosesAndInvokesDismissOnce() {
        val host = setAbout()
        composeTestRule.onNodeWithTag(AboutDialogTags.ACCEPT)
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, host.dismissCount)
        assertDialogClosed()
    }

    @Test
    fun backDismissesOnceWithoutOtherSideEffects() {
        val host = setAbout()
        composeTestRule.onNodeWithTag(AboutDialogTags.DIALOG).assertIsDisplayed()
        composeTestRule.waitForIdle()
        pressBackAsUser()

        assertDialogClosed()
        assertEquals(1, host.dismissCount)
        assertEquals(0, host.sessionVersion)
    }

    private fun assertDialogClosed() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(AboutDialogTags.DIALOG)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    private fun setAbout(): AboutHost {
        val host = AboutHost()
        composeTestRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (host.visible) {
                        AboutDialog(
                            onDismiss = {
                                host.dismissCount += 1
                                host.visible = false
                            },
                        )
                    }
                }
            }
        }
        return host
    }

    private class AboutHost {
        var visible by mutableStateOf(true)
        var dismissCount = 0
        var sessionVersion by mutableStateOf(0)
    }
}
