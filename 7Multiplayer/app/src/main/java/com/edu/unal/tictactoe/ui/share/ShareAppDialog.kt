package com.edu.unal.tictactoe.ui.share

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edu.unal.tictactoe.R
import kotlin.math.roundToInt

internal object ShareAppDialogTags {
    const val DIALOG = "share_app_dialog"
    const val QR = "share_app_qr"
    const val CLOSE = "share_app_close"
}

private val MinActionHeight = 48.dp
private val MaxQrSide = 280.dp

// A QR code must be dark modules on a light background to be read, so these never follow the theme.
private val QrDark = Color.Black
private val QrLight = Color.White

/**
 * Dialog with the QR code of [link], the text that invites to scan it and the "Cerrar" action. It
 * is stateless: whoever shows it decides when it exists, and [onDismiss] runs for "Cerrar", Back
 * and a tap outside the dialog. The content scrolls when it does not fit, for example in landscape
 * or with a large font.
 *
 * The code is drawn on a canvas from the module matrix, so it stays sharp at any size, always dark
 * on white (also in the dark theme) and with its quiet zone. A [link] that cannot be encoded
 * leaves the code out.
 */
@Composable
fun ShareAppDialog(link: String, onDismiss: () -> Unit) {
    val matrix = remember(link) { encodeQr(link) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(ShareAppDialogTags.DIALOG),
        title = { Text(text = stringResource(R.string.share_app)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (matrix != null) {
                    QrCodeImage(
                        matrix = matrix,
                        description = stringResource(R.string.share_app_qr_description),
                    )
                }
                Text(
                    text = stringResource(R.string.share_app_scan),
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = MinActionHeight)
                    .testTag(ShareAppDialogTags.CLOSE),
            ) {
                Text(text = stringResource(R.string.share_app_close))
            }
        },
    )
}

/** Draws [matrix] as a square as wide as the space allows, up to [MaxQrSide]. */
@Composable
private fun QrCodeImage(matrix: QrMatrix, description: String) {
    Canvas(
        modifier = Modifier
            .widthIn(max = MaxQrSide)
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag(ShareAppDialogTags.QR)
            .semantics {
                contentDescription = description
                role = Role.Image
            },
    ) {
        drawRect(color = QrLight)
        val module = size.width / matrix.size
        // Each module is snapped to whole pixels so the edges stay crisp at any size.
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (!matrix[x, y]) continue
                val left = (x * module).roundToInt()
                val top = (y * module).roundToInt()
                val right = ((x + 1) * module).roundToInt()
                val bottom = ((y + 1) * module).roundToInt()
                drawRect(
                    color = QrDark,
                    topLeft = Offset(left.toFloat(), top.toFloat()),
                    size = Size((right - left).toFloat(), (bottom - top).toFloat()),
                )
            }
        }
    }
}
