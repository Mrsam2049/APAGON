package com.iue.apagon.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Respuesta de SiMEM PublicData (dataset E17D25). Estructura típica:
 *
 * {
 *   "success": true,
 *   "result": {
 *     "name": "...",
 *     "records": [ { "FechaHora": "...", "Valor": 123.4, "CodigoVariable": "..." } ]
 *   }
 * }
 *
 * Los nombres de campo dentro de "records" dependen del dataset; aquí se cubren los más
 * comunes (Fecha/FechaHora/Valor). Ajustar si E17D25 usa otras claves.
 */
data class SimemResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("result") val result: SimemResult? = null
) {
    /** (fecha, valor) del registro más reciente con valor no nulo, o null si no hay datos. */
    fun latestValue(): Pair<String, Double>? {
        val record = result?.records.orEmpty()
            .lastOrNull { it.value() != null && it.date() != null }
            ?: return null
        return record.date()!! to record.value()!!
    }
}

data class SimemResult(
    @SerializedName("name") val name: String? = null,
    @SerializedName("records") val records: List<SimemRecord> = emptyList()
)

data class SimemRecord(
    @SerializedName("Fecha") val fecha: String? = null,
    @SerializedName("FechaHora") val fechaHora: String? = null,
    @SerializedName("Valor") val valor: Double? = null,
    @SerializedName("CodigoVariable") val codigoVariable: String? = null
) {
    fun date(): String? = fechaHora ?: fecha
    fun value(): Double? = valor
}
