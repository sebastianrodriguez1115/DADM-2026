package com.edu.unal.directorio.domain

/** Company data as entered in the form: a [Company] without an `id`.
 * The classification is `null` until one is chosen. */
data class CompanyDraft(
    val name: String = "",
    val email: String = "",
    val url: String = "",
    val phone: String = "",
    val products: String = "",
    val classification: Classification? = null,
)
