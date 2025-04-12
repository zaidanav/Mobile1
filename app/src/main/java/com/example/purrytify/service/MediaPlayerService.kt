package com.example.purrytify.service

import android.net.Uri
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.purrytify.models.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

class MediaPlayerService : Service() {
    private val TAG = "MediaPlayerService"
    private val mediaPlayer = MediaPlayer()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    // Flags for the end of playback
    private val _reachedEndOfPlayback = MutableStateFlow(false)
    val reachedEndOfPlayback: StateFlow<Boolean> = _reachedEndOfPlayback

    // Bonus features state
    private var shuffleEnabled = false
    private var repeatMode = 0 // 0: off, 1: repeat all, 2: repeat one

    private val binder = MediaPlayerBinder()
    private lateinit var localBroadcastManager: LocalBroadcastManager

    inner class MediaPlayerBinder : Binder() {
        fun getService(): MediaPlayerService = this@MediaPlayerService
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        // Set up completion listener
        mediaPlayer.setOnCompletionListener {
            Log.d(TAG, "Song completed playback")
            _isPlaying.value = false

            // Handle repeat one mode
            if (repeatMode == 2) {
                Log.d(TAG, "Repeat One mode active, replaying current song")
                _currentSong.value?.let { song ->
                    // Replay the same song
                    playAgain()
                }
            } else if (repeatMode == 1) {
                // In Repeat All mode, send broadcast to proceed to next song
                Log.d(TAG, "Repeat All mode active, proceeding to next song")
                val intent = Intent("com.example.purrytify.SONG_COMPLETED")
                localBroadcastManager.sendBroadcast(intent)
            } else {
                // In no-repeat mode (0), check if we're at the end
                Log.d(TAG, "No repeat mode, checking if we need to stop")
                // We'll set a flag that can be checked by the ViewModel
                _reachedEndOfPlayback.value = true
                // Still send the completion broadcast to let ViewModel handle the situation
                val intent = Intent("com.example.purrytify.SONG_COMPLETED")
                intent.putExtra("END_OF_PLAYBACK", true)
                localBroadcastManager.sendBroadcast(intent)
            }
        }

        // Set up error listener
        mediaPlayer.setOnErrorListener { mp, what, extra ->
            Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
            true // Return true to indicate we handled the error
        }
    }

    fun playSong(song: Song) {
        try {
            Log.d(TAG, "Playing song: ${song.title}, path: ${song.filePath}")
            // Reset end of playback flag when starting a new song
            _reachedEndOfPlayback.value = false

            // Reset media player if currently playing another song
            mediaPlayer.reset()

            // Check if path is content URI or local file path
            if (song.filePath.startsWith("content://")) {
                // Use ContentResolver for content URI
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
                // Regular local file
                mediaPlayer.setDataSource(song.filePath)
            }

            // Prepare and play
            mediaPlayer.prepare()
            mediaPlayer.start()

            // Update state
            _currentSong.value = song
            _isPlaying.value = true
            _duration.value = mediaPlayer.duration
            Log.d(TAG, "Song duration: ${mediaPlayer.duration}ms")

            // Start position tracking
            startPositionTracking()

        } catch (e: IOException) {
            Log.e(TAG, "Error playing song: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}")
            e.printStackTrace()
        }
    }

    // Play the current song again (for repeat one)
    private fun playAgain() {
        try {
            mediaPlayer.seekTo(0)
            mediaPlayer.start()
            _isPlaying.value = true
            startPositionTracking()
        } catch (e: Exception) {
            Log.e(TAG, "Error replaying song: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (mediaPlayer.isPlaying) {
            Log.d(TAG, "Pausing playback")
            mediaPlayer.pause()
            _isPlaying.value = false
        } else {
            Log.d(TAG, "Resuming playback")
            mediaPlayer.start()
            _isPlaying.value = true
            startPositionTracking()
        }
    }

    fun seekTo(position: Int) {
        Log.d(TAG, "Seeking to position: ${position}ms")
        mediaPlayer.seekTo(position)
        _currentPosition.value = position
    }

    // Set shuffle mode
    fun setShuffleEnabled(enabled: Boolean) {
        Log.d(TAG, "Shuffle mode set to: $enabled")
        shuffleEnabled = enabled
    }

    // Set repeat mode
    fun setRepeatMode(mode: Int) {
        Log.d(TAG, "Repeat mode set to: $mode")
        repeatMode = mode
    }

    // Reset the end of playback flag
    fun resetEndOfPlaybackFlag() {
        _reachedEndOfPlayback.value = false
    }

    // Method to stop playback and move to end of track
    fun stopPlayback() {
        try {
            if (mediaPlayer.isPlaying) {
                // Get the total duration of the current song
                val totalDuration = mediaPlayer.duration

                // Seek to the end of the track
                mediaPlayer.seekTo(totalDuration)

                // Pause playback
                mediaPlayer.pause()

                // Update states
                _isPlaying.value = false
                _currentPosition.value = totalDuration
                _reachedEndOfPlayback.value = true

                Log.d(TAG, "Playback stopped and moved to end of track")
            } else {
                Log.d(TAG, "No need to stop playback, already paused")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback: ${e.message}")
        }
    }

    private fun startPositionTracking() {
        Thread {
            while (mediaPlayer.isPlaying) {
                try {
                    _currentPosition.value = mediaPlayer.currentPosition
                    Thread.sleep(500) // Update more frequently for smoother UI
                } catch (e: Exception) {
                    Log.e(TAG, "Error in position tracking: ${e.message}")
                    break
                }
            }
        }.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        mediaPlayer.release()
        super.onDestroy()
    }

}