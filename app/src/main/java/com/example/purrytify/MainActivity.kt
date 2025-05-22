package com.example.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Text
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.ui.components.BottomNavbar
import com.example.purrytify.ui.navigation.AppNavigation
import com.example.purrytify.ui.screens.LoginScreen
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.util.TokenManager
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.ui.components.MiniPlayer
import com.example.purrytify.viewmodels.MainViewModel
import com.example.purrytify.viewmodels.ViewModelFactoryProvider


class MainActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager

    private var isLoggedIn = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = (application as PurrytifyApp).tokenManager
        isLoggedIn.value = tokenManager.isLoggedIn()

        setContent {
            PurrytifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLoggedIn.value) {
                        val navController = rememberNavController()
                        val viewModel: MainViewModel = viewModel(factory = ViewModelFactoryProvider.Factory)
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
                            }
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier.padding(paddingValues)
                            ) {
                                AppNavigation(navController = navController)
                            }
                        }
                    } else {
                        LoginScreen(
                            onLoginSuccess = {
                                isLoggedIn.value = true
                            }
                        )
                    }
                }
            }
        }
    }
}
