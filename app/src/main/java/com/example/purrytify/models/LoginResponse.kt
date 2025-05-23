package com.example.purrytify.models

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
)