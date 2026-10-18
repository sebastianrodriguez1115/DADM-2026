package com.edu.unal.directorio.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.unal.directorio.data.CompanyRepository
import com.edu.unal.directorio.data.SaveResult
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.domain.ClassificationError
import com.edu.unal.directorio.domain.CompanyDraft
import com.edu.unal.directorio.domain.CompanyValidator
import com.edu.unal.directorio.domain.EmailError
import com.edu.unal.directorio.domain.NameError
import com.edu.unal.directorio.domain.PhoneError
import com.edu.unal.directorio.domain.ProductsError
import com.edu.unal.directorio.domain.UrlError
import com.edu.unal.directorio.domain.ValidationErrors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Errors shown in the form: the domain [validation] codes plus the two uniqueness errors, which
 * only the storage can detect (design D5). Each field has at most one error.
 */
data class CompanyFormErrors(
    val validation: ValidationErrors = ValidationErrors(),
    val nameTaken: Boolean = false,
    val emailTaken: Boolean = false,
) {
    val name: NameError? get() = validation.name
    val email: EmailError? get() = validation.email
    val url: UrlError? get() = validation.url
    val phone: PhoneError? get() = validation.phone
    val products: ProductsError? get() = validation.products
    val classification: ClassificationError? get() = validation.classification
}

/** Everything the form screen draws. Field texts are exactly as typed (not trimmed). */
data class CompanyFormUiState(
    /** `true` when editing an existing company (title "Editar empresa"). */
    val isEditing: Boolean = false,
    /** `true` while the company to edit is being loaded; saving is not possible meanwhile. */
    val isLoading: Boolean = false,
    val name: String = "",
    val email: String = "",
    val url: String = "",
    val phone: String = "",
    val products: String = "",
    val classifications: Set<Classification> = emptySet(),
    val errors: CompanyFormErrors = CompanyFormErrors(),
    /** `true` from the moment "Guardar" is accepted; the button is disabled while it is. */
    val isSaving: Boolean = false,
    /** `true` after an unexpected storage failure; the typed values are kept. */
    val saveFailed: Boolean = false,
)

/**
 * One-shot signals for the screen. They are NOT part of [CompanyFormUiState] so that they cannot
 * be replayed by a recomposition or a rotation.
 */
sealed interface CompanyFormEvent {
    /** The company was saved; the navigation decides the destination (`onSaved()`). */
    data class Saved(val id: Long) : CompanyFormEvent

    /** The company being edited does not exist (anymore); go back to the list (`onDeleted()`). */
    data object CompanyNotFound : CompanyFormEvent
}

/**
 * ViewModel of the create/edit form (design D6). It is Activity-scoped and reused between
 * openings, so [open] resets the whole state each time.
 *
 * Signals: [events] is backed by a buffered [Channel] exposed as a flow, so each event is
 * delivered exactly once to the (single) collector; the screen consumes it in a `LaunchedEffect`.
 * An event emitted while nobody collects (for example during a rotation) waits in the channel
 * and is delivered to the next collector, and is never repeated after being consumed.
 */
class CompanyFormViewModel(private val repository: CompanyRepository) : ViewModel() {
    private val _state = MutableStateFlow(CompanyFormUiState())
    val state: StateFlow<CompanyFormUiState> = _state.asStateFlow()

    private val _events = Channel<CompanyFormEvent>(Channel.BUFFERED)
    val events: Flow<CompanyFormEvent> = _events.receiveAsFlow()

    private var editingId: Long? = null
    private var loadJob: Job? = null
    private var saveJob: Job? = null

    /**
     * Starts a new form session: `null` for a blank create form, or the id of the company to
     * edit. Discards any previous state, errors and in-flight work. If the company does not exist,
     * emits [CompanyFormEvent.CompanyNotFound].
     */
    fun open(id: Long?) {
        loadJob?.cancel()
        saveJob?.cancel()
        // Drop signals nobody consumed from a previous session so they cannot leak into this one.
        while (_events.tryReceive().isSuccess) { /* discard */ }
        editingId = id
        if (id == null) {
            _state.value = CompanyFormUiState()
            return
        }
        _state.value = CompanyFormUiState(isEditing = true, isLoading = true)
        loadJob = viewModelScope.launch {
            val company = repository.observeCompany(id).first()
            if (company == null) {
                _events.send(CompanyFormEvent.CompanyNotFound)
            } else {
                _state.value = CompanyFormUiState(
                    isEditing = true,
                    name = company.name,
                    email = company.email,
                    url = company.url,
                    phone = company.phone,
                    products = company.products,
                    classifications = company.classifications,
                )
            }
        }
    }

    fun onNameChange(value: String) = _state.update {
        it.copy(name = value, errors = it.errors.copy(validation = it.errors.validation.copy(name = null), nameTaken = false))
    }

    fun onEmailChange(value: String) = _state.update {
        it.copy(email = value, errors = it.errors.copy(validation = it.errors.validation.copy(email = null), emailTaken = false))
    }

    fun onUrlChange(value: String) = _state.update {
        it.copy(url = value, errors = it.errors.copy(validation = it.errors.validation.copy(url = null)))
    }

    fun onPhoneChange(value: String) = _state.update {
        it.copy(phone = value, errors = it.errors.copy(validation = it.errors.validation.copy(phone = null)))
    }

    fun onProductsChange(value: String) = _state.update {
        it.copy(products = value, errors = it.errors.copy(validation = it.errors.validation.copy(products = null)))
    }

    /** Checks or unchecks [classification] and clears the classification error. */
    fun onToggleClassification(classification: Classification) = _state.update {
        val updated = if (classification in it.classifications) {
            it.classifications - classification
        } else {
            it.classifications + classification
        }
        it.copy(
            classifications = updated,
            errors = it.errors.copy(validation = it.errors.validation.copy(classification = null)),
        )
    }

    /**
     * Validates all fields at once and, if valid, saves the trimmed draft. A call while another
     * save is running (double tap) is ignored.
     */
    fun save() {
        val current = _state.value
        if (current.isSaving || current.isLoading) return
        val result = CompanyValidator.validate(
            CompanyDraft(
                name = current.name,
                email = current.email,
                url = current.url,
                phone = current.phone,
                products = current.products,
                classifications = current.classifications,
            ),
        )
        if (!result.isValid) {
            _state.update { it.copy(errors = CompanyFormErrors(validation = result.errors), saveFailed = false) }
            return
        }
        // Set synchronously, before launching, so a second tap sees isSaving = true.
        _state.update { it.copy(errors = CompanyFormErrors(), isSaving = true, saveFailed = false) }
        saveJob = viewModelScope.launch {
            try {
                when (val saved = repository.save(result.draft, editingId)) {
                    is SaveResult.Saved -> {
                        // isSaving stays true: the form is about to close and must not save twice.
                        _events.send(CompanyFormEvent.Saved(saved.id))
                    }
                    SaveResult.NameTaken -> rejected(nameTaken = true, emailTaken = false)
                    SaveResult.EmailTaken -> rejected(nameTaken = false, emailTaken = true)
                    SaveResult.NameAndEmailTaken -> rejected(nameTaken = true, emailTaken = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    private fun rejected(nameTaken: Boolean, emailTaken: Boolean) = _state.update {
        it.copy(
            isSaving = false,
            errors = CompanyFormErrors(nameTaken = nameTaken, emailTaken = emailTaken),
        )
    }
}
