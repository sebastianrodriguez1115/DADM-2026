package com.edu.unal.directorio.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

// Tipo mínimo de prueba de build (tarea 1.2); el grupo 3 lo reemplaza.
@Dao
interface CompanyDao {
    @Insert
    suspend fun insert(company: CompanyEntity): Long

    @Query("SELECT COUNT(*) FROM company")
    suspend fun count(): Int

    @Query("SELECT * FROM company ORDER BY name")
    fun observeAll(): Flow<List<CompanyEntity>>
}
