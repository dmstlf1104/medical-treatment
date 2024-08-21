package com.example.ex0710

import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Streaming

data class ChatRequest(
    val message: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val chat_history: List<String> = emptyList(),
    val user_id: String,
    val max_new_tokens: Int = 1024,
    val temperature: Double = 0.8,
    val top_p: Double = 0.95,
    val top_k: Int = 50,
    val repetition_penalty: Double = 1.2
)

interface ChatBotService {
    @Headers("Content-Type: application/json")
    @POST("/generate")
    @Streaming
    suspend fun sendMessage(@Body request: ChatRequest): ResponseBody

    companion object {
        private const val BASE_URL = "https://aa0d-211-183-33-254.ngrok-free.app"  // 여기에 실제 ngrok URL을 입력하세요

        fun create(): ChatBotService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ChatBotService::class.java)
        }
    }
}