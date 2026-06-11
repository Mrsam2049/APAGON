package com.iue.apagon.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Cuerpo del POST a SINERGOX (https://servapibi.xm.com.co/daily).
 * Las claves van en PascalCase tal como las espera la API de XM.
 *
 * Ejemplo: { "MetricId": "VoluUtilDiarEner", "StartDate": "...", "EndDate": "...", "Entity": "Sistema" }
 */
data class XmRequest(
    @SerializedName("MetricId") val metricId: String,
    @SerializedName("StartDate") val startDate: String,
    @SerializedName("EndDate") val endDate: String,
    @SerializedName("Entity") val entity: String
)
