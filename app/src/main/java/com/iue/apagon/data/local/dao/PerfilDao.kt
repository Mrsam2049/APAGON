package com.iue.apagon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.iue.apagon.data.local.entity.PerfilEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilDao {

    /** Perfil observable (siempre la fila id = 1). */
    @Query("SELECT * FROM perfil WHERE id = 1")
    fun get(): Flow<PerfilEntity?>

    @Query("SELECT * FROM perfil WHERE id = 1")
    suspend fun getOnce(): PerfilEntity?

    /** Crea la fila por defecto si aún no existe. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefault(perfil: PerfilEntity)

    /** Suma (o resta, si es negativo) Vatios al total. */
    @Query("UPDATE perfil SET vatiosTotales = vatiosTotales + :cant WHERE id = 1")
    suspend fun addVatios(cant: Int)

    /** Persiste un desbloqueo (la lógica de validación/CSV vive en el repositorio). */
    @Update
    suspend fun update(perfil: PerfilEntity)
}
