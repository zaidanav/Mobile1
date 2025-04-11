package com.example.purrytify.service

import android.net.Uri
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.purrytify.models.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

class MediaPlayerService : Service() {

    private val mediaPlayer = MediaPlayer()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    private val binder = MediaPlayerBinder()

    inner class MediaPlayerBinder : Binder() {
        fun getService(): MediaPlayerService = this@MediaPlayerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        // Set up completion listener
        mediaPlayer.setOnCompletionListener {
            _isPlaying.value = false
        }
    }

    fun playSong(song: Song) {
        try {
            // Reset media player jika sedang memainkan lagu lain
            mediaPlayer.reset()

            // Cek apakah path adalah content URI atau file path lokal
            if (song.filePath.startsWith("content://")) {
                // Gunakan ContentResolver untuk content URI
                val uri = Uri.parse(song.filePath)
                val contentResolver = applicationContext.contentResolver
                val afd = contentResolver.openFileDescriptor(uri, "r")

                if (afd != null) {
                    mediaPlayer.setDataSource(afd.fileDescriptor)
                    afd.close()
                } else {
                    throw IOException("Cannot open file descriptor for URI: ${song.filePath}")
                }
            } else {
                // File lokal biasa
                mediaPlayer.setDataSource(song.filePath)
            }

            // Prepare dan play
            mediaPlayer.prepare()
            mediaPlayer.start()

            // Update state
            _currentSong.value = song
            _isPlaying.value = true
            _duration.value = mediaPlayer.duration

            // Start position tracking
            startPositionTracking()

        } catch (e: IOException) {
            Log.e("MediaPlayerService", "Error playing song: ${e.message}")
        } catch (e: Exception) {
            Log.e("MediaPlayerService", "Unexpected error: ${e.message}")
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            _isPlaying.value = false
        } else {
            mediaPlayer.start()
            _isPlaying.value = true
            startPositionTracking()
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer.seekTo(position)
        _currentPosition.value = position
    }

    private fun startPositionTracking() {
        Thread {
            while (mediaPlayer.isPlaying) {
                try {
                    _currentPosition.value = mediaPlayer.currentPosition
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    break
                }
            }
        }.start()
    }

    override fun onDestroy() {
        mediaPlayer.release()
        super.onDestroy()
    }
}