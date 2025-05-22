package com.example.purrytify.network

import com.example.purrytify.models.LoginRequest
import com.example.purrytify.models.LoginResponse
import com.example.purrytify.models.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    // profile
    @GET("api/profile")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<UserProfile>
}