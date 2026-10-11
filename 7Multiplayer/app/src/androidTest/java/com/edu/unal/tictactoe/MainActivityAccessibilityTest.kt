package com.edu.unal.tictactoe

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.ui.game.AboutDialogTags
import com.edu.unal.tictactoe.ui.game.DifficultyDialogTags
import com.edu.unal.tictactoe.ui.game.DifficultyLabels
import com.edu.unal.tictactoe.ui.game.GameMenuTags
import com.edu.unal.tictactoe.ui.game.GameViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BOARD_TAG = "tic_tac_toe_board"
private const val NEW_GAME_TAG = "tic_tac_toe_new_game"
private const val CELL_TAG_PREFIX = "tic_tac_toe_cell_"
private val MIN_TOUCH_TARGET = 48.dp
private val BOUNDS_TOLERANCE = 1.dp

@RunWith(AndroidJUnit4::class)
class MainActivityAccessibilityTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var activityViewModel: GameViewModel
    private val preferencesFixture = GamePreferencesTestFixture()
    private var originalRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun launchActivity() {
        preferencesFixture.prepare(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            originalRequestedOrientation = activity.requestedOrientation
            activityViewModel = ViewModelProvider(activity)[GameViewModel::class.java]
        }
        composeTestRule.waitForIdle()
        composeTestRule.startLocalGame()
    }

    @After
    fun restoreOrientationAndCloseActivity() {
        try {
            if (::scenario.isInitialized) {
                try {
                    if (scenario.state != Lifecycle.State.DESTROYED) {
                        scenario.onActivity { it.requestedOrientation = originalRequestedOrientation }
                    }
                } finally {
                    scenario.close()
                }
            }
        } finally {
            preferencesFixture.cleanup()
        }
    }

    @Test
    fun realPortraitAndLandscapeShowNoTitleAndKeepMenuBoardAndControlsReachable() {
        val orientations = listOf(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT to Configuration.ORIENTATION_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE to Configuration.ORIENTATION_LANDSCAPE,
        )
        orientations.forEach { (requested, expected) ->
            forceRealOrientation(requested, expected)
            val root = composeTestRule.onRoot().getUnclippedBoundsInRoot()
            val rootWidth = root.right - root.left
            val rootHeight = root.bottom - root.top
            val isLandscape = expected == Configuration.ORIENTATION_LANDSCAPE
            assertTrue(
                "root must take the real orientation, not a clamped box",
                if (isLandscape) rootWidth > rootHeight else rootHeight > rootWidth,
            )
            assertAccessibleLayout()
        }
    }

    @Test
    fun menuEntriesExposeLabelsAndClickActionsMatchingTheDifficultyGate() {
        composeTestRule.onNodeWithTag(BOARD_TAG).performScrollTo()
        composeTestRule.onNodeWithTag(CELL_TAG_PREFIX + 4).assertIsDisplayed().assertIsEnabled().performClick()
        composeTestRule.waitForIdle()
        assertFalse(activityViewModel.uiState.value.canChangeDifficulty)
        openMenu()
        assertMenuEntries(difficultyEnabled = false)
        clickMenuEntry(GameMenuTags.NEW_GAME)
        openMenu()
        clickMenuEntry(GameMenuTags.NEW_GAME)
        assertTrue(activityViewModel.uiState.value.canChangeDifficulty)
        openMenu()
        assertMenuEntries(difficultyEnabled = true)
        composeTestRule.waitForIdle()
        pressBack()
        composeTestRule.waitForIdle()
        awaitTag(GameMenuTags.MENU, present = false)
    }

    @Test
    fun difficultyDialogOptionsExposeRadioSemanticsAndCancelKeepsTheValue() {
        openMenu()
        clickMenuEntry(GameMenuTags.DIFFICULTY)
        awaitTag(DifficultyDialogTags.DIALOG, present = true)
        DifficultyLevel.entries.forEach { level ->
            val tag = DifficultyDialogTags.option(level)
            val option = composeTestRule.onNodeWithTag(tag)
                .performScrollTo()
                .assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                .assertHasClickAction()
                .assert(hasText(context.getString(DifficultyLabels.labelRes(level))))
            assertTouchTarget(tag)
            if (level == DifficultyLevel.Expert) option.assertIsSelected() else option.assertIsNotSelected()
        }
        composeTestRule.waitForIdle()
        pressBack()
        composeTestRule.waitForIdle()
        awaitTag(DifficultyDialogTags.DIALOG, present = false)
        val state = activityViewModel.uiState.value
        assertEquals(DifficultyLevel.Expert, state.difficulty)
        assertTrue(state.canChangeDifficulty)
    }

    @Test
    fun aboutDialogButtonsExposeLabelsRolesAndTouchTargets() {
        openMenu()
        clickMenuEntry(GameMenuTags.ABOUT)
        awaitTag(AboutDialogTags.DIALOG, present = true)
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.about_image_description),
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Juan Sebastian Rodriguez").assertIsDisplayed()
        assertLabeled(AboutDialogTags.ACCEPT, R.string.about_accept, Role.Button, enabled = true)
        composeTestRule.onNodeWithTag(AboutDialogTags.ACCEPT).performClick()
        awaitTag(AboutDialogTags.DIALOG, present = false)
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
    }

    private fun forceRealOrientation(requested: Int, expected: Int) {
        scenario.onActivity { it.requestedOrientation = requested }
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            var orientation = Configuration.ORIENTATION_UNDEFINED
            scenario.onActivity { orientation = it.resources.configuration.orientation }
            orientation == expected && scenario.state == Lifecycle.State.RESUMED
        }
        composeTestRule.waitForIdle()
    }

    private fun assertAccessibleLayout() {
        composeTestRule.onNodeWithTag(BOARD_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        // The controls may be below the fold on a short window: reach the menu by scrolling, then
        // read both bounds at that same scroll position so they can be compared.
        val menu = composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .getUnclippedBoundsInRoot()
        val board = composeTestRule.onNodeWithTag(BOARD_TAG).getUnclippedBoundsInRoot()
        val width = board.right - board.left
        val height = board.bottom - board.top
        assertTrue("board must have positive size", width > 0.dp && height > 0.dp)
        assertTrue("board must stay square", kotlin.math.abs((width - height).value) <= BOUNDS_TOLERANCE.value)
        composeTestRule.onNodeWithText(context.getString(R.string.app_name)).assertDoesNotExist()
        assertTrue(
            "menu must not overlap the board",
            menu.left >= board.right || board.left >= menu.right ||
                menu.top >= board.bottom || board.top >= menu.bottom,
        )
        assertTouchTarget(GameMenuTags.BUTTON)
        (0 until 9).forEach { location ->
            val tag = CELL_TAG_PREFIX + location
            composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
            assertTouchTarget(tag)
        }
        val newGame = composeTestRule.onNodeWithTag(NEW_GAME_TAG)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .assert(hasText(context.getString(R.string.new_game)))
            .getUnclippedBoundsInRoot()
        assertTouchTarget(NEW_GAME_TAG)
        val menuBeside = composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .getUnclippedBoundsInRoot()
        assertTrue(
            "menu must share the controls row with New game",
            menuBeside.top < newGame.bottom && newGame.top < menuBeside.bottom,
        )
        openMenu()
        assertMenuEntries(difficultyEnabled = true)
        pressBack()
        composeTestRule.waitForIdle()
        awaitTag(GameMenuTags.MENU, present = false)
    }

    private fun assertMenuEntries(difficultyEnabled: Boolean) {
        assertEquals(
            5,
            composeTestRule.onAllNodes(
                hasAnyAncestor(hasTestTag(GameMenuTags.MENU)) and hasClickAction(),
            )
                .fetchSemanticsNodes().size,
        )
        assertLabeled(GameMenuTags.NEW_GAME, R.string.new_game, enabled = true)
        assertLabeled(GameMenuTags.ABOUT, R.string.menu_about, enabled = true)
        assertLabeled(GameMenuTags.RESET_SCORES, R.string.menu_reset_scores, enabled = true)
        assertLabeled(GameMenuTags.DIFFICULTY, R.string.menu_difficulty, enabled = difficultyEnabled)
        assertLabeled(GameMenuTags.SOUND, R.string.menu_sound, enabled = true)
        composeTestRule.onNodeWithTag(GameMenuTags.SOUND)
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.sound_enabled),
            ))
    }

    private fun assertLabeled(tag: String, labelRes: Int, role: Role? = null, enabled: Boolean) {
        val node = composeTestRule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assert(hasText(context.getString(labelRes)))
        if (role != null) node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, role))
        if (enabled) node.assertIsEnabled().assertHasClickAction() else node.assertIsNotEnabled()
        assertTouchTarget(tag)
    }

    private fun assertTouchTarget(tag: String) {
        val node = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
        val touch = node.touchBoundsInRoot
        val min = MIN_TOUCH_TARGET - BOUNDS_TOLERANCE
        with(composeTestRule.density) {
            assertTrue("$tag touch width below 48dp", (touch.right - touch.left).toDp() >= min)
            assertTrue("$tag touch height below 48dp", (touch.bottom - touch.top).toDp() >= min)
        }
    }

    private fun openMenu() {
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.menu_open))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun clickMenuEntry(tag: String) {
        composeTestRule.onNodeWithTag(tag).assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        awaitTag(GameMenuTags.MENU, present = false)
    }

    private fun awaitTag(tag: String, present: Boolean) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().run {
                if (present) isNotEmpty() else isEmpty()
            }
        }
    }
}
