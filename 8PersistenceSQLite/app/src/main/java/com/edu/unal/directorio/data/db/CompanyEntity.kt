package com.edu.unal.directorio.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Row of the `company` table (design D3).
 *
 * [nameKey] and [emailKey] hold the output of `normalizeKey` for [name] and [email]. Only the
 * repository writes them; the unique indices on both columns enforce case and accent
 * insensitive uniqueness in the database itself. [classificationId] references the fixed
 * `classification` catalog (required; no cascade, the catalog is never deleted).
 */
@Entity(
    tableName = "company",
    indices = [
        Index(value = ["name_key"], unique = true),
        Index(value = ["email_key"], unique = true),
        Index(value = ["classification_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ClassificationEntity::class,
            parentColumns = ["id"],
            childColumns = ["classification_id"],
        ),
    ],
)
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "name_key") val nameKey: String,
    val email: String,
    @ColumnInfo(name = "email_key") val emailKey: String,
    val url: String = "",
    val phone: String = "",
    val products: String = "",
    @ColumnInfo(name = "classification_id") val classificationId: Int,
)
