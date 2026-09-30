package com.edu.unal.tictactoe.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.edu.unal.tictactoe.R
import com.edu.unal.tictactoe.game.models.DifficultyLevel

internal object DifficultyDialogTags {
    const val DIALOG = "difficulty_dialog"

    fun option(level: DifficultyLevel): String = "difficulty_option_${level.name}"
}

@Composable
fun DifficultyDialog(
    currentDifficulty: DifficultyLevel,
    onDifficultySelected: (DifficultyLevel) -> Boolean,
    onDismiss: () -> Unit,
    onShowToast: (DifficultyLevel) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(DifficultyDialogTags.DIALOG),
        title = { Text(text = stringResource(R.string.difficulty_dialog_title)) },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .verticalScroll(scrollState),
            ) {
                DifficultyLevel.entries.forEach { level ->
                    DifficultyOption(
                        level = level,
                        selected = level == currentDifficulty,
                        onSelect = {
                            val accepted = onDifficultySelected(level)
                            onDismiss()
                            if (accepted) {
                                onShowToast(level)
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun DifficultyOption(
    level: DifficultyLevel,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = 8.dp)
            .testTag(DifficultyDialogTags.option(level)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = stringResource(DifficultyLabels.labelRes(level)),
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
