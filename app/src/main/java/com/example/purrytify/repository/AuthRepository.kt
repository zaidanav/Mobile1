package com.example.purrytify.repository

import com.example.purrytify.models.LoginRequest
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.util.TokenManager

class AuthRepository(private val tokenManager: TokenManager) {

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = RetrofitClient.apiService.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val accessToken = response.body()!!.accessToken
                tokenManager.saveToken(accessToken)
                Result.success(accessToken)
            } else {
                Result.failure(Exception("Login failed: ${response.errorBody()?.string() ?: "Unknown error"}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Login failed: ${e.message}"))
        }
    }

    fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }

    fun logout() {
        tokenManager.deleteToken()
    }
}