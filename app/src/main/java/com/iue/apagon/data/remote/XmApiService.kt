package com.iue.apagon.data.remote

import com.iue.apagon.data.remote.dto.SimemResponse
import com.iue.apagon.data.remote.dto.XmRequest
import com.iue.apagon.data.remote.dto.XmResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * SINERGOX / API XM. Base URL: https://servapibi.xm.com.co/
 */
interface XmApiService {
    @POST("daily")
    suspend fun getDailyMetric(@Body request: XmRequest): XmResponse
}

/**
 * SiMEM PublicData. Base URL: https://www.simem.co/
 * (Va en este mismo archivo por pedido, pero usa un Retrofit distinto por tener otra base URL.)
 */
interface SimemApiService {
    @GET("backend-files/api/PublicData")
    suspend fun getGeneracionReal(
        @Query("datasetid") datasetId: String,
        @Query("startdate") startDate: String? = null,
        @Query("enddate") endDate: String? = null
    ): SimemResponse
}
