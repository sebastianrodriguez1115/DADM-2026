package com.edu.unal.directorio.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.unal.directorio.ui.companyViewModelFactory
import com.edu.unal.directorio.ui.detail.CompanyDetailEvent
import com.edu.unal.directorio.ui.detail.CompanyDetailScreen
import com.edu.unal.directorio.ui.detail.CompanyDetailViewModel
import com.edu.unal.directorio.ui.form.CompanyFormEvent
import com.edu.unal.directorio.ui.form.CompanyFormScreen
import com.edu.unal.directorio.ui.form.CompanyFormViewModel
import com.edu.unal.directorio.ui.list.CompanyListScreen
import com.edu.unal.directorio.ui.list.CompanyListUiState
import com.edu.unal.directorio.ui.list.CompanyListViewModel

/**
 * Draws the current screen. The ViewModel is Activity-scoped.
 *
 * Back is intercepted only on detail and form; on the list the system handles it.
 */
@Composable
fun AppNavigation(navigation: NavigationViewModel = viewModel()) {
    val screen by navigation.screen.collectAsState()

    when (screen) {
        Screen.List -> Unit
        is Screen.Detail, is Screen.Form -> BackHandler { navigation.back() }
    }

    when (val current = screen) {
        Screen.List -> ListRoute(navigation)
        is Screen.Detail -> DetailRoute(current, navigation)
        is Screen.Form -> FormRoute(current, navigation)
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

/**
 * Connects the form with its Activity-scoped ViewModel. `open` runs once per entry to the form
 * (also when the same id is opened again later); the `rememberSaveable` flag keeps a rotation
 * from calling it again and wiping what was typed. Events are consumed once: saved goes to
 * `onSaved()` and a missing company to `onDeleted()`.
 */
@Composable
private fun FormRoute(screen: Screen.Form, navigation: NavigationViewModel) {
    val viewModel: CompanyFormViewModel =
        viewModel(factory = companyViewModelFactory { CompanyFormViewModel(it) })
    // The calculation lambda runs only on the first composition of this entry (before the first
    // frame), not after a rotation restores the saved value, and it writes no Compose state.
    // Leaving the form discards the saved value, so the next entry opens (and resets) again.
    rememberSaveable { viewModel.open(screen.id); true }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CompanyFormEvent.Saved -> navigation.onSaved()
                CompanyFormEvent.CompanyNotFound -> navigation.onDeleted()
            }
        }
    }
    CompanyFormScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onUrlChange = viewModel::onUrlChange,
        onPhoneChange = viewModel::onPhoneChange,
        onProductsChange = viewModel::onProductsChange,
        onToggleClassification = viewModel::onToggleClassification,
        onSave = viewModel::save,
        onCancel = navigation::back,
    )
}

/**
 * Connects the detail with its Activity-scoped ViewModel. `open` runs once per entry (same
 * `rememberSaveable` pattern as [FormRoute]). The back-to-list signal, sent after deleting or
 * when the company does not exist, is consumed once and goes to `onDeleted()`.
 */
@Composable
private fun DetailRoute(screen: Screen.Detail, navigation: NavigationViewModel) {
    val viewModel: CompanyDetailViewModel =
        viewModel(factory = companyViewModelFactory { CompanyDetailViewModel(it) })
    rememberSaveable { viewModel.open(screen.id); true }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CompanyDetailEvent.BackToList -> navigation.onDeleted()
            }
        }
    }
    CompanyDetailScreen(
        state = state,
        onBack = navigation::back,
        onEdit = { navigation.openEdit(screen.id) },
        onDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDelete,
    )
}
