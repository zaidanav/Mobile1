package com.example.purrytify

import android.app.Application
import android.util.Log
import com.example.purrytify.data.database.AppDatabase
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.util.NetworkConnectionObserver
import com.example.purrytify.util.TokenManager

class PurrytifyApp : Application() {
    lateinit var tokenManager: TokenManager
    lateinit var networkConnectionObserver: NetworkConnectionObserver

    val database by lazy {
        Log.d("PurrytifyApp", "Initializing Room Database")
        AppDatabase.getDatabase(this)
    }

    val songRepository by lazy {
        SongRepository(database.songDao())
    }

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        RetrofitClient.initialize(this)

        // Initialize NetworkConnectionObserver
        networkConnectionObserver = NetworkConnectionObserver(this)
        networkConnectionObserver.start()
    }

    override fun onTerminate() {
        super.onTerminate()
        networkConnectionObserver.stop()
    }
}