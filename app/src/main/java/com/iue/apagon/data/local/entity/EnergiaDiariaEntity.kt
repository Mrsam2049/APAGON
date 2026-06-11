package com.iue.apagon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache local de los datos obtenidos de SiMEM y SINERGOX en el splash.
 * Permite jugar offline después del primer fetch.
 */
@Entity(tableName = "energia_diaria")
data class EnergiaDiariaEntity(
    @PrimaryKey
    val fecha: String,                  // ISO yyyy-MM-dd
    val fuente: String,                 // "SIMEM" | "SINERGOX"
    val valorMw: Double,                // generación/volumen útil
    val fetchedAtEpoch: Long
)
