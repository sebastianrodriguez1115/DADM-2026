package com.edu.unal.tictactoe.ui.game

import androidx.compose.foundation.layout.Box
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
import com.edu.unal.tictactoe.R

internal object GameMenuTags {
    const val BUTTON = "game_menu_button"
    const val MENU = "game_menu"
    const val NEW_GAME = "game_menu_new_game"
    const val DIFFICULTY = "game_menu_difficulty"
    const val ABOUT = "game_menu_about"
    const val EXIT = "game_menu_exit"
}

@Composable
fun GameMenu(
    canChangeDifficulty: Boolean,
    onNewGame: () -> Unit,
    onDifficulty: () -> Unit,
    onAbout: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

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
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.menu_exit)) },
                onClick = {
                    expanded = false
                    onExit()
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu_exit),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.testTag(GameMenuTags.EXIT),
            )
        }
    }
}
