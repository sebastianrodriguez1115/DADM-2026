package com.edu.unal.tictactoe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.ui.share.ShareAppDialog
import com.edu.unal.tictactoe.ui.share.ShareAppDialogTags
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val EXAMPLE_LINK = "https://example.com/invitacion"
private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()

@RunWith(AndroidJUnit4::class)
class ShareAppDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewport = FixedViewport(composeTestRule)

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private var dismissals = 0
    private var dark by mutableStateOf(false)

    private fun show(
        link: String = EXAMPLE_LINK,
        width: Int = 360,
        height: Int = 640,
        fontScale: Float = 1f,
    ) {
        viewport.setContent(width, height, fontScale) {
            TicTacToeTheme(darkTheme = dark, dynamicColor = false) {
                ShareAppDialog(link = link, onDismiss = { dismissals++ })
            }
        }
    }

    private fun qrPixels(): Triple<IntArray, Int, Int> {
        val bitmap = composeTestRule.onNodeWithTag(ShareAppDialogTags.QR).captureToImage()
            .asAndroidBitmap()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return Triple(pixels, bitmap.width, bitmap.height)
    }

    @Test
    fun showsTheTitleTheQrTheTextAndTheCloseAction() {
        show()

        assertEquals("Compartir app", context.getString(R.string.share_app))
        assertEquals("Escanea para instalar el Triqui", context.getString(R.string.share_app_scan))
        assertEquals("Cerrar", context.getString(R.string.share_app_close))
        composeTestRule.onNodeWithTag(ShareAppDialogTags.DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText("Compartir app").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ShareAppDialogTags.QR).assertIsDisplayed()
        composeTestRule.onNodeWithText("Escanea para instalar el Triqui").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ShareAppDialogTags.CLOSE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cerrar").assertIsDisplayed()
    }

    @Test
    fun theQrExposesItsDescriptionToAssistiveTechnologies() {
        show()

        assertEquals("Código QR para instalar la app", context.getString(R.string.share_app_qr_description))
        composeTestRule.onNodeWithTag(ShareAppDialogTags.QR)
            .assertContentDescriptionEquals("Código QR para instalar la app")
    }

    @Test
    fun theQrIsSquareAndAtLeast200DpWhenThereIsRoom() {
        show()

        val bounds = composeTestRule.onNodeWithTag(ShareAppDialogTags.QR).getUnclippedBoundsInRoot()
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        assertTrue("width $width", width >= 200.dp)
        assertEquals(width.value, height.value, 0.5f)
    }

    @Test
    fun closeIsEnabledMeasuresAtLeast48DpAndInvokesOnlyItsCallback() {
        show()

        val close = composeTestRule.onNodeWithTag(ShareAppDialogTags.CLOSE)
        close.assertIsEnabled().assertHasClickAction()
        val bounds = close.getUnclippedBoundsInRoot()
        assertTrue(bounds.bottom - bounds.top >= 48.dp)
        assertTrue(bounds.right - bounds.left >= 48.dp)

        close.performClick()

        assertEquals(1, dismissals)
    }

    @Test
    fun theQrAndCloseStayReachableWithALargeFont() {
        show(width = 320, height = 480, fontScale = 2f)

        composeTestRule.onNodeWithTag(ShareAppDialogTags.QR).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Escanea para instalar el Triqui").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(ShareAppDialogTags.CLOSE).assertIsDisplayed()
    }

    @Test
    fun theQrDrawnOnScreenDecodesToExactlyTheLink() {
        show()

        val (pixels, width, height) = qrPixels()
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))

        assertEquals(EXAMPLE_LINK, QRCodeReader().decode(bitmap).text)
    }

    @Test
    fun theQrIsBlackOnWhiteWithALightQuietZoneInLightAndDarkThemes() {
        show()
        val light = qrPixels()
        composeTestRule.runOnIdle { dark = true }
        composeTestRule.waitForIdle()
        val night = qrPixels()

        listOf(light, night).forEach { (pixels, width, height) ->
            assertEquals("top left corner", WHITE, pixels[0])
            assertEquals("bottom right corner", WHITE, pixels[width * height - 1])
            assertTrue("a pure black module", pixels.any { it == BLACK })
            assertTrue("only black and white", pixels.all { it == BLACK || it == WHITE })
        }
        assertTrue("the theme must not change the picture", light.first.contentEquals(night.first))
    }
}
