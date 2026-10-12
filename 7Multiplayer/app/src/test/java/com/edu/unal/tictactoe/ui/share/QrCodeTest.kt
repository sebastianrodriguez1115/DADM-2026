package com.edu.unal.tictactoe.ui.share

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeTest {
    private val link = "https://example.com/invitacion"

    private fun matrix(content: String): QrMatrix = checkNotNull(encodeQr(content))

    /** Renders [matrix] the way a scanner would see it and reads it back with ZXing. */
    private fun decode(matrix: QrMatrix, scale: Int = 6): String {
        val side = matrix.size * scale
        val pixels = IntArray(side * side) { index ->
            val dark = matrix[index % side / scale, index / side / scale]
            if (dark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(side, side, pixels)))
        return QRCodeReader().decode(bitmap).text
    }

    @Test
    fun theMatrixIsSquareAndBiggerThanTheMinimumVersion() {
        val matrix = matrix(link)

        // Version 1 is 21 modules wide; with the quiet zone on both sides it is 29.
        assertTrue("size ${matrix.size}", matrix.size >= 21 + 2 * QUIET_ZONE_MODULES)
        assertFalse(matrix[0, 0])
    }

    @Test
    fun theQuietZoneIsLightOnAllFourSides() {
        val matrix = matrix(link)
        val last = matrix.size - 1

        for (band in 0 until QUIET_ZONE_MODULES) {
            for (i in 0..last) {
                assertFalse("top $band,$i", matrix[i, band])
                assertFalse("bottom $band,$i", matrix[i, last - band])
                assertFalse("left $band,$i", matrix[band, i])
                assertFalse("right $band,$i", matrix[last - band, i])
            }
        }
    }

    @Test
    fun theFirstModuleInsideTheQuietZoneIsDarkBecauseOfTheFinderPattern() {
        val matrix = matrix(link)

        assertTrue(matrix[QUIET_ZONE_MODULES, QUIET_ZONE_MODULES])
        val last = matrix.size - 1 - QUIET_ZONE_MODULES
        assertTrue(matrix[last, QUIET_ZONE_MODULES])
        assertTrue(matrix[QUIET_ZONE_MODULES, last])
    }

    @Test
    fun theRoundTripReturnsExactlyTheLink() {
        assertEquals(link, decode(matrix(link)))
    }

    @Test
    fun aLongLinkWithParametersRoundTripsUnchanged() {
        val long = "https://example.com/invitacion/" + "segmento".repeat(20) +
            "?token=AbC123-_.~&utm_source=qr&utm_medium=app&redirect=https%3A%2F%2Fexample.com%2Fok#frag"

        val matrix = matrix(long)

        assertEquals(long, decode(matrix))
        assertTrue(matrix.size > matrix(link).size)
    }

    @Test
    fun aLinkWithNonAsciiCharactersRoundTripsUnchanged() {
        val accented = "https://example.com/invitación?nombre=Sebastián"

        assertEquals(accented, decode(matrix(accented)))
    }

    @Test
    fun theSameLinkAlwaysGivesTheSameMatrix() {
        val first = matrix(link)
        val second = matrix(link)

        assertEquals(first.size, second.size)
        for (y in 0 until first.size) for (x in 0 until first.size) {
            assertEquals("$x,$y", first[x, y], second[x, y])
        }
    }

    @Test
    fun anEmptyLinkGivesNoMatrix() {
        assertNull(encodeQr(""))
    }

    @Test
    fun aLinkThatDoesNotFitAnyQrGivesNoMatrix() {
        assertNull(encodeQr("a".repeat(10_000)))
    }

    @Test
    fun aNonEmptyLinkGivesAMatrix() {
        assertNotNull(encodeQr("x"))
    }
}
