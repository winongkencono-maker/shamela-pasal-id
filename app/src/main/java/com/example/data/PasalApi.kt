package com.example.data

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

interface PasalApiService {
    @GET("search")
    suspend fun search(
        @Header("Authorization") authorization: String,
        @Query("q") query: String,
        @Query("type") type: String? = null,
        @Query("limit") limit: Int? = null
    ): PasalSearchResponse

    @GET("laws")
    suspend fun getLaws(
        @Header("Authorization") authorization: String,
        @Query("type") type: String? = null,
        @Query("year") year: Int? = null,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): PasalListResponse

    @GET("laws/{frbr_uri}")
    suspend fun getLawDetail(
        @Header("Authorization") authorization: String,
        @Path(value = "frbr_uri", encoded = true) frbrUri: String
    ): PasalDetailResponse
}

object PasalApiClient {
    private const val BASE_URL = "https://pasal.id/api/v1/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: PasalApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PasalApiService::class.java)
    }

    fun getAuthHeader(token: String? = null): String {
        val actualToken = token?.trim()?.takeIf { it.isNotEmpty() }
            ?: run {
                try {
                    // Access BuildConfig dynamically via reflection or direct call
                    com.example.BuildConfig.PASAL_API_TOKEN
                } catch (e: Throwable) {
                    ""
                }
            }.trim().takeIf { it.isNotEmpty() }
            ?: "pasal_mcp_f60e69426452_591e107a1f56a22261db5f80f5be255f3b4fc701481878d8"

        return if (actualToken.startsWith("Bearer ", ignoreCase = true)) {
            actualToken
        } else {
            "Bearer $actualToken"
        }
    }
}
