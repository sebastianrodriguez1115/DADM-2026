package com.edu.unal.directorio.data

import com.edu.unal.directorio.domain.Company
import com.edu.unal.directorio.domain.CompanyDraft
import com.edu.unal.directorio.domain.CompanyFilter
import kotlinx.coroutines.flow.Flow

/** Outcome of [CompanyRepository.save]. Uniqueness is the only expected, non-exceptional failure. */
sealed interface SaveResult {
    /** The company was written; [id] is its id (new or the edited one). */
    data class Saved(val id: Long) : SaveResult

    /** Another company already has the same normalized name. Nothing was written. */
    data object NameTaken : SaveResult

    /** Another company already has the same normalized email. Nothing was written. */
    data object EmailTaken : SaveResult

    /** Both the name and the email are taken (by the same or different companies). */
    data object NameAndEmailTaken : SaveResult
}

/**
 * Storage of companies (design D6). Unexpected failures (I/O, missing company on edit) propagate
 * as exceptions; the ViewModel translates them into user-facing messages.
 */
interface CompanyRepository {
    /** Companies matching [filter], ordered by normalized name; re-emits when data changes. */
    fun observeCompanies(filter: CompanyFilter): Flow<List<Company>>

    /** The company with [id], or `null` when it does not exist (also after it is deleted). */
    fun observeCompany(id: Long): Flow<Company?>

    /**
     * Saves [draft] as a new company ([editingId] `null`) or as an edit of [editingId].
     * The caller is expected to have validated the draft; the repository trims it anyway.
     */
    suspend fun save(draft: CompanyDraft, editingId: Long?): SaveResult

    /** Deletes the company and its classifications. Deleting a missing id does nothing. */
    suspend fun delete(id: Long)
}
