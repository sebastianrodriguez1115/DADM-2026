package com.edu.unal.directorio.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.unal.directorio.ui.companyViewModelFactory
import com.edu.unal.directorio.ui.list.CompanyListScreen
import com.edu.unal.directorio.ui.list.CompanyListUiState
import com.edu.unal.directorio.ui.list.CompanyListViewModel

/**
 * Draws the current screen. The ViewModel is Activity-scoped.
 *
 * PROVISIONAL: the content of each screen is a placeholder that groups 5 to 7 replace.
 * Back is intercepted only on detail and form; on the list the system handles it.
 */
@Composable
fun AppNavigation(navigation: NavigationViewModel = viewModel()) {
    val screen by navigation.screen.collectAsState()

    when (screen) {
        Screen.List -> Unit
        is Screen.Detail, is Screen.Form -> BackHandler { navigation.back() }
    }

    val current = screen
    if (current is Screen.List) {
        ListRoute(navigation)
        return
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            when (current) {
                Screen.List -> Unit
                is Screen.Detail -> {
                    Text("PROVISIONAL detail ${current.id}")
                    Button(onClick = { navigation.openEdit(current.id) }) { Text("PROVISIONAL edit") }
                    Button(onClick = { navigation.onDeleted() }) { Text("PROVISIONAL delete") }
                }
                is Screen.Form -> {
                    Text("PROVISIONAL form ${current.id ?: "new"}")
                    Button(onClick = { navigation.onSaved() }) { Text("PROVISIONAL save") }
                    Button(onClick = { navigation.back() }) { Text("PROVISIONAL cancel") }
                }
            }
        }
    }
}

/** Connects the list screen with its Activity-scoped ViewModel (filters survive detail and back). */
@Composable
private fun ListRoute(navigation: NavigationViewModel) {
    val viewModel: CompanyListViewModel =
        viewModel(factory = companyViewModelFactory { CompanyListViewModel(it) })
    val filter by viewModel.filter.collectAsState()
    val results by viewModel.results.collectAsState()
    CompanyListScreen(
        state = CompanyListUiState(filter.query, filter.classifications, results),
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::clearQuery,
        onToggleClassification = viewModel::toggleClassification,
        onCompanyClick = navigation::openDetail,
        onAddClick = navigation::openCreate,
    )
}
