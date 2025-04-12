package com.example.purrytify.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

// NetworkConnectionObserver class for observing network connectivity changes
class NetworkConnectionObserver(private val context: Context) {

    private val _isConnected = MutableStateFlow(getInitialConnectionStatus())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d("NetworkObserver", "Network available")
            updateConnectionStatus(true)
        }

        override fun onLost(network: Network) {
            Log.d("NetworkObserver", "Network lost, checking if any other network is available")
            val stillConnected = NetworkUtils.isNetworkAvailable(context)
            updateConnectionStatus(stillConnected)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )
            Log.d("NetworkObserver", "Network capabilities changed, hasInternet: $hasInternet")
            updateConnectionStatus(hasInternet)
        }

        override fun onUnavailable() {
            Log.d("NetworkObserver", "Network unavailable")
            updateConnectionStatus(false)
        }
    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        Log.d("NetworkObserver", "Updating connection status to: $isConnected")
        if (_isConnected.value != isConnected) {
            _isConnected.value = isConnected

            // Emit event to EventBus
            coroutineScope.launch {
                if (isConnected) {
                    EventBus.emitNetworkEvent(EventBus.NetworkEvent.Connected)
                } else {
                    EventBus.emitNetworkEvent(EventBus.NetworkEvent.Disconnected)
                }
            }
        }
    }

    private fun getInitialConnectionStatus(): Boolean {
        val isConnected = NetworkUtils.isNetworkAvailable(context)
        Log.d("NetworkObserver", "Initial connection status: $isConnected")
        return isConnected
    }

    fun start() {
        Log.d("NetworkObserver", "Starting network observer")
        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            Log.e("NetworkObserver", "Error registering network callback", e)
        }
    }

    fun stop() {
        Log.d("NetworkObserver", "Stopping network observer")
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e("NetworkObserver", "Error unregistering network callback", e)
        }
    }

    fun checkAndUpdateConnectionStatus() {
        val isConnected = NetworkUtils.isNetworkAvailable(context)
        Log.d("NetworkObserver", "Manually checking connection status: $isConnected")
        updateConnectionStatus(isConnected)
    }
}