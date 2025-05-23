package com.example.purrytify.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.purrytify.repository.AuthRepository
import com.example.purrytify.util.EventBus
import com.example.purrytify.util.NetworkUtils
import com.example.purrytify.util.TokenManager
import kotlinx.coroutines.*

class TokenRefreshService : Service() {

    private val TAG = "TokenRefreshService"
    private lateinit var tokenManager: TokenManager
    private lateinit var authRepository: AuthRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    // Flag to indicate service is initialized
    private var isInitialized = false

    companion object {
        private const val TOKEN_CHECK_INTERVAL = 30_000L // 30 seconds
        private const val INITIAL_DELAY = 5_000L // 5 second initial delay
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TokenRefreshService created")

        // Defer heavy initialization for later
        serviceScope.launch(Dispatchers.IO) {
            initializeService()
        }
    }

    private suspend fun initializeService() {
        try {
            withContext(Dispatchers.IO) {
                tokenManager = (application as com.example.purrytify.PurrytifyApp).tokenManager
                authRepository = AuthRepository(tokenManager)
                isInitialized = true
                Log.d(TAG, "TokenRefreshService initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing service: ${e.message}")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "TokenRefreshService started")

        // Start token checking with delay to avoid blocking startup
        serviceScope.launch {
            // Wait for service to initialize and add initial delay
            delay(INITIAL_DELAY)
            if (!isInitialized) {
                initializeService()
            }
            startTokenChecking()
        }

        // if the service is killed, restart it with the last intent
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d(TAG, "TokenRefreshService destroyed")

        job?.cancel()
        serviceScope.cancel()

        super.onDestroy()
    }

    private fun startTokenChecking() {
        job?.cancel()

        // Start a new coroutine to check the token status
        job = serviceScope.launch {
            while (isActive) {
                if (!isInitialized) {
                    Log.d(TAG, "Service not initialized yet, delaying token check")
                    delay(1000)
                    continue
                }

                Log.d(TAG, "Checking token status...")

                if (!tokenManager.isLoggedIn()) {
                    Log.d(TAG, "User not logged in, stopping token check")
                    stopSelf()
                    break
                }

                // If the device offline, skip token verification
                if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
                    Log.d(TAG, "No internet connection, skipping token verification")
                    delay(TOKEN_CHECK_INTERVAL)
                    continue
                }

                // check if the token is expiring soon
                if (tokenManager.isTokenExpiring()) {
                    Log.d(TAG, "Token is expiring soon, trying to refresh")
                    refreshToken()
                } else {
                    // Check if the token is still valid
                    val verifyResult = authRepository.verifyToken()
                    if (verifyResult.isFailure || verifyResult.getOrNull() == false) {
                        Log.d(TAG, "Token verification failed, trying to refresh")
                        refreshToken()
                    } else {
                        Log.d(TAG, "Token is still valid")
                    }
                }

                // Wait for the next check
                delay(TOKEN_CHECK_INTERVAL)
            }
        }
    }

    private suspend fun refreshToken() {
        // Check if the device is online
        if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
            Log.d(TAG, "No internet connection, skipping token refresh")
            return
        }

        val result = authRepository.refreshToken()

        if (result.isSuccess) {
            Log.d(TAG, "Token refreshed successfully")

        } else {
            // Check if the device is online
            if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
                Log.d(TAG, "Token refresh failed due to no internet, not logging out")
                return
            }

            Log.e(TAG, "Failed to refresh token: ${result.exceptionOrNull()?.message}")
            // Send event token refresh failed
            EventBus.emitTokenEvent(EventBus.TokenEvent.TokenRefreshFailed)
            // If the refresh token fails, log the user out
            Log.d(TAG, "User logged out due to token refresh failure")
            stopSelf()
        }
    }
}