package com.edu.unal.tictactoe.ui.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edu.unal.tictactoe.R

private val MinEntryHeight = 56.dp
private val MaxEntryWidth = 360.dp

/**
 * Start screen with exactly two entries, "play against the computer" and "play online"; it shows
 * no board and no top bar. It is stateless: choosing an entry only calls its callback.
 *
 * The entries sit centered in a vertically scrollable column, so in landscape, with little height
 * or with a large font scale they stay reachable instead of being clipped. Each entry is at least
 * [MinEntryHeight] tall and spans the width up to [MaxEntryWidth]. The screen respects the
 * system window insets and does not fix the orientation.
 */
@Composable
fun StartScreen(
    onPlayLocal: () -> Unit,
    onPlayOnline: () -> Unit,
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
            StartEntry(text = stringResource(R.string.start_play_local), onClick = onPlayLocal)
            StartEntry(text = stringResource(R.string.start_play_online), onClick = onPlayOnline)
        }
    }
}

@Composable
private fun StartEntry(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .widthIn(max = MaxEntryWidth)
            .fillMaxWidth()
            .heightIn(min = MinEntryHeight),
    ) {
        Text(text)
    }
}
