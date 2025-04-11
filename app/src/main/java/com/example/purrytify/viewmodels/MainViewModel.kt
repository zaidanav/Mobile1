package com.example.purrytify.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.models.Song
import com.example.purrytify.service.MediaPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private var mediaPlayerService: MediaPlayerService? = null
    private var bound = false
    // Currently playing song
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlayerService.MediaPlayerBinder
            mediaPlayerService = binder.getService()
            bound = true

            // Collect data from service
            viewModelScope.launch {
                mediaPlayerService?.currentSong?.collect { song ->
                    _currentSong.value = song
                }
            }

            viewModelScope.launch {
                mediaPlayerService?.isPlaying?.collect { playing ->
                    _isPlaying.value = playing
                }
            }

            viewModelScope.launch {
                mediaPlayerService?.currentPosition?.collect { position ->
                    _currentPosition.value = position
                }
            }

            viewModelScope.launch {
                mediaPlayerService?.duration?.collect { duration ->
                    _duration.value = duration
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaPlayerService = null
            bound = false
        }
    }

    fun bindService(context: Context) {
        Intent(context, MediaPlayerService::class.java).also { intent ->
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            context.startService(intent)
        }
    }

    fun unbindService(context: Context) {
        if (bound) {
            context.unbindService(serviceConnection)
            bound = false
        }
    }

    fun playSong(song: Song) {
        mediaPlayerService?.playSong(song)
    }

    // Toggle play/pause
    fun togglePlayPause() {
        mediaPlayerService?.togglePlayPause()
    }

    fun seekTo(position: Int) {
        mediaPlayerService?.seekTo(position)
    }
    // Set current song
    fun setCurrentSong(song: Song) {
        Log.d("MainViewModel", "Setting current song: ${song.title}")
        _currentSong.value = song
        _isPlaying.value = true
    }
}