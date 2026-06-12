package com.iue.apagon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Estado persistente de un logro. [id] coincide con Logro.name.
 */
@Entity(tableName = "logros")
data class LogroEntity(
    @PrimaryKey val id: String,
    val desbloqueado: Boolean = false
)
