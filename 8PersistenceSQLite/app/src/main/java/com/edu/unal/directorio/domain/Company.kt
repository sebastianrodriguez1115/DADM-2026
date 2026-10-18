package com.edu.unal.directorio.domain

/** A stored company. Optional text fields are empty strings when not provided. */
data class Company(
    val id: Long,
    val name: String,
    val email: String,
    val url: String,
    val phone: String,
    val products: String,
    val classification: Classification,
)
