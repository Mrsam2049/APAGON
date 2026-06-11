package com.iue.apagon.data.remote

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Proveedor manual de red (sin Hilt). Expone dos servicios Retrofit creados a partir de
 * dos Retrofit distintos, uno por cada base URL (XM y SiMEM están en hosts diferentes).
 * Las instancias se crean perezosamente y se comparten en todo el proceso.
 */
object NetworkModule {

    private const val XM_BASE_URL = "https://servapibi.xm.com.co/"
    private const val SIMEM_BASE_URL = "https://www.simem.co/"

    private val gson = GsonBuilder().setLenient().create()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
            .build()
    }

    private fun buildRetrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    /** Servicio para servapibi.xm.com.co (SINERGOX). */
    val xmApi: XmApiService by lazy {
        buildRetrofit(XM_BASE_URL).create(XmApiService::class.java)
    }

    /** Servicio para simem.co (SiMEM PublicData). */
    val simemApi: SimemApiService by lazy {
        buildRetrofit(SIMEM_BASE_URL).create(SimemApiService::class.java)
    }
}
