package com.edu.unal.tictactoe.ui.online.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edu.unal.tictactoe.R

internal object WaitingForOpponentTags {
    const val MESSAGE = "waiting_message"
    const val CANCEL = "waiting_cancel"
}

private val MinActionHeight = 48.dp

/**
 * Screen shown to whoever created a game and waits for a rival: the waiting text and the cancel
 * action, never a board. It is stateless: cancelling only calls [onCancel].
 *
 * The content sits centered in a vertically scrollable column, so in landscape or with a large
 * font scale both elements stay reachable. It respects the system window insets and has no top
 * bar, like the start screen.
 */
@Composable
fun WaitingForOpponentScreen(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.lobby_waiting_for_opponent),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(WaitingForOpponentTags.MESSAGE),
            )
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .heightIn(min = MinActionHeight)
                    .testTag(WaitingForOpponentTags.CANCEL),
            ) {
                Text(text = stringResource(R.string.lobby_cancel))
            }
        }
    }
}
