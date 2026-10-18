package com.edu.unal.directorio.data.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

// Tipo mínimo de prueba de build (tarea 1.2); el grupo 3 lo reemplaza.
@Entity(tableName = "company")
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)
