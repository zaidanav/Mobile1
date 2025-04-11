package com.example.purrytify.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.entity.Song as EntitySong
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.models.Song
import com.example.purrytify.service.MediaPlayerService
import com.example.purrytify.util.SongMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(private val songRepository: SongRepository) : ViewModel() {
    private val TAG = "MainViewModel"
    private var mediaPlayerService: MediaPlayerService? = null
    private var bound = false

    // Currently playing song
    private val _currentSong = kotlinx.coroutines.flow.MutableStateFlow<Song?>(null)
    val currentSong: kotlinx.coroutines.flow.StateFlow<Song?> = _currentSong

    // Playback state
    private val _isPlaying = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isPlaying: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPlaying

    private val _currentPosition = kotlinx.coroutines.flow.MutableStateFlow(0)
    val currentPosition: kotlinx.coroutines.flow.StateFlow<Int> = _currentPosition

    private val _duration = kotlinx.coroutines.flow.MutableStateFlow(0)
    val duration: kotlinx.coroutines.flow.StateFlow<Int> = _duration

    // Queue for songs (bonus feature)
    private val _queue = kotlinx.coroutines.flow.MutableStateFlow<List<Song>>(emptyList())
    val queue: kotlinx.coroutines.flow.StateFlow<List<Song>> = _queue

    // Current index in the queue (for next/previous navigation)
    private val _currentQueueIndex = kotlinx.coroutines.flow.MutableStateFlow(-1)

    // Repeat mode (bonus feature) - 0: Off, 1: Repeat All, 2: Repeat One
    private val _repeatMode = kotlinx.coroutines.flow.MutableStateFlow(0)
    val repeatMode: kotlinx.coroutines.flow.StateFlow<Int> = _repeatMode

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            val binder = service as MediaPlayerService.MediaPlayerBinder
            mediaPlayerService = binder.getService()
            bound = true

            // Collect data from service
            viewModelScope.launch {
                mediaPlayerService?.currentSong?.collect { song ->
                    Log.d(TAG, "Current song updated: ${song?.title}")
                    _currentSong.value = song
                }
            }

            viewModelScope.launch {
                mediaPlayerService?.isPlaying?.collect { playing ->
                    Log.d(TAG, "Playing state updated: $playing")
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
                    Log.d(TAG, "Duration updated: $duration ms")
                    _duration.value = duration
                }
            }

            // If we already have a song to play when service connects, play it
            _currentSong.value?.let { song ->
                if (_isPlaying.value) {
                    mediaPlayerService?.playSong(song)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            mediaPlayerService = null
            bound = false
        }
    }

    fun bindService(context: Context) {
        Log.d(TAG, "Binding to service")
        Intent(context, MediaPlayerService::class.java).also { intent ->
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            context.startService(intent)
        }
    }

    fun unbindService(context: Context) {
        if (bound) {
            Log.d(TAG, "Unbinding from service")
            context.unbindService(serviceConnection)
            bound = false
        }
    }

    fun playSong(song: Song) {
        Log.d(TAG, "Playing song: ${song.title}")
        mediaPlayerService?.playSong(song)

        // If we're playing a song directly (not from queue),
        // get all songs and set up a new queue
        viewModelScope.launch {
            if (_queue.value.isEmpty()) {
                Log.d(TAG, "Queue is empty, loading all songs")
                loadAllSongs(song.id)
            } else {
                // Update current index in queue
                val index = _queue.value.indexOfFirst { it.id == song.id }
                if (index != -1) {
                    Log.d(TAG, "Setting current queue index to $index")
                    _currentQueueIndex.value = index
                }
            }
        }

        // Update song's last played timestamp
        viewModelScope.launch {
            Log.d(TAG, "Updating last played timestamp for song ${song.id}")
            songRepository.updateLastPlayed(song.id)
        }
    }

    // Load all songs as a queue - fixed to properly handle LiveData
    private suspend fun loadAllSongs(currentSongId: Long) {
        Log.d(TAG, "Loading all songs")
        // Get all songs from repository using proper LiveData handling
        val songsList = withContext(Dispatchers.Main) {
            val songs = mutableListOf<EntitySong>()
            val latch = java.util.concurrent.CountDownLatch(1)

            val observer = Observer<List<EntitySong>> { songList ->
                songs.addAll(songList)
                latch.countDown()
            }

            songRepository.allSongs.observeForever(observer)

            try {
                latch.await()
                songRepository.allSongs.removeObserver(observer)
                songs
            } catch (e: Exception) {
                Log.e(TAG, "Error loading songs: ${e.message}")
                songRepository.allSongs.removeObserver(observer)
                emptyList<EntitySong>()
            }
        }

        // Convert to UI model
        val uiSongs = SongMapper.fromEntityList(songsList)
        Log.d(TAG, "Loaded ${uiSongs.size} songs from repository")

        // Set the queue
        _queue.value = uiSongs

        // Set current index
        val index = uiSongs.indexOfFirst { it.id == currentSongId }
        Log.d(TAG, "Setting current queue index to $index for song $currentSongId")
        _currentQueueIndex.value = index
    }

    // Toggle play/pause
    fun togglePlayPause() {
        Log.d(TAG, "Toggle play/pause")
        mediaPlayerService?.togglePlayPause()
    }

    fun seekTo(position: Int) {
        Log.d(TAG, "Seek to position: $position")
        mediaPlayerService?.seekTo(position)
    }

    // Set current song
    fun setCurrentSong(song: Song) {
        Log.d(TAG, "Setting current song: ${song.title}")
        _currentSong.value = song
        _isPlaying.value = true

        // Play the song using the media player service
        mediaPlayerService?.playSong(song)
    }

    // Play next song in queue
    fun playNext() {
        Log.d(TAG, "Play next requested")
        if (_queue.value.isEmpty()) {
            Log.d(TAG, "Queue is empty, can't play next")
            return
        }

        if (_currentQueueIndex.value == -1) {
            Log.d(TAG, "Current index is -1, can't play next")
            return
        }

        // Determine next index based on repeat mode
        val nextIndex = getNextIndex()
        Log.d(TAG, "Next index calculated: $nextIndex")

        // Play the next song
        _queue.value.getOrNull(nextIndex)?.let { nextSong ->
            Log.d(TAG, "Playing next song: ${nextSong.title}")
            _currentQueueIndex.value = nextIndex
            setCurrentSong(nextSong)
        } ?: Log.d(TAG, "No song found at index $nextIndex")
    }

    // Play previous song in queue
    fun playPrevious() {
        Log.d(TAG, "Play previous requested")
        if (_queue.value.isEmpty()) {
            Log.d(TAG, "Queue is empty, can't play previous")
            return
        }

        if (_currentQueueIndex.value == -1) {
            Log.d(TAG, "Current index is -1, can't play previous")
            return
        }

        // If we're more than 3 seconds into the song, restart it instead of going to previous
        if (_currentPosition.value > 3000) {
            Log.d(TAG, "More than 3 seconds into song, restarting current song")
            mediaPlayerService?.seekTo(0)
            return
        }

        // Get previous index
        val prevIndex = getPreviousIndex()
        Log.d(TAG, "Previous index calculated: $prevIndex")

        // Play the previous song
        _queue.value.getOrNull(prevIndex)?.let { prevSong ->
            Log.d(TAG, "Playing previous song: ${prevSong.title}")
            _currentQueueIndex.value = prevIndex
            setCurrentSong(prevSong)
        } ?: Log.d(TAG, "No song found at index $prevIndex")
    }

    // Get next index considering repeat modes
    private fun getNextIndex(): Int {
        val currentIndex = _currentQueueIndex.value
        val queueSize = _queue.value.size

        // Return current index for repeat one
        if (_repeatMode.value == 2) {
            Log.d(TAG, "Repeat One mode, returning current index: $currentIndex")
            return currentIndex
        }

        // Standard next index
        val nextIndex = currentIndex + 1
        // Consider repeat all
        return if (nextIndex >= queueSize) {
            if (_repeatMode.value == 1) {
                Log.d(TAG, "Repeat All mode, wrapping to beginning")
                0
            } else {
                Log.d(TAG, "No repeat, staying at current index")
                currentIndex
            }
        } else {
            nextIndex
        }
    }

    // Get previous index considering repeat modes
    private fun getPreviousIndex(): Int {
        val currentIndex = _currentQueueIndex.value
        val queueSize = _queue.value.size

        // Return current index for repeat one
        if (_repeatMode.value == 2) {
            Log.d(TAG, "Repeat One mode, returning current index: $currentIndex")
            return currentIndex
        }

        // Standard previous index
        val prevIndex = currentIndex - 1
        // Consider repeat all
        return if (prevIndex < 0) {
            if (_repeatMode.value == 1) {
                Log.d(TAG, "Repeat All mode, wrapping to end")
                queueSize - 1
            } else {
                Log.d(TAG, "No repeat, staying at current index")
                currentIndex
            }
        } else {
            prevIndex
        }
    }

    // Add a song to the queue
    fun addToQueue(song: Song) {
        val currentQueue = _queue.value.toMutableList()
        currentQueue.add(song)
        _queue.value = currentQueue

        Log.d(TAG, "Added ${song.title} to queue. Queue size: ${currentQueue.size}")

        // If this is the first song and nothing is playing, start playing it
        if (currentQueue.size == 1 && _currentSong.value == null) {
            Log.d(TAG, "First song in queue, playing it")
            playSong(song)
        }
    }

    // Remove song from queue by index
    fun removeFromQueue(index: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (index >= 0 && index < currentQueue.size) {
            Log.d(TAG, "Removing song at index $index from queue")
            currentQueue.removeAt(index)
            _queue.value = currentQueue
        }
    }

    // Clear the entire queue
    fun clearQueue() {
        Log.d(TAG, "Clearing queue")
        _queue.value = emptyList()
        // Don't stop the current song, just clear what's coming next
    }

    // Set repeat mode
    fun setRepeatMode(mode: Int) {
        Log.d(TAG, "Setting repeat mode to $mode")
        _repeatMode.value = mode
        mediaPlayerService?.setRepeatMode(mode)
    }

    // Toggle like status for a song
    fun toggleLike(songId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Toggling like for song $songId to $isLiked")
                // Update in repository
                songRepository.toggleLike(songId, isLiked)

                // Update current song if it's the same one
                _currentSong.value?.let { song ->
                    if (song.id == songId) {
                        _currentSong.value = song.copy(isLiked = isLiked)
                    }
                }

                // Update in queue if present
                val updatedQueue = _queue.value.map { song ->
                    if (song.id == songId) song.copy(isLiked = isLiked) else song
                }
                _queue.value = updatedQueue

            } catch (e: Exception) {
                Log.e(TAG, "Error toggling like: ${e.message}")
            }
        }
    }
}