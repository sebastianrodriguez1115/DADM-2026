package com.edu.unal.tictactoe.ui.online.lobby

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.edu.unal.tictactoe.R

internal object NicknameDialogTags {
    const val DIALOG = "nickname_dialog"
    const val FIELD = "nickname_field"
    const val ACCEPT = "nickname_accept"
    const val CANCEL = "nickname_cancel"
}

private val MinActionHeight = 48.dp

/**
 * Dialog that asks for the nickname, for the first time and for "change nickname" alike. It is
 * stateless except for the text being edited, which survives rotation.
 *
 * [nickname] is the current nickname to preload, or null when there is none yet. The text is
 * handed to [onAccept] exactly as typed: trimming and validation belong to the caller, and the
 * field does not limit its length so that an over-long text can be submitted and rejected. While
 * [isRejected] is true the error is shown and the dialog stays open. [onDismiss] runs for the
 * cancel button, Back and a tap outside the dialog.
 */
@Composable
fun NicknameDialog(
    nickname: String?,
    isRejected: Boolean,
    onAccept: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val initial = nickname.orEmpty()
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
    val text = field.text

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(NicknameDialogTags.DIALOG),
        title = { Text(text = stringResource(R.string.nickname_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = field,
                    onValueChange = { field = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NicknameDialogTags.FIELD),
                    label = { Text(text = stringResource(R.string.nickname_field_label)) },
                    isError = isRejected,
                    supportingText = if (isRejected) {
                        {
                            Text(text = stringResource(R.string.nickname_error))
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAccept(text) }),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAccept(text) },
                modifier = Modifier
                    .heightIn(min = MinActionHeight)
                    .testTag(NicknameDialogTags.ACCEPT),
            ) {
                Text(text = stringResource(R.string.about_accept))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = MinActionHeight)
                    .testTag(NicknameDialogTags.CANCEL),
            ) {
                Text(text = stringResource(R.string.lobby_cancel))
            }
        },
    )
}
