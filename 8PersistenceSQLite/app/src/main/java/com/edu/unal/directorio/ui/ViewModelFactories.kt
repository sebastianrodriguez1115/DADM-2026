package com.edu.unal.directorio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.edu.unal.directorio.DirectorioApplication
import com.edu.unal.directorio.data.CompanyRepository

/**
 * Builds a factory for a ViewModel that depends only on the [CompanyRepository].
 *
 * The repository is read from the application's container each time a ViewModel is created, not
 * when the factory is built, so a ViewModel created after `AppContainer.resetForTests()` uses
 * the rebuilt database.
 *
 * Usage in a composable: `viewModel(factory = companyViewModelFactory { repo -> MyViewModel(repo) })`.
 */
inline fun <reified VM : ViewModel> companyViewModelFactory(
    crossinline create: (CompanyRepository) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer { create(repositoryFrom(this)) }
}

/** Reads the repository from the [DirectorioApplication] found in [extras]. */
fun repositoryFrom(extras: CreationExtras): CompanyRepository =
    (extras[APPLICATION_KEY] as DirectorioApplication).container.companyRepository
