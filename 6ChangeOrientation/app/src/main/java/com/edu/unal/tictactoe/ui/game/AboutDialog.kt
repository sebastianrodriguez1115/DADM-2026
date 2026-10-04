package com.edu.unal.tictactoe.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edu.unal.tictactoe.R

internal object AboutDialogTags {
    const val DIALOG = "about_dialog"
    const val IMAGE = "about_dialog_image"
    const val ACCEPT = "about_dialog_accept"
}

@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(AboutDialogTags.DIALOG),
        title = { Text(text = stringResource(R.string.about_title)) },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_tic_tac_toe),
                    contentDescription = stringResource(R.string.about_image_description),
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            color = colorResource(R.color.tic_tac_toe_icon_background),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .testTag(AboutDialogTags.IMAGE),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.about_author),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(AboutDialogTags.ACCEPT),
            ) {
                Text(text = stringResource(R.string.about_accept))
            }
        },
    )
}
