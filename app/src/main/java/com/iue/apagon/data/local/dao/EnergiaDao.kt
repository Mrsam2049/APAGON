package com.iue.apagon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iue.apagon.data.local.entity.EnergiaDiariaEntity

@Dao
interface EnergiaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EnergiaDiariaEntity>)

    @Query("SELECT * FROM energia_diaria WHERE fuente = :fuente ORDER BY fecha DESC LIMIT :limit")
    suspend fun ultimosPorFuente(fuente: String, limit: Int): List<EnergiaDiariaEntity>

    @Query("SELECT * FROM energia_diaria ORDER BY fecha DESC LIMIT 1")
    suspend fun masReciente(): EnergiaDiariaEntity?
}
