package com.iue.apagon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.iue.apagon.data.local.entity.NocheEntity
import com.iue.apagon.data.local.entity.PartidaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartidaDao {

    @Insert
    suspend fun insertPartida(partida: PartidaEntity): Long

    @Insert
    suspend fun insertNoche(noche: NocheEntity): Long

    @Insert
    suspend fun insertNoches(noches: List<NocheEntity>)

    @Query("SELECT * FROM partidas ORDER BY fechaFinEpoch DESC")
    fun observarHistorial(): Flow<List<PartidaEntity>>

    @Query("SELECT * FROM partidas WHERE id = :id")
    suspend fun getPartida(id: Long): PartidaEntity?

    @Query("SELECT * FROM noches WHERE partidaId = :partidaId ORDER BY numeroNoche ASC")
    suspend fun getNochesDePartida(partidaId: Long): List<NocheEntity>

    @Query("DELETE FROM partidas WHERE id = :id")
    suspend fun deletePartida(id: Long)
}
