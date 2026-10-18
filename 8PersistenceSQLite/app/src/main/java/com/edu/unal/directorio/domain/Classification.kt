package com.edu.unal.directorio.domain

/**
 * Business classification of a company.
 *
 * The persisted value is the stable [code], never the enum name or the ordinal. Spanish labels
 * live in the UI layer (string resources), not here.
 */
enum class Classification(val code: String) {
    Consulting("CONSULTING"),
    CustomDevelopment("CUSTOM_DEVELOPMENT"),
    SoftwareFactory("SOFTWARE_FACTORY");

    companion object {
        /**
         * Returns the classification whose [code] matches exactly, or `null` when the code is
         * unknown. It never throws; callers decide how to treat unknown codes.
         */
        fun fromCode(code: String): Classification? = entries.firstOrNull { it.code == code }
    }
}
