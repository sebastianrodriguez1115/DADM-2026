package com.edu.unal.directorio.domain

/**
 * List filter: free [query] text (matched by substring on the normalized name) and the checked
 * [classifications] (a company matches when its single classification is one of them; empty means no filter).
 */
data class CompanyFilter(
    val query: String = "",
    val classifications: Set<Classification> = emptySet(),
)
