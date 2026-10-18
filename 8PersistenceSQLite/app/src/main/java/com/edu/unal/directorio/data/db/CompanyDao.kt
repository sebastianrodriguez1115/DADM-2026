package com.edu.unal.directorio.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data access for companies and the classification catalog (design D3, D4, D7).
 *
 * The DAO does no text processing: callers pass keys already built with `normalizeKey` and, for
 * LIKE patterns, already escaped with `escapeLike`.
 */
@Dao
abstract class CompanyDao {

    /**
     * Companies whose `name_key` contains [query] and, when [noClassFilter] is 0, whose
     * `classification_id` is one of [classificationIds]. Ordered by `name_key`, then `id`.
     *
     * [query] must be `escapeLike(normalizeKey(text))`; the empty string matches everything.
     *
     * Empty-list decision: no sentinel is used. With no chips pass `noClassFilter = 1` and an
     * EMPTY [classificationIds] list: Room 3 expands it to `IN ()`, which SQLite accepts (an empty
     * `IN` list is simply false), and the `noClassFilter = 1` branch makes the whole condition
     * true. Verified by CompanyDaoTest.emptyChipListWithNoClassFilterReturnsEverything. With
     * chips pass `noClassFilter = 0` and the ids of the marked classifications.
     */
    @Query(
        """
        SELECT * FROM company
        WHERE name_key LIKE '%' || :query || '%' ESCAPE '\'
          AND (:noClassFilter = 1 OR classification_id IN (:classificationIds))
        ORDER BY name_key, id
        """,
    )
    abstract fun observeFiltered(
        query: String,
        noClassFilter: Int,
        classificationIds: List<Int>,
    ): Flow<List<CompanyEntity>>

    /** Emits the company with [id], or `null` when it does not exist (or is deleted later). */
    @Query("SELECT * FROM company WHERE id = :id")
    abstract fun observeById(id: Long): Flow<CompanyEntity?>

    /** Returns the id of the company whose `name_key` equals [nameKey], or `null`. */
    @Query("SELECT id FROM company WHERE name_key = :nameKey")
    abstract suspend fun findIdByNameKey(nameKey: String): Long?

    /** Returns the id of the company whose `email_key` equals [emailKey], or `null`. */
    @Query("SELECT id FROM company WHERE email_key = :emailKey")
    abstract suspend fun findIdByEmailKey(emailKey: String): Long?

    /** Inserts a company and returns its new id. Fails on a duplicate `name_key` or `email_key`. */
    @Insert
    abstract suspend fun insert(company: CompanyEntity): Long

    /** Updates the company with the same id as [company]. Returns the number of rows changed. */
    @Update
    abstract suspend fun update(company: CompanyEntity): Int

    /** Deletes the company; the catalog is untouched. Returns rows deleted. */
    @Query("DELETE FROM company WHERE id = :id")
    abstract suspend fun deleteById(id: Long): Int

    /** The catalog rows ordered by `id`. */
    @Query("SELECT * FROM classification ORDER BY id")
    abstract suspend fun getClassifications(): List<ClassificationEntity>
}
