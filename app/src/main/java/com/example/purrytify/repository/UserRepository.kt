package com.example.purrytify.repository

import com.example.purrytify.models.UserProfile
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.util.TokenManager

class UserRepository(private val tokenManager: TokenManager) {

    suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val token = tokenManager.getToken()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("Token tidak tersedia, silakan login terlebih dahulu"))
            }

            val response = RetrofitClient.apiService.getUserProfile("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Gagal mendapatkan profil: ${response.errorBody()?.string() ?: "Unknown error"}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mendapatkan profil: ${e.message}"))
        }
    }

    // Function lain terkait dengan user di masa depan bisa ditambahkan di sini
}