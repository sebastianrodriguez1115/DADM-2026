package com.edu.unal.directorio.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

/**
 * Row of the `company_classification` table (design D3): one classification of one company.
 *
 * [classification] stores the stable `Classification.code`, never the enum name or ordinal.
 * Deleting the company deletes its rows here (`ON DELETE CASCADE`). The composite primary key
 * starts with `company_id`, so it also serves as the index for the foreign key.
 */
@Entity(
    tableName = "company_classification",
    primaryKeys = ["company_id", "classification"],
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["company_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["classification"])],
)
data class ClassificationEntity(
    @ColumnInfo(name = "company_id") val companyId: Long,
    val classification: String,
)
