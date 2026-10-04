package com.edu.unal.tictactoe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals

private const val FIXED_VIEWPORT_TAG = "fixed_test_viewport"
private const val VIEWPORT_TOLERANCE_DP = 1f

/**
 * Hosts content in a container of exactly [width] x [height] dp whatever the device running the
 * test, so the layout under test sees the same scenario on every screen:
 *
 * - **Size and density.** When no density is requested and the window has fewer pixels than the
 *   viewport needs at the window density, the density is lowered just enough for the viewport to
 *   fit. An explicitly requested density is never changed: if it does not fit, the test fails.
 * - **Font scale.** The content always gets the requested font scale (1 by default), never the
 *   system one.
 * - **Window insets.** The safe-drawing insets of the test window (system bars and display
 *   cutout) are consumed, so content that pads itself with them, such as a `Scaffold`, still gets
 *   the whole viewport. Otherwise those insets, which are fixed in pixels, would take a different
 *   amount of dp on each device and at each lowered density.
 *
 * The container starts at the origin of the root, so bounds in root are bounds in the viewport.
 * `getUnclippedBoundsInRoot` converts pixels with the density of the node it reads, which for any
 * node inside the viewport is the applied one; [density] exposes it.
 *
 * The system theme is not part of the viewport: content must pass `darkTheme` explicitly whenever
 * a test depends on the colors.
 *
 * After every [setContent] and [show] the measured container is asserted to have the requested
 * size (and the requested density, if any), so a clipped viewport fails the test instead of
 * silently changing its scenario.
 */
internal class FixedViewport(private val rule: ComposeContentTestRule) {
    private data class Spec(val width: Int, val height: Int, val fontScale: Float, val density: Float?)

    private var spec by mutableStateOf(Spec(1, 1, 1f, null))

    /** Density actually applied to the viewport (px per dp). */
    @Volatile
    var density: Float = 0f
        private set

    fun setContent(
        width: Int,
        height: Int,
        fontScale: Float = 1f,
        density: Float? = null,
        content: @Composable () -> Unit,
    ) {
        spec = Spec(width, height, fontScale, density)
        rule.setContent {
            val windowDensity = LocalDensity.current
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val requested = spec.density ?: windowDensity.density
                val applied = minOf(
                    requested,
                    constraints.maxWidth / spec.width.toFloat(),
                    constraints.maxHeight / spec.height.toFloat(),
                )
                SideEffect { this@FixedViewport.density = applied }
                CompositionLocalProvider(LocalDensity provides Density(applied, spec.fontScale)) {
                    Box(
                        Modifier
                            .size(spec.width.dp, spec.height.dp)
                            .consumeWindowInsets(WindowInsets.safeDrawing)
                            .testTag(FIXED_VIEWPORT_TAG),
                    ) {
                        content()
                    }
                }
            }
        }
        rule.waitForIdle()
        assertMeasured()
    }

    fun show(width: Int, height: Int, fontScale: Float = 1f, density: Float? = null) {
        rule.runOnIdle { spec = Spec(width, height, fontScale, density) }
        rule.waitForIdle()
        assertMeasured()
    }

    private fun assertMeasured() {
        spec.density?.let { requested ->
            assertEquals("requested density does not fit the window", requested, density, 0f)
        }
        val bounds = rule.onNodeWithTag(FIXED_VIEWPORT_TAG).getUnclippedBoundsInRoot()
        assertEquals(
            "viewport width was clipped by the window",
            spec.width.toFloat(),
            bounds.right.value - bounds.left.value,
            VIEWPORT_TOLERANCE_DP,
        )
        assertEquals(
            "viewport height was clipped by the window",
            spec.height.toFloat(),
            bounds.bottom.value - bounds.top.value,
            VIEWPORT_TOLERANCE_DP,
        )
    }
}
