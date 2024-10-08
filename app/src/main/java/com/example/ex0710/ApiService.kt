package com.example.ex0710

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit


data class TermResult(
    val term_ko: String,
    val term_en: String,
    val explanation: String
)

interface ApiService {
    @Multipart
    @POST("/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): List<TermResult>

    @FormUrlEncoded
    @POST("/sendUserId")
    suspend fun sendUserId(@Field("userId") userId: String): Response<Unit>

    @GET("/getMedicalTerms")
    suspend fun getMedicalTerms(): List<TermResult>

    companion object {
        private const val BASE_URL = "https://5a7f-211-183-33-254.ngrok-free.app"

        fun create(): ApiService {
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client = OkHttpClient.Builder()
                .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
                .connectTimeout(200, TimeUnit.SECONDS)
                .writeTimeout(200, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
