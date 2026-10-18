package com.edu.unal.directorio.ui.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edu.unal.directorio.R
import com.edu.unal.directorio.ui.labelRes

const val DETAIL_BACK_TAG = "detail_back"
const val DETAIL_EDIT_TAG = "detail_edit"
const val DETAIL_DELETE_TAG = "detail_delete"

/**
 * Detail of a company (design D9). State and callbacks are parameters. The content scrolls when
 * it does not fit. While the company is loading, or does not exist (the screen is about to be
 * closed), only the bar is drawn.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDetailScreen(
    state: CompanyDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(DETAIL_BACK_TAG)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back_description),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val content = state.content
        if (content is CompanyDetailContent.Loaded) {
            val company = content.company
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(company.name, style = MaterialTheme.typography.headlineSmall)
                DetailField(
                    R.string.detail_label_classification,
                    // joinToString's lambda is not composable, so resolve the labels first.
                    company.classifications.sortedBy { it.ordinal }
                        .map { stringResource(it.labelRes()) }
                        .joinToString(", "),
                )
                DetailField(R.string.detail_label_email, company.email)
                DetailField(R.string.detail_label_website, company.url)
                DetailField(R.string.detail_label_phone, company.phone)
                DetailField(R.string.detail_label_products, company.products)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onEdit, modifier = Modifier.testTag(DETAIL_EDIT_TAG)) {
                        Text(stringResource(R.string.detail_edit))
                    }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.testTag(DETAIL_DELETE_TAG)) {
                        Text(stringResource(R.string.detail_delete))
                    }
                }
                if (state.deleteFailed) {
                    Text(
                        stringResource(R.string.detail_delete_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (state.deleteDialogVisible) {
                DeleteCompanyDialog(
                    companyName = company.name,
                    onConfirm = onConfirmDelete,
                    onDismiss = onDismissDelete,
                )
            }
        }
    }
}

/** A label with its value; an empty [value] shows "No indicado". Line breaks are kept. */
@Composable
private fun DetailField(@StringRes label: Int, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value.ifEmpty { stringResource(R.string.detail_not_provided) },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
