package com.iue.apagon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Perfil persistente del jugador (fila única, id = 1). Guarda la moneda (Vatios) y todo lo
 * desbloqueado. Los conjuntos se serializan como CSV de ids.
 */
@Entity(tableName = "perfil")
data class PerfilEntity(
    @PrimaryKey val id: Int = 1,
    val vatiosTotales: Int = 0,
    val cartasDesbloqueadas: String = "",                 // CSV de ids de carta
    val municipiosDesbloqueados: String = "apartado",     // CSV; el primero viene gratis
    val mejorasCompradas: String = ""                     // CSV de ids de mejora
)
