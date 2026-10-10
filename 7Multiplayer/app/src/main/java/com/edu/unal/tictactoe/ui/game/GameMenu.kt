package com.edu.unal.tictactoe.ui.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.edu.unal.tictactoe.R

internal object GameMenuTags {
    const val BUTTON = "game_menu_button"
    const val MENU = "game_menu"
    const val NEW_GAME = "game_menu_new_game"
    const val DIFFICULTY = "game_menu_difficulty"
    const val SOUND = "game_menu_sound"
    const val ABOUT = "game_menu_about"
    const val RESET_SCORES = "game_menu_reset_scores"
}

/**
 * Options menu with exactly five entries, in this order: New game, Difficulty (only actionable
 * if [canChangeDifficulty]), Sound (exposes its state), Reset scores and About. Every entry has
 * an icon and a text label, and choosing one closes the menu before invoking its callback.
 *
 * It is shown as a compact button placed next to the game controls, not in a top bar.
 *
 * [onResetScores] is immediate (no confirmation), always enabled, and only zeroes the three
 * score counters, which are stored persistently; it does not change the game in progress or the
 * finished one. There is no exit option. The game in progress is not permanent storage: only the
 * score and the difficulty persist.
 */
@Composable
fun GameMenu(
    canChangeDifficulty: Boolean,
    soundEnabled: Boolean,
    onNewGame: () -> Unit,
    onDifficulty: () -> Unit,
    onToggleSound: () -> Unit,
    onResetScores: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val soundStateLabel = stringResource(
        if (soundEnabled) R.string.sound_enabled else R.string.sound_disabled,
    )

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag(GameMenuTags.BUTTON),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_menu_overflow),
                contentDescription = stringResource(R.string.menu_open),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag(GameMenuTags.MENU),
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.new_game)) },
                onClick = {
                    expanded = false
                    onNewGame()
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu_new_game),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.testTag(GameMenuTags.NEW_GAME),
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.menu_difficulty)) },
                onClick = {
                    expanded = false
                    onDifficulty()
                },
                enabled = canChangeDifficulty,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu_difficulty),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.testTag(GameMenuTags.DIFFICULTY),
            )
            DropdownMenuItem(
                text = {
                    Column {
                        Text(text = stringResource(R.string.menu_sound))
                        Text(text = soundStateLabel)
                    }
                },
                onClick = {
                    expanded = false
                    onToggleSound()
                },
                leadingIcon = {
                    Icon(painter = painterResource(R.drawable.ic_menu_sound), contentDescription = null)
                },
                modifier = Modifier
                    .testTag(GameMenuTags.SOUND)
                    .semantics {
                        stateDescription = soundStateLabel
                    },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.menu_reset_scores)) },
                onClick = {
                    expanded = false
                    onResetScores()
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu_reset_scores),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.testTag(GameMenuTags.RESET_SCORES),
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.menu_about)) },
                onClick = {
                    expanded = false
                    onAbout()
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu_about),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.testTag(GameMenuTags.ABOUT),
            )
        }
    }
}
