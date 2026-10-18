package com.edu.unal.directorio.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.unal.directorio.data.CompanyRepository
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.domain.Company
import com.edu.unal.directorio.domain.CompanyFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * What the list shows. At most one empty message applies: [showDirectoryEmpty] and
 * [showNoMatches] are never both true, and neither is true before the first load.
 */
data class CompanyListResults(
    val companies: List<Company> = emptyList(),
    /** `true` when at least one company exists, regardless of the filter. */
    val hasAnyCompany: Boolean = false,
    val loaded: Boolean = false,
) {
    val showDirectoryEmpty: Boolean get() = loaded && companies.isEmpty() && !hasAnyCompany
    val showNoMatches: Boolean get() = loaded && companies.isEmpty() && hasAnyCompany
}

/** Everything the list screen renders: the typed filter plus the [results]. */
data class CompanyListUiState(
    val query: String = "",
    val selected: Set<Classification> = emptySet(),
    val results: CompanyListResults = CompanyListResults(),
)

/**
 * Holds the search text and the checked classifications (Activity-scoped, so they survive going to
 * the detail and back, design D6) and derives the list from the repository. Normalization and
 * escaping are the repository's job.
 *
 * [filter] updates immediately (the text field reads it); [results] is derived and shared with
 * `WhileSubscribed(5_000)`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CompanyListViewModel(private val repository: CompanyRepository) : ViewModel() {
    private val _filter = MutableStateFlow(CompanyFilter())

    /** The search text exactly as typed and the checked classifications. */
    val filter: StateFlow<CompanyFilter> = _filter.asStateFlow()

    /** The filtered list and whether any company exists. */
    val results: StateFlow<CompanyListResults> = _filter
        .flatMapLatest { filter ->
            repository.observeCompanies(filter).flatMapLatest { companies ->
                if (companies.isNotEmpty()) {
                    flowOf(CompanyListResults(companies, hasAnyCompany = true, loaded = true))
                } else {
                    repository.observeCompanies(CompanyFilter()).map { all ->
                        CompanyListResults(emptyList(), hasAnyCompany = all.isNotEmpty(), loaded = true)
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompanyListResults())

    fun onQueryChange(query: String) {
        _filter.update { it.copy(query = query) }
    }

    fun clearQuery() = onQueryChange("")

    /** Checks the classification if it was unchecked, and unchecks it otherwise. */
    fun toggleClassification(classification: Classification) {
        _filter.update {
            val next = if (classification in it.classifications) {
                it.classifications - classification
            } else {
                it.classifications + classification
            }
            it.copy(classifications = next)
        }
    }
}
