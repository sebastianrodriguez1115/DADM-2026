package com.edu.unal.tictactoe.ui.share

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Width, in modules, of the light margin the QR specification requires around the code. */
internal const val QUIET_ZONE_MODULES = 4

/**
 * Square matrix of QR modules, quiet zone included: [get] is true for a dark module and false for
 * a light one, with `x` growing to the right and `y` downwards, both in `0 until size`.
 */
class QrMatrix internal constructor(val size: Int, private val dark: BooleanArray) {
    operator fun get(x: Int, y: Int): Boolean = dark[y * size + x]
}

/**
 * Encodes [content] exactly as given (UTF-8, error correction level M) into a [QrMatrix] with a
 * [QUIET_ZONE_MODULES] margin on every side. Returns null when there is nothing to encode (empty
 * [content]) or it does not fit in any QR code. Pure: no Android or Compose types.
 */
fun encodeQr(content: String): QrMatrix? {
    if (content.isEmpty()) return null
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to QUIET_ZONE_MODULES,
    )
    // Requesting a 0x0 image makes ZXing return the smallest one: one pixel per module.
    val bits = try {
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
    } catch (_: WriterException) {
        return null
    }
    val size = bits.width
    return QrMatrix(size, BooleanArray(size * size) { bits[it % size, it / size] })
}
