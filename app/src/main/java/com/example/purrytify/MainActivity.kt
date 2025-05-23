package com.example.purrytify

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.receivers.SongCompletionReceiver
import com.example.purrytify.service.TokenRefreshService
import com.example.purrytify.ui.components.BottomNavbar
import com.example.purrytify.ui.components.LoadingScreen
import com.example.purrytify.ui.components.MiniPlayer
import com.example.purrytify.ui.components.NoInternetConnection
import com.example.purrytify.ui.navigation.AppNavigation
import com.example.purrytify.ui.screens.LoginScreen
import com.example.purrytify.ui.screens.PlayerScreen
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.util.EventBus
import com.example.purrytify.util.NetworkConnectionObserver
import com.example.purrytify.util.TokenManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.purrytify.viewmodels.MainViewModel
import com.example.purrytify.viewmodels.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.util.SongDownloadManager

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private lateinit var tokenManager: TokenManager
    private lateinit var networkConnectionObserver: NetworkConnectionObserver
    private lateinit var mainViewModel: MainViewModel
    private lateinit var songRepository: SongRepository
    private val isLoggedIn = mutableStateOf(false)
    private val isNetworkAvailable = mutableStateOf(true)
    private val isInitialized = mutableStateOf(false)
    private lateinit var songCompletionReceiver: SongCompletionReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var downloadManager: SongDownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi NetworkConnectionObserver
        networkConnectionObserver = NetworkConnectionObserver(applicationContext)
        networkConnectionObserver.start()

        tokenManager = (application as PurrytifyApp).tokenManager

        // Initialize SongDownloadManager
        downloadManager = SongDownloadManager(this)

        // Lightweight initialization first
        lifecycleScope.launch(Dispatchers.Default) {
            initializeApp()
        }

        setContent {
            PurrytifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Show loading screen while initializing
                    if (!isInitialized.value) {
                        LoadingScreen()
                        return@Surface
                    }

                    val networkStatus by networkConnectionObserver.isConnected.collectAsState()

                    if (isLoggedIn.value) {
                        initializeUserData()

                        val navController = rememberNavController()

                        val currentSong by mainViewModel.currentSong.collectAsState()
                        val isPlaying by mainViewModel.isPlaying.collectAsState()

                        // State to track if the full player is showing
                        var showPlayerScreen by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxSize()) {
                            // Main scaffold with navigation bar and mini player
                            Scaffold(
                                bottomBar = {
                                    AnimatedVisibility(
                                        visible = !showPlayerScreen,
                                        enter = fadeIn() + slideInVertically { it },
                                        exit = fadeOut() + slideOutVertically { it }
                                    ) {
                                        Column {
                                            if (currentSong != null) {
                                                MiniPlayer(
                                                    currentSong = currentSong,
                                                    isPlaying = isPlaying,
                                                    onPlayPauseClick = { mainViewModel.togglePlayPause() },
                                                    onPlayerClick = { showPlayerScreen = true }
                                                )
                                            }

                                            BottomNavbar(navController = navController)
                                        }
                                    }
                                },
                                topBar = {
                                    NoInternetConnection(isVisible = !networkStatus)
                                }
                            ) { paddingValues ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (!showPlayerScreen) paddingValues else PaddingValues(0.dp))
                                ) {
                                    AppNavigation(
                                        navController = navController,
                                        networkConnectionObserver = networkConnectionObserver,
                                        mainViewModel = mainViewModel // Tambahkan parameter ini
                                    )
                                }
                            }

                            // Show the full player screen if needed
                            AnimatedVisibility(
                                visible = showPlayerScreen,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                PlayerScreen(
                                    onDismiss = { showPlayerScreen = false },
                                    navController = navController,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        LoginScreen(
                            onLoginSuccess = {
                                isLoggedIn.value = true
                                // Only start token service after successful login
                                lifecycleScope.launch(Dispatchers.IO) {
                                    startTokenRefreshService()
                                }

                                // Check if the current song belongs to the new user
                                checkCurrentSongAfterLogin()

                                // Check network status after login
                                networkConnectionObserver.checkAndUpdateConnectionStatus()
                            }
                        )
                    }
                }
            }
        }
    }

    private suspend fun initializeApp() {
        try {
            Log.d(TAG, "Starting app initialization")

            // Initialize lightweight components first
            withContext(Dispatchers.Main) {
                tokenManager = (application as PurrytifyApp).tokenManager
                songRepository = (application as PurrytifyApp).songRepository
                networkConnectionObserver = (application as PurrytifyApp).networkConnectionObserver
                localBroadcastManager = LocalBroadcastManager.getInstance(this@MainActivity)

                // Check login status (simple operation)
                isLoggedIn.value = tokenManager.isLoggedIn()
            }

            // Initialize network observer (non-blocking)
            networkConnectionObserver.start()

            // Initialize MainViewModel (potentially heavy)
            withContext(Dispatchers.Main) {
                mainViewModel = ViewModelProvider(
                    this@MainActivity,
                    ViewModelFactory.getInstance(applicationContext)
                ).get(MainViewModel::class.java)

                // Bind media player service
                mainViewModel.bindService(this@MainActivity)
            }

            // Initialize broadcast receiver for song completion
            withContext(Dispatchers.Main) {
                songCompletionReceiver = SongCompletionReceiver(mainViewModel)
                localBroadcastManager.registerReceiver(
                    songCompletionReceiver,
                    IntentFilter("com.example.purrytify.SONG_COMPLETED")
                )
            }

            // Setup EventBus listeners
            setupEventBusListeners()

            // Only start token service if user is logged in
            if (isLoggedIn.value) {
                startTokenRefreshService()
            }

            // Initialize download manager
            downloadManager = SongDownloadManager(this@MainActivity)

            // Mark initialization as complete
            isInitialized.value = true
            Log.d(TAG, "App initialization completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error during initialization: ${e.message}")
            // Even on error, we need to mark as initialized to show login screen
            isInitialized.value = true
        }
    }

    private fun setupEventBusListeners() {
        // Event Bus for token events
        lifecycleScope.launch {
            EventBus.tokenEvents.collectLatest { event ->
                when (event) {
                    is EventBus.TokenEvent.TokenRefreshFailed -> {
                        // Show toast message and log out
                        withContext(Dispatchers.Main) {
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
        }

        // Event Bus for network events
        lifecycleScope.launch {
            EventBus.networkEvents.collectLatest { event ->
                when (event) {
                    is EventBus.NetworkEvent.Connected -> {
                        Log.d(TAG, "Network connected")
                        isNetworkAvailable.value = true
                        networkConnectionObserver.checkAndUpdateConnectionStatus()
                    }
                    is EventBus.NetworkEvent.Disconnected -> {
                        Log.d(TAG, "Network disconnected")
                        isNetworkAvailable.value = false
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check is service running and start if not, but only if initialized and logged in
        if (isInitialized.value && tokenManager.isLoggedIn()) {
            lifecycleScope.launch(Dispatchers.IO) {
                startTokenRefreshService()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receiver using LocalBroadcastManager
        try {
            localBroadcastManager.unregisterReceiver(songCompletionReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }

        networkConnectionObserver.stop()
        stopTokenRefreshService()
        mainViewModel.unbindService(this)

        // Release download manager resources
        if (::downloadManager.isInitialized) {
            downloadManager.release()
        }
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
        songRepository.setCurrentUserId(-1)
        mainViewModel.handleLogout()

        isLoggedIn.value = false

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun initializeUserData() {
        val userId = tokenManager.getUserId()
        if (userId > 0) {
            Log.d(TAG, "Initializing with user ID: $userId")
            songRepository.setCurrentUserId(userId)
        }
    }

    private fun checkCurrentSongAfterLogin() {
        val currentSong = mainViewModel.currentSong.value

        if (currentSong != null) {
            lifecycleScope.launch {
                try {
                    val songExists = songRepository.getSongByIdDirect(currentSong.id) != null

                    if (!songExists) {
                        Log.d(TAG, "Current song does not belong to the new user, stopping playback")
                        mainViewModel.handleLogout()
                    } else {
                        Log.d(TAG, "Current song belongs to the new user, continuing playback")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking current song after login: ${e.message}")
                    mainViewModel.handleLogout()
                }
            }
        }
    }
}