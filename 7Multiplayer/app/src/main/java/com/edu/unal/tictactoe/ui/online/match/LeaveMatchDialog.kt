package com.edu.unal.tictactoe.ui.online.match

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edu.unal.tictactoe.R

internal object LeaveMatchDialogTags {
    const val DIALOG = "leave_match_dialog"
    const val CONFIRM = "leave_match_confirm"
    const val CANCEL = "leave_match_cancel"
}

private val MinActionHeight = 48.dp

/**
 * Confirmation shown before leaving a match in progress. It is stateless: whoever shows it decides
 * when it exists. [onDismiss] runs for "Cancelar", Back and a tap outside the dialog; [onConfirm]
 * runs for "Abandonar".
 */
@Composable
fun LeaveMatchDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(LeaveMatchDialogTags.DIALOG),
        title = { Text(text = stringResource(R.string.leave_match_title)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier
                    .heightIn(min = MinActionHeight)
                    .testTag(LeaveMatchDialogTags.CONFIRM),
            ) {
                Text(text = stringResource(R.string.leave_match_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = MinActionHeight)
                    .testTag(LeaveMatchDialogTags.CANCEL),
            ) {
                Text(text = stringResource(R.string.lobby_cancel))
            }
        },
    )
}
