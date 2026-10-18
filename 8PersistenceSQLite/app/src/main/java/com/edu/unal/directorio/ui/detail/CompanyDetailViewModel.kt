package com.edu.unal.directorio.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.unal.directorio.data.CompanyRepository
import com.edu.unal.directorio.domain.Company
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the detail shows for the opened company. */
sealed interface CompanyDetailContent {
    /** Nothing was received yet (initial state, or just after opening another company). */
    data object Loading : CompanyDetailContent

    data class Loaded(val company: Company) : CompanyDetailContent

    /** The id does not exist or the company was deleted; a back-to-list signal is emitted. */
    data object NotFound : CompanyDetailContent
}

/** Everything the detail screen draws. */
data class CompanyDetailUiState(
    val content: CompanyDetailContent = CompanyDetailContent.Loading,
    /** `true` while the "¿Eliminar empresa?" dialog is open. */
    val deleteDialogVisible: Boolean = false,
    /** `true` after a failed deletion; the company is still stored. */
    val deleteFailed: Boolean = false,
)

/** One-shot signals for the screen (not part of [CompanyDetailUiState] so they never replay). */
sealed interface CompanyDetailEvent {
    /** The company was deleted or no longer exists; the navigation goes to the list (`onDeleted()`). */
    data object BackToList : CompanyDetailEvent
}

/**
 * ViewModel of the detail screen (design D6, D9). It is Activity-scoped and reused between
 * openings, so [open] switches company and resets the dialog and the failure.
 *
 * Observation: the company is collected only while [state] has subscribers
 * (`WhileSubscribed(5_000)`), i.e. while the screen is visible.
 *
 * Single navigation: after a confirmed delete the repository also emits `null`. Both that and the
 * success of `delete` want to leave the screen, so [signalBackToList] sends the event at most once
 * per [open] session (guarded by [leaveSignaled]), whichever of the two comes first.
 *
 * Signals: [events] is a buffered [Channel] exposed with `receiveAsFlow()` (same pattern as the
 * form), consumed once by the single collector; unconsumed signals are dropped by [open].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CompanyDetailViewModel(private val repository: CompanyRepository) : ViewModel() {
    private val companyId = MutableStateFlow<Long?>(null)
    private val dialogVisible = MutableStateFlow(false)
    private val deleteFailed = MutableStateFlow(false)

    private val _events = Channel<CompanyDetailEvent>(Channel.BUFFERED)
    val events: Flow<CompanyDetailEvent> = _events.receiveAsFlow()

    private var leaveSignaled = false

    private val content: Flow<CompanyDetailContent> = companyId.flatMapLatest { id ->
        if (id == null) {
            flowOf(CompanyDetailContent.Loading)
        } else {
            repository.observeCompany(id)
                .map { company ->
                    if (company == null) CompanyDetailContent.NotFound else CompanyDetailContent.Loaded(company)
                }
                .onEach { if (it is CompanyDetailContent.NotFound) signalBackToList() }
                // A switch of company shows "loading", never the previous company or "not found".
                .onStart { emit(CompanyDetailContent.Loading) }
        }
    }

    val state: StateFlow<CompanyDetailUiState> =
        combine(content, dialogVisible, deleteFailed) { c, dialog, failed ->
            CompanyDetailUiState(c, dialog, failed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompanyDetailUiState())

    /** Starts a new detail session for [id]; resets the dialog, the failure and pending signals. */
    fun open(id: Long) {
        while (_events.tryReceive().isSuccess) { /* discard signals of the previous session */ }
        leaveSignaled = false
        dialogVisible.value = false
        deleteFailed.value = false
        companyId.value = id
    }

    /** Opens the confirmation dialog. Deletes nothing. */
    fun requestDelete() {
        deleteFailed.value = false
        dialogVisible.value = true
    }

    /** Closes the dialog without deleting ("Cancelar" and Back). */
    fun dismissDelete() {
        dialogVisible.value = false
    }

    /**
     * Deletes the company and signals back to the list. A call with the dialog closed (for example
     * a second tap) is ignored. On failure the company stays and [CompanyDetailUiState.deleteFailed] is set.
     */
    fun confirmDelete() {
        val id = companyId.value ?: return
        if (!dialogVisible.value) return
        // Closed synchronously so a second tap sees it closed.
        dialogVisible.value = false
        viewModelScope.launch {
            try {
                repository.delete(id)
                if (companyId.value == id) signalBackToList()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (companyId.value == id) deleteFailed.update { true }
            }
        }
    }

    private fun signalBackToList() {
        if (leaveSignaled) return
        leaveSignaled = true
        _events.trySend(CompanyDetailEvent.BackToList)
    }
}
