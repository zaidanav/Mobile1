package com.example.purrytify.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// EventBus for handling events in the app
object EventBus {

    // Token events
    sealed class TokenEvent {
        data object TokenRefreshFailed : TokenEvent()
    }

    // Network events
    sealed class NetworkEvent {
        data object Connected : NetworkEvent()
        data object Disconnected : NetworkEvent()
    }

    private val _tokenEvents = MutableSharedFlow<TokenEvent>()
    val tokenEvents: SharedFlow<TokenEvent> = _tokenEvents.asSharedFlow()

    private val _networkEvents = MutableSharedFlow<NetworkEvent>()
    val networkEvents: SharedFlow<NetworkEvent> = _networkEvents.asSharedFlow()

    // Emit event for token
    suspend fun emitTokenEvent(event: TokenEvent) {
        _tokenEvents.emit(event)
    }

    // Emit event for network
    suspend fun emitNetworkEvent(event: NetworkEvent) {
        _networkEvents.emit(event)
    }
}