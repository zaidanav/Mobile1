package com.example.purrytify.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.models.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {
    // Currently playing song
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    // Toggle play/pause
    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    // Set current song
    fun setCurrentSong(song: Song) {
        Log.d("MainViewModel", "Setting current song: ${song.title}")
        _currentSong.value = song
        _isPlaying.value = true
    }
}

object ViewModelFactoryProvider {
    private var instance: MainViewModel? = null

    val Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                if (instance == null) {
                    instance = MainViewModel()
                }
                return instance as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}