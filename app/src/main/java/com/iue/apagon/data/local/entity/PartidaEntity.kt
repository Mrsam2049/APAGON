package com.iue.apagon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registro histórico de una partida jugada (campaña o supervivencia).
 * Se inserta al terminar la partida y se muestra en HistorialActivity.
 */
@Entity(tableName = "partidas")
data class PartidaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val modo: String,                  // GameMode.name
    val municipio: String,             // Municipio.name
    val fechaInicioEpoch: Long,        // System.currentTimeMillis() al iniciar
    val fechaFinEpoch: Long,           // al terminar
    val nochesCompletadas: Int,
    val victoria: Boolean,
    val razonFin: String?,             // GameOverReason.name o null si victoria
    val coberturaFinal: Double,
    val presupuestoFinal: Double,
    val bienestarFinal: Double,
    val ambientalFinal: Double,
    val puntajeFinal: Int
)
