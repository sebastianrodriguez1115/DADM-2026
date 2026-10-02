package com.edu.unal.tictactoe.ui.game

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.edu.unal.tictactoe.R

internal object ExitConfirmationDialogTags {
    const val DIALOG = "exit_confirmation_dialog"
    const val CONFIRM = "exit_confirmation_confirm"
    const val CANCEL = "exit_confirmation_cancel"
}

@Composable
fun ExitConfirmationDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = Modifier.testTag(ExitConfirmationDialogTags.DIALOG),
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = { Text(text = stringResource(R.string.exit_dialog_title)) },
        text = { Text(text = stringResource(R.string.exit_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(ExitConfirmationDialogTags.CONFIRM),
            ) {
                Text(text = stringResource(R.string.exit_yes))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag(ExitConfirmationDialogTags.CANCEL),
            ) {
                Text(text = stringResource(R.string.exit_no))
            }
        },
    )
}
