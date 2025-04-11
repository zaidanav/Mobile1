package com.example.purrytify

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.service.TokenRefreshService
import com.example.purrytify.ui.components.BottomNavbar
import com.example.purrytify.ui.components.MiniPlayer
import com.example.purrytify.ui.components.NoInternetConnection
import com.example.purrytify.ui.navigation.AppNavigation
import com.example.purrytify.ui.screens.LoginScreen
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.util.EventBus
import com.example.purrytify.util.NetworkConnectionObserver
import com.example.purrytify.util.TokenManager
import androidx.compose.ui.platform.LocalContext
import com.example.purrytify.viewmodels.MainViewModel
import com.example.purrytify.viewmodels.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager
    private lateinit var networkConnectionObserver: NetworkConnectionObserver

    private var isLoggedIn = mutableStateOf(false)
    private var isNetworkAvailable = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = (application as PurrytifyApp).tokenManager
        networkConnectionObserver = (application as PurrytifyApp).networkConnectionObserver

        isLoggedIn.value = tokenManager.isLoggedIn()

        // Event Bus for token events
        lifecycleScope.launch {
            EventBus.tokenEvents.collectLatest { event ->
                when (event) {
                    is EventBus.TokenEvent.TokenRefreshFailed -> {
                        // Show toast message and log out
                        Toast.makeText(
                            this@MainActivity,
                            "Session expired. Please log in again.",
                            Toast.LENGTH_LONG
                        ).show()
                        logout()
                    }
                }
            }
        }

        // Event Bus for network events
        lifecycleScope.launch {
            EventBus.networkEvents.collectLatest { event ->
                when (event) {
                    is EventBus.NetworkEvent.Connected -> {
                        Log.d("NetworkObserver", "Network connected")
                        isNetworkAvailable.value = true
                        networkConnectionObserver.checkAndUpdateConnectionStatus()
                    }
                    is EventBus.NetworkEvent.Disconnected -> {
                        Log.d("NetworkObserver", "Network disconnected")
                        isNetworkAvailable.value = false
                    }
                }
            }
        }

        // Start observing network connection
        networkConnectionObserver.start()

        // Start token refresh service if logged in
        if (isLoggedIn.value) {
            startTokenRefreshService()
        }

        setContent {
            PurrytifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val networkStatus by networkConnectionObserver.isConnected.collectAsState()

                    if (isLoggedIn.value) {
                        val navController = rememberNavController()
                        val viewModel: MainViewModel = viewModel(
                            factory = ViewModelFactory.getInstance(LocalContext.current)
                        )

                        val currentSong = viewModel.currentSong.collectAsState().value
                        val isPlaying = viewModel.isPlaying.collectAsState().value

                        Scaffold(
                            bottomBar = {
                                Column {
                                    if (currentSong != null) {
                                        MiniPlayer(
                                            currentSong = currentSong,
                                            isPlaying = isPlaying,
                                            onPlayPauseClick = { viewModel.togglePlayPause() },
                                            onPlayerClick = { /* Navigate to full player */ }
                                        )
                                    } else {
                                        Text("Debug: currentSong is null", color = Color.Red)
                                    }

                                    BottomNavbar(navController = navController)
                                }
                            },
                            topBar = {
                                NoInternetConnection(isVisible = !networkStatus)
                            }
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier.padding(paddingValues)
                            ) {
                                AppNavigation(
                                    navController = navController,
                                    networkConnectionObserver = networkConnectionObserver,
                                )
                            }
                        }
                    } else {
                        LoginScreen(
                            onLoginSuccess = {
                                isLoggedIn.value = true
                                startTokenRefreshService()

                                // Check network status after login
                                networkConnectionObserver.checkAndUpdateConnectionStatus()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check is service running and start if not
        if (tokenManager.isLoggedIn()) {
            startTokenRefreshService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkConnectionObserver.stop()
        stopTokenRefreshService()
    }

    private fun startTokenRefreshService() {
        val serviceIntent = Intent(this, TokenRefreshService::class.java)
        startService(serviceIntent)
    }

    private fun stopTokenRefreshService() {
        val serviceIntent = Intent(this, TokenRefreshService::class.java)
        stopService(serviceIntent)
    }

    fun logout() {
        stopTokenRefreshService()
        tokenManager.deleteTokens()
        isLoggedIn.value = false


        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}