package com.iue.apagon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iue.apagon.data.local.entity.LogroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogroDao {

    /** Todos los logros (para la grilla de la pantalla de logros). */
    @Query("SELECT * FROM logros")
    fun observe(): Flow<List<LogroEntity>>

    @Query("SELECT id FROM logros WHERE desbloqueado = 1")
    suspend fun getDesbloqueadosIds(): List<String>

    /** Siembra los logros (no pisa los ya existentes). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(logros: List<LogroEntity>)

    @Query("UPDATE logros SET desbloqueado = 1 WHERE id = :id")
    suspend fun marcarDesbloqueado(id: String)
}
