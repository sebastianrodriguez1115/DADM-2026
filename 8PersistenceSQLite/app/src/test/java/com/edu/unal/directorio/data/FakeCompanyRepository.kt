package com.edu.unal.directorio.data

import com.edu.unal.directorio.domain.Company
import com.edu.unal.directorio.domain.CompanyDraft
import com.edu.unal.directorio.domain.CompanyFilter
import com.edu.unal.directorio.domain.normalizeKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [CompanyRepository] for JVM tests (design D10). It mirrors [RoomCompanyRepository]:
 * trimmed fields, uniqueness by `normalizeKey` excluding the edited company, results ordered by
 * normalized name then id, and the D7 filter (substring of the normalized name, union of the
 * checked classifications, empty set means no classification filter).
 *
 * Failure switches: set [failNextSave] / [failNextDelete] to make the next call throw
 * [failure] once (the switch resets itself). A failed call changes nothing.
 */
class FakeCompanyRepository(initial: List<CompanyDraft> = emptyList()) : CompanyRepository {
    private val companies = MutableStateFlow<List<Company>>(emptyList())
    private var nextId = 1L

    /** When `true`, the next [save] throws [failure] and the flag goes back to `false`. */
    var failNextSave = false

    /** When `true`, the next [delete] throws [failure] and the flag goes back to `false`. */
    var failNextDelete = false

    /** Exception thrown by a switched failure. */
    var failure: Exception = IllegalStateException("Simulated storage failure")

    /** Number of [save] calls that reached the storage logic (not counting switched failures). */
    var saveCalls = 0
        private set

    init {
        initial.forEach { check(saveBlocking(it, null) is SaveResult.Saved) { "Duplicate initial company" } }
    }

    /** Snapshot of the stored companies in storage order, for assertions. */
    val current: List<Company> get() = companies.value

    override fun observeCompanies(filter: CompanyFilter): Flow<List<Company>> {
        val query = normalizeKey(filter.query)
        return companies.map { list ->
            list.filter { company ->
                query in normalizeKey(company.name) &&
                    (filter.classifications.isEmpty() ||
                        company.classifications.any { it in filter.classifications })
            }.sortedWith(compareBy({ normalizeKey(it.name) }, { it.id }))
        }
    }

    override fun observeCompany(id: Long): Flow<Company?> =
        companies.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun save(draft: CompanyDraft, editingId: Long?): SaveResult =
        saveBlocking(draft, editingId)

    private fun saveBlocking(draft: CompanyDraft, editingId: Long?): SaveResult {
        if (failNextSave) {
            failNextSave = false
            throw failure
        }
        saveCalls++
        require(draft.classifications.isNotEmpty()) { "A company needs at least one classification" }
        val name = draft.name.trim()
        val email = draft.email.trim()
        val list = companies.value
        if (editingId != null && list.none { it.id == editingId }) {
            throw NoSuchElementException("Company $editingId does not exist")
        }
        val nameTaken = list.any { it.id != editingId && normalizeKey(it.name) == normalizeKey(name) }
        val emailTaken = list.any { it.id != editingId && normalizeKey(it.email) == normalizeKey(email) }
        when {
            nameTaken && emailTaken -> return SaveResult.NameAndEmailTaken
            nameTaken -> return SaveResult.NameTaken
            emailTaken -> return SaveResult.EmailTaken
        }
        val id = editingId ?: nextId++
        val company = Company(
            id = id,
            name = name,
            email = email,
            url = draft.url.trim(),
            phone = draft.phone.trim(),
            products = draft.products.trim(),
            classifications = draft.classifications,
        )
        companies.value = list.filter { it.id != id } + company
        return SaveResult.Saved(id)
    }

    override suspend fun delete(id: Long) {
        if (failNextDelete) {
            failNextDelete = false
            throw failure
        }
        companies.value = companies.value.filter { it.id != id }
    }
}
