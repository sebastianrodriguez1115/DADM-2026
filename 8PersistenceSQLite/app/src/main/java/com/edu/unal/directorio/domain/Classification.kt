package com.edu.unal.directorio.domain

/**
 * Business classification of a company; a company has exactly one.
 *
 * The enum is the single source of the fixed `id`/`code` pairs of the `classification` catalog
 * table. The persisted value in `company.classification_id` is the stable [id], never the enum
 * name or the ordinal. Spanish labels live in the UI layer (string resources), not here.
 */
enum class Classification(val id: Int, val code: String) {
    Consulting(1, "CONSULTING"),
    CustomDevelopment(2, "CUSTOM_DEVELOPMENT"),
    SoftwareFactory(3, "SOFTWARE_FACTORY");

    companion object {
        /** Returns the classification with this [id]; throws [IllegalArgumentException] if unknown. */
        fun fromId(id: Int): Classification =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Unknown classification id: $id")
    }
}
