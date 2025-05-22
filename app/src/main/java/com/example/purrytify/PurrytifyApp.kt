package com.example.purrytify

import android.app.Application
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.util.TokenManager

class PurrytifyApp : Application() {
    lateinit var tokenManager: TokenManager

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        RetrofitClient.initialize(this)
    }
}