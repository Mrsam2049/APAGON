package com.iue.apagon.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Respuesta de SINERGOX (/daily). Estructura típica de la API de XM:
 *
 * {
 *   "Items": [
 *     {
 *       "MetricId": "VoluUtilDiarEner",
 *       "MetricUnits": "GWh",
 *       "DailyEntities": [
 *         { "Id": "Sistema", "Date": "2024-06-01T00:00:00-05:00", "Values": { "Value": 12345.6 } }
 *       ]
 *     }
 *   ]
 * }
 *
 * Nota: para métricas diarias (como VoluUtilDiarEner) "Values" trae un único "Value".
 */
data class XmResponse(
    @SerializedName("Items") val items: List<XmItem> = emptyList()
) {
    /** (fecha, valor) del registro diario más reciente con valor no nulo, o null si no hay datos. */
    fun latestValue(): Pair<String, Double>? {
        val entity = items
            .flatMap { it.dailyEntities }
            .lastOrNull { it.date != null && it.values?.value != null }
            ?: return null
        return entity.date!! to entity.values!!.value!!
    }
}

data class XmItem(
    @SerializedName("MetricId") val metricId: String? = null,
    @SerializedName("MetricName") val metricName: String? = null,
    @SerializedName("MetricUnits") val units: String? = null,
    @SerializedName("Entity") val entity: String? = null,
    @SerializedName("DailyEntities") val dailyEntities: List<XmDailyEntity> = emptyList()
)

data class XmDailyEntity(
    @SerializedName("Id") val id: String? = null,
    @SerializedName("Date") val date: String? = null,
    @SerializedName("Values") val values: XmValues? = null
)

data class XmValues(
    @SerializedName("Value") val value: Double? = null
)
