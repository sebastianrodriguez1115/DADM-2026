package com.edu.unal.directorio.data.db

import androidx.room3.Database
import androidx.room3.RoomDatabase

// Tipo mínimo de prueba de build (tarea 1.2); el grupo 3 lo reemplaza.
@Database(entities = [CompanyEntity::class], version = 1, exportSchema = false)
abstract class DirectorioDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao
}
