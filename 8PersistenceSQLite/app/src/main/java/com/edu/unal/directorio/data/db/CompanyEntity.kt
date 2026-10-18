package com.edu.unal.directorio.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Row of the `company` table (design D3).
 *
 * [nameKey] and [emailKey] hold the output of `normalizeKey` for [name] and [email]. Only the
 * repository writes them; the unique indices on both columns enforce case and accent
 * insensitive uniqueness in the database itself.
 */
@Entity(
    tableName = "company",
    indices = [
        Index(value = ["name_key"], unique = true),
        Index(value = ["email_key"], unique = true),
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
)
