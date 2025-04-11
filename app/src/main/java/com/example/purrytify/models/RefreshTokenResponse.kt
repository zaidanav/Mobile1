package com.example.purrytify.models

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)