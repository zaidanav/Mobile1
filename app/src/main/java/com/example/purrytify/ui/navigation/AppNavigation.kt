package com.example.purrytify.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.ui.components.NoInternetScreen
import com.example.purrytify.ui.screens.HomeScreen
import com.example.purrytify.ui.screens.LibraryScreen
import com.example.purrytify.ui.screens.ProfileScreen
import com.example.purrytify.util.NetworkConnectionObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

object Destinations {
    const val HOME_ROUTE = "home"
    const val LIBRARY_ROUTE = "library"
    const val PROFILE_ROUTE = "profile"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    networkConnectionObserver: NetworkConnectionObserver,
) {

    val isConnected by networkConnectionObserver.isConnected.collectAsState()

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
            networkConnectionObserver.checkAndUpdateConnectionStatus()
            if (isConnected) {
                ProfileScreen()
            } else {
                NoInternetScreen()
            }
        }
    }
}