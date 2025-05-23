package com.example.purrytify.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.ui.screens.HomeScreen
import com.example.purrytify.ui.screens.LibraryScreen
import com.example.purrytify.ui.screens.OnlineSongsScreen
import com.example.purrytify.ui.screens.ProfileScreen
import com.example.purrytify.ui.screens.QueueScreen
import com.example.purrytify.util.NetworkConnectionObserver
import com.example.purrytify.viewmodels.MainViewModel

object Destinations {
    const val HOME_ROUTE = "home"
    const val LIBRARY_ROUTE = "library"
    const val PROFILE_ROUTE = "profile"
    const val QUEUE_ROUTE = "queue"
    const val ONLINE_SONGS_ROUTE = "online_songs"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel,  // Penting: parameter ini harus ada
    networkConnectionObserver: NetworkConnectionObserver  // Penting: parameter ini harus ada
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME_ROUTE,
        modifier = modifier
    ) {
        composable(Destinations.HOME_ROUTE) {
            HomeScreen()
        }
        composable(Destinations.LIBRARY_ROUTE) {
            LibraryScreen()
        }
        composable(Destinations.PROFILE_ROUTE) {
            ProfileScreen()
        }
        composable(Destinations.QUEUE_ROUTE) {
            QueueScreen(
                onNavigateBack = { navController.popBackStack() },
                mainViewModel = mainViewModel
            )
        }
        composable(Destinations.ONLINE_SONGS_ROUTE) {
            OnlineSongsScreen(
                onSongSelected = { song ->
                    // Play the online song
                    mainViewModel.playOnlineSong(song)
                }
            )
        }
    }
}