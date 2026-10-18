package com.edu.unal.directorio.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import com.edu.unal.directorio.data.db.CompanyEntity
import com.edu.unal.directorio.data.db.DirectorioDatabase
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.domain.Company
import com.edu.unal.directorio.domain.CompanyDraft
import com.edu.unal.directorio.domain.CompanyFilter
import com.edu.unal.directorio.domain.escapeLike
import com.edu.unal.directorio.domain.normalizeKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [CompanyRepository] backed by Room (design D4, D6).
 *
 * Transaction: the uniqueness check and the single company write run inside
 * `db.useWriterConnection { it.immediateTransaction { ... } }`. The DAO calls made inside join
 * that transaction, so a failure anywhere rolls back everything.
 */
class RoomCompanyRepository(private val db: DirectorioDatabase) : CompanyRepository {
    private val dao = db.companyDao()

    override fun observeCompanies(filter: CompanyFilter): Flow<List<Company>> =
        dao.observeFiltered(
            query = escapeLike(normalizeKey(filter.query)),
            noClassFilter = if (filter.classifications.isEmpty()) 1 else 0,
            classificationIds = filter.classifications.map { it.id },
        ).map { rows -> rows.map { it.toCompany() } }

    override fun observeCompany(id: Long): Flow<Company?> =
        dao.observeById(id).map { it?.toCompany() }

    /**
     * Rules:
     * - A draft without classification is rejected with [IllegalArgumentException] before touching
     *   the database (the form validator normally prevents it; this is the storage-level guard).
     * - Editing an [editingId] that no longer exists throws [NoSuchElementException] and rolls
     *   back; the form shows its generic "could not save" message.
     * - A uniqueness violation raised by the database itself (the pre-check makes it nearly
     *   impossible) is translated to [SaveResult.NameTaken] / [SaveResult.EmailTaken].
     *   Any other exception propagates.
     */
    override suspend fun save(draft: CompanyDraft, editingId: Long?): SaveResult {
        val classification = requireNotNull(draft.classification) { "A company needs a classification" }
        val name = draft.name.trim()
        val email = draft.email.trim()
        val nameKey = normalizeKey(name)
        val emailKey = normalizeKey(email)

        return try {
            db.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    val nameOwner = dao.findIdByNameKey(nameKey)
                    val emailOwner = dao.findIdByEmailKey(emailKey)
                    val nameTaken = nameOwner != null && nameOwner != editingId
                    val emailTaken = emailOwner != null && emailOwner != editingId
                    when {
                        nameTaken && emailTaken -> SaveResult.NameAndEmailTaken
                        nameTaken -> SaveResult.NameTaken
                        emailTaken -> SaveResult.EmailTaken
                        else -> {
                            val entity = CompanyEntity(
                                id = editingId ?: 0,
                                name = name,
                                nameKey = nameKey,
                                email = email,
                                emailKey = emailKey,
                                url = draft.url.trim(),
                                phone = draft.phone.trim(),
                                products = draft.products.trim(),
                                classificationId = classification.id,
                            )
                            val id = if (editingId == null) {
                                dao.insert(entity)
                            } else {
                                if (dao.update(entity) == 0) {
                                    throw NoSuchElementException("Company $editingId does not exist")
                                }
                                editingId
                            }
                            SaveResult.Saved(id)
                        }
                    }
                }
            }
        } catch (e: SQLiteConstraintException) {
            toSaveResult(e) ?: throw e
        }
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}

/**
 * Translates the exception the database throws on a duplicate key into a [SaveResult], by the
 * column named in its message ("UNIQUE constraint failed: company.name_key"). Returns `null` for
 * any other constraint failure, which the caller must rethrow.
 */
internal fun toSaveResult(e: SQLiteConstraintException): SaveResult? {
    val message = e.message.orEmpty()
    val name = "company.name_key" in message
    val email = "company.email_key" in message
    return when {
        name && email -> SaveResult.NameAndEmailTaken
        name -> SaveResult.NameTaken
        email -> SaveResult.EmailTaken
        else -> null
    }
}

/** Maps a row to the domain; `classification_id` is resolved with [Classification.fromId]. */
private fun CompanyEntity.toCompany(): Company = Company(
    id = id,
    name = name,
    email = email,
    url = url,
    phone = phone,
    products = products,
    classification = Classification.fromId(classificationId),
)
