package com.iue.apagon.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Detalle de cada noche dentro de una partida. Permite reconstruir el reporte nocturno
 * y la curva de indicadores por noche.
 */
@Entity(
    tableName = "noches",
    foreignKeys = [
        ForeignKey(
            entity = PartidaEntity::class,
            parentColumns = ["id"],
            childColumns = ["partidaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("partidaId")]
)
data class NocheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val partidaId: Long,
    val numeroNoche: Int,
    val energiaDisponibleMw: Double,
    val demandaTotalMw: Double,
    val coberturaPct: Double,
    val presupuesto: Double,
    val bienestar: Double,
    val ambiental: Double,
    val distritosApagadosCsv: String,   // ids de distritos apagados separados por coma
    val cartasJugadasCsv: String,
    val elNinoIntensity: Double
)
