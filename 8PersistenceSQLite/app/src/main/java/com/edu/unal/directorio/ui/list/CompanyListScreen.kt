package com.edu.unal.directorio.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edu.unal.directorio.R
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.domain.Company
import com.edu.unal.directorio.ui.labelRes

/** Test tag of the search field. */
const val SEARCH_FIELD_TAG = "list_search_field"

/** Test tag of the company list. */
const val COMPANY_LIST_TAG = "list_companies"

/** Test tag of the filter chip of [classification]. */
fun chipTag(classification: Classification) = "list_chip_${classification.code}"

/** Test tag of the list item of the company with [id]. */
fun companyItemTag(id: Long) = "list_item_$id"

/** Company list screen. State and callbacks come as parameters; there is no ViewModel in here. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyListScreen(
    state: CompanyListUiState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onToggleClassification: (Classification) -> Unit,
    onCompanyClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.list_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.list_add_description))
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.list_search_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.list_search_clear_description),
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(SEARCH_FIELD_TAG),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Classification.entries.forEach { classification ->
                    FilterChip(
                        selected = classification in state.selected,
                        onClick = { onToggleClassification(classification) },
                        label = { Text(stringResource(classification.labelRes())) },
                        modifier = Modifier.testTag(chipTag(classification)),
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                val results = state.results
                when {
                    results.showDirectoryEmpty -> EmptyMessage(
                        title = stringResource(R.string.list_empty_title),
                        hint = stringResource(R.string.list_empty_hint),
                    )
                    results.showNoMatches -> EmptyMessage(
                        title = stringResource(R.string.list_no_matches),
                        hint = null,
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag(COMPANY_LIST_TAG),
                        contentPadding = PaddingValues(bottom = 88.dp),
                    ) {
                        items(results.companies, key = { it.id }) { company ->
                            CompanyItem(company, onClick = { onCompanyClick(company.id) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyItem(company: Company, onClick: () -> Unit) {
    // Classifications in enum order so the text is stable.
    val labels = Classification.entries.filter { it in company.classifications }
        .map { stringResource(it.labelRes()) }
        .joinToString(", ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(companyItemTag(company.id)),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(company.name, style = MaterialTheme.typography.titleMedium)
        Text(
            labels,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyMessage(title: String, hint: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
