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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MainViewModel(private val songRepository: SongRepository) : ViewModel() {
    private val TAG = "MainViewModel"
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

    // Queue for songs (bonus feature)
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue

    // All songs from repository (for navigation when no queue is set)
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())

    // Current index in the queue (for next/previous navigation)
    private val _currentQueueIndex = MutableStateFlow(-1)

    // Repeat mode (bonus feature) - 0: Off, 1: Repeat All, 2: Repeat One
    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode

    // Shuffle mode
    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled

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

    init {
        // Instead of loading all songs immediately, we'll do it lazily when needed
        // viewModelScope.launch {
        //    loadAllSongs()
        // }
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

        viewModelScope.launch {
            // Check if song is in the queue
            val queueIndex = _queue.value.indexOfFirst { it.id == song.id }

            if (queueIndex != -1) {
                // Song is in the queue, update the queue index
                Log.d(TAG, "Song is in queue at position $queueIndex")
                _currentQueueIndex.value = queueIndex
            } else if (_queue.value.isEmpty()) {
                // Queue is empty, use all songs for navigation
                Log.d(TAG, "Queue is empty, using all songs for navigation")

                // Lazily load all songs if needed
                if (_allSongs.value.isEmpty()) {
                    loadAllSongs()
                }

                // Update current index for the song being played
                val allSongsIndex = _allSongs.value.indexOfFirst { it.id == song.id }
                if (allSongsIndex != -1) {
                    Log.d(TAG, "Setting current index in all songs to $allSongsIndex")
                    _currentQueueIndex.value = allSongsIndex
                }
            } else {
                // Song is not in queue, but we have a queue
                // Let's keep the queue but update what's playing
                Log.d(TAG, "Song not in queue, but queue exists")
                // We don't update the queue index here as the song isn't in the queue
            }

            // Update song's last played timestamp
            Log.d(TAG, "Updating last played timestamp for song ${song.id}")
            songRepository.updateLastPlayed(song.id)
        }
    }

    // Load all songs from repository
    private suspend fun loadAllSongs() {
        Log.d(TAG, "Loading all songs")

        try {
            // Switch to IO dispatcher for database operations
            withContext(Dispatchers.IO) {
                val observer = Observer<List<EntitySong>> { songList ->
                    Log.d(TAG, "Received ${songList.size} songs from repository")
                    // Convert to UI model and update state
                    val uiSongs = SongMapper.fromEntityList(songList)
                    _allSongs.value = uiSongs
                }

                // Observe on the Main thread to get LiveData updates
                withContext(Dispatchers.Main) {
                    // Store the LiveData temporarily to avoid memory leaks
                    val songsLiveData = songRepository.allSongs
                    songsLiveData.observeForever(observer)

                    try {
                        // Wait for the first emission with a timeout
                        withTimeoutOrNull(5000L) {
                            // Use a channel to get a notification when data arrives
                            val channel = Channel<Unit>(Channel.RENDEZVOUS)
                            val tempObserver = Observer<List<EntitySong>> {
                                channel.trySend(Unit)
                            }
                            songsLiveData.observeForever(tempObserver)
                            try {
                                channel.receive() // Wait for data
                            } finally {
                                songsLiveData.removeObserver(tempObserver)
                            }
                        }
                    } finally {
                        // Make sure to clean up the observer
                        songsLiveData.removeObserver(observer)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading songs: ${e.message}")
            // Set an empty list in case of error
            _allSongs.value = emptyList()
        }
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

        // Update queue information if necessary
        viewModelScope.launch {
            // Check if song is in the queue
            val queueIndex = _queue.value.indexOfFirst { it.id == song.id }
            if (queueIndex != -1) {
                _currentQueueIndex.value = queueIndex
            }
        }
    }

    // Play next song
    fun playNext() {
        Log.d(TAG, "Play next requested")

        // First, check if we have an active queue
        if (_queue.value.isNotEmpty()) {
            Log.d(TAG, "Using queue for next song")
            playNextFromQueue()
        } else {
            Log.d(TAG, "Using all songs for next song")
            playNextFromAllSongs()
        }
    }

    // Play next song from the active queue
    private fun playNextFromQueue() {
        val queue = _queue.value

        if (queue.isEmpty()) {
            Log.d(TAG, "Queue is empty, cannot play next")
            return
        }

        // Handle case where we don't have a valid index yet
        if (_currentQueueIndex.value == -1) {
            val currentSong = _currentSong.value
            val newIndex = if (currentSong != null) {
                queue.indexOfFirst { it.id == currentSong.id }
            } else {
                -1
            }

            if (newIndex != -1) {
                _currentQueueIndex.value = newIndex
            } else {
                // If still no index, start from the beginning of queue
                _currentQueueIndex.value = 0
                val firstSong = queue.firstOrNull()
                if (firstSong != null) {
                    setCurrentSong(firstSong)
                    return
                }
            }
        }

        // Determine next index based on repeat mode
        val nextIndex = getNextIndex(queue.size)
        Log.d(TAG, "Next queue index calculated: $nextIndex")

        // Check if the next index is different from current index
        // If they're the same and not in Repeat One mode, we're at the end and should stop playback
        if (nextIndex == _currentQueueIndex.value && _repeatMode.value != 2) {
            Log.d(TAG, "Already at the last song in queue and repeat is off, stopping playback")
            // Stop the current playback
            stopCurrentPlayback()
            return
        }

        // Play the next song from queue
        queue.getOrNull(nextIndex)?.let { nextSong ->
            Log.d(TAG, "Playing next song from queue: ${nextSong.title}")
            _currentQueueIndex.value = nextIndex
            setCurrentSong(nextSong)
        } ?: Log.d(TAG, "No song found in queue at index $nextIndex")
    }

    // Play next song from all songs if no queue is active
    private fun playNextFromAllSongs() {
        // Lazily load all songs if they haven't been loaded yet
        viewModelScope.launch {
            if (_allSongs.value.isEmpty()) {
                loadAllSongs()
            }

            val songs = _allSongs.value

            if (songs.isEmpty()) {
                Log.d(TAG, "No songs available, can't play next")
                return@launch
            }

            // Handle case where we don't have a valid index yet
            if (_currentQueueIndex.value == -1) {
                val currentSong = _currentSong.value
                val newIndex = if (currentSong != null) {
                    songs.indexOfFirst { it.id == currentSong.id }
                } else {
                    -1
                }

                if (newIndex != -1) {
                    _currentQueueIndex.value = newIndex
                } else {
                    // If still no index, start from the beginning
                    _currentQueueIndex.value = 0
                    val firstSong = songs.firstOrNull()
                    if (firstSong != null) {
                        setCurrentSong(firstSong)
                        return@launch
                    }
                }
            }

            // Determine next index based on repeat mode
            val nextIndex = getNextIndex(songs.size)
            Log.d(TAG, "Next all songs index calculated: $nextIndex")

            // Check if the next index is different from current index
            // If they're the same and not in Repeat One mode, we're at the end and should stop playback
            if (nextIndex == _currentQueueIndex.value && _repeatMode.value != 2) {
                Log.d(TAG, "Already at the last song in library and repeat is off, stopping playback")
                // Stop the current playback
                stopCurrentPlayback()
                return@launch
            }

            // Play the next song
            songs.getOrNull(nextIndex)?.let { nextSong ->
                Log.d(TAG, "Playing next song from all songs: ${nextSong.title}")
                _currentQueueIndex.value = nextIndex
                setCurrentSong(nextSong)
            } ?: Log.d(TAG, "No song found in all songs at index $nextIndex")
        }
    }

    // Helper method to stop current playback
    private fun stopCurrentPlayback() {
        // Stop the MediaPlayer using the service's dedicated method
        mediaPlayerService?.stopPlayback()

        // Update UI state to reflect stopped playback
        _isPlaying.value = false
    }

    // Play previous song
    fun playPrevious() {
        Log.d(TAG, "Play previous requested")

        // If we're more than 3 seconds into the song, restart it instead of going to previous
        if (_currentPosition.value > 3000) {
            Log.d(TAG, "More than 3 seconds into song, restarting current song")
            mediaPlayerService?.seekTo(0)
            return
        }

        // First, check if we have an active queue
        if (_queue.value.isNotEmpty()) {
            Log.d(TAG, "Using queue for previous song")
            playPreviousFromQueue()
        } else {
            Log.d(TAG, "Using all songs for previous song")
            playPreviousFromAllSongs()
        }
    }

    // Play previous song from the active queue
    private fun playPreviousFromQueue() {
        val queue = _queue.value

        if (queue.isEmpty()) {
            Log.d(TAG, "Queue is empty, cannot play previous")
            return
        }

        // Handle case where we don't have a valid index yet
        if (_currentQueueIndex.value == -1) {
            val currentSong = _currentSong.value
            val newIndex = if (currentSong != null) {
                queue.indexOfFirst { it.id == currentSong.id }
            } else {
                -1
            }

            if (newIndex != -1) {
                _currentQueueIndex.value = newIndex
            } else {
                // If still no index, start from the beginning of queue
                _currentQueueIndex.value = 0
                val firstSong = queue.firstOrNull()
                if (firstSong != null) {
                    setCurrentSong(firstSong)
                    return
                }
            }
        }

        // Get previous index
        val prevIndex = getPreviousIndex(queue.size)
        Log.d(TAG, "Previous queue index calculated: $prevIndex")

        // Play the previous song from queue
        queue.getOrNull(prevIndex)?.let { prevSong ->
            Log.d(TAG, "Playing previous song from queue: ${prevSong.title}")
            _currentQueueIndex.value = prevIndex
            setCurrentSong(prevSong)
        } ?: Log.d(TAG, "No song found in queue at index $prevIndex")
    }

    // Play previous song from all songs if no queue is active
    private fun playPreviousFromAllSongs() {
        // Lazily load all songs if they haven't been loaded yet
        viewModelScope.launch {
            if (_allSongs.value.isEmpty()) {
                loadAllSongs()
            }

            val songs = _allSongs.value

            if (songs.isEmpty()) {
                Log.d(TAG, "No songs available, can't play previous")
                return@launch
            }

            // Handle case where we don't have a valid index yet
            if (_currentQueueIndex.value == -1) {
                val currentSong = _currentSong.value
                val newIndex = if (currentSong != null) {
                    songs.indexOfFirst { it.id == currentSong.id }
                } else {
                    -1
                }

                if (newIndex != -1) {
                    _currentQueueIndex.value = newIndex
                } else {
                    // If still no index, start from the beginning
                    _currentQueueIndex.value = 0
                    val firstSong = songs.firstOrNull()
                    if (firstSong != null) {
                        setCurrentSong(firstSong)
                        return@launch
                    }
                }
            }

            // Get previous index
            val prevIndex = getPreviousIndex(songs.size)
            Log.d(TAG, "Previous all songs index calculated: $prevIndex")

            // Play the previous song
            songs.getOrNull(prevIndex)?.let { prevSong ->
                Log.d(TAG, "Playing previous song from all songs: ${prevSong.title}")
                _currentQueueIndex.value = prevIndex
                setCurrentSong(prevSong)
            } ?: Log.d(TAG, "No song found in all songs at index $prevIndex")
        }
    }

    // Get next index considering repeat modes
    private fun getNextIndex(listSize: Int): Int {
        val currentIndex = _currentQueueIndex.value

        // Check for invalid scenario
        if (listSize == 0) return -1

        // Return current index for repeat one
        if (_repeatMode.value == 2) {
            Log.d(TAG, "Repeat One mode, returning current index: $currentIndex")
            return currentIndex
        }

        // Standard next index
        val nextIndex = currentIndex + 1

        // Check if we're at the end of the list
        if (nextIndex >= listSize) {
            if (_repeatMode.value == 1) {
                // Repeat All mode - wrap around to the beginning
                Log.d(TAG, "Repeat All mode, wrapping to beginning")
                return 0
            } else {
                // No repeat mode - stay at end and don't play next
                Log.d(TAG, "No repeat mode and reached end, keeping current index")
                // Send a signal that we've reached the end
                mediaPlayerService?.let {
                    if (!it.reachedEndOfPlayback.value) {
                        // The MediaPlayerService handles playback end, but we set the flag here too
                        // in case this method is called outside of song completion event
                    }
                }
                return currentIndex
            }
        } else {
            // Not at end of list, return next index
            return nextIndex
        }
    }

    // Get previous index considering repeat modes
    private fun getPreviousIndex(listSize: Int): Int {
        val currentIndex = _currentQueueIndex.value

        // Check for invalid scenario
        if (listSize == 0) return -1

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
                listSize - 1
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

        // If this is the first song in the queue and nothing is playing, start playing it
        if (currentQueue.size == 1 && _currentSong.value == null) {
            Log.d(TAG, "First song in queue, playing it")
            playSong(song)
        } else if (_currentSong.value != null && currentQueue.size == 1) {
            // If we have a playing song and this is the first queued song,
            // set the current queue index properly
            _currentQueueIndex.value = 0
        }

        // Show a success message or notification could be added here
    }

    // Remove song from queue by index
    fun removeFromQueue(index: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (index >= 0 && index < currentQueue.size) {
            Log.d(TAG, "Removing song at index $index from queue")
            currentQueue.removeAt(index)
            _queue.value = currentQueue

            // Update current index if needed
            if (_currentQueueIndex.value >= index && _queue.value.isNotEmpty()) {
                _currentQueueIndex.value = _currentQueueIndex.value - 1
                if (_currentQueueIndex.value < 0) _currentQueueIndex.value = 0
            }
        }
    }

    // Clear the entire queue
    fun clearQueue() {
        Log.d(TAG, "Clearing queue")
        _queue.value = emptyList()
        _currentQueueIndex.value = -1
        // Don't stop the current song, just clear what's coming next
    }

    // Set repeat mode
    fun setRepeatMode(mode: Int) {
        Log.d(TAG, "Setting repeat mode to $mode")
        _repeatMode.value = mode
        mediaPlayerService?.setRepeatMode(mode)
    }

    // Set shuffle mode
    fun setShuffleEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting shuffle to $enabled")
        _shuffleEnabled.value = enabled
        mediaPlayerService?.setShuffleEnabled(enabled)
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

                // Update in all songs
                val updatedAllSongs = _allSongs.value.map { song ->
                    if (song.id == songId) song.copy(isLiked = isLiked) else song
                }
                _allSongs.value = updatedAllSongs

            } catch (e: Exception) {
                Log.e(TAG, "Error toggling like: ${e.message}")
            }
        }
    }

    // Handle song completion (called from song completion receiver)
    fun onSongCompleted(isEndOfPlayback: Boolean = false) {
        Log.d(TAG, "Song completed, isEndOfPlayback: $isEndOfPlayback")

        // Check repeat mode
        when (_repeatMode.value) {
            1 -> {
                // Repeat All mode - always play next song
                Log.d(TAG, "Repeat All mode, playing next song")
                playNext()
            }
            2 -> {
                // Repeat One mode - should be handled by MediaPlayerService already
                Log.d(TAG, "Repeat One mode, should be handled by MediaPlayerService")
            }
            else -> {
                // No repeat (0)
                if (isEndOfPlayback) {
                    // We're at the end of the queue/playlist and not in repeat mode
                    // Just stop - don't proceed to the next song
                    Log.d(TAG, "End of playlist reached without repeat mode, stopping playback")

                    // If needed, you could update UI state here to indicate playback has ended
                    _isPlaying.value = false

                    // Reset the service's end of playback flag if needed
                    mediaPlayerService?.resetEndOfPlaybackFlag()
                } else {
                    // We're not at the end yet, so play the next song
                    Log.d(TAG, "Playing next song (not at end of playlist)")
                    playNext()
                }
            }
        }
    }

    // Reorder song in queue
    fun moveSongInQueue(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        val currentQueue = _queue.value.toMutableList()
        if (fromIndex < 0 || fromIndex >= currentQueue.size ||
            toIndex < 0 || toIndex >= currentQueue.size) {
            Log.e(TAG, "Invalid indices for queue reordering: from=$fromIndex, to=$toIndex")
            return
        }

        // Move the item in the queue
        val movedItem = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, movedItem)
        _queue.value = currentQueue

        // Update current index if necessary
        if (_currentQueueIndex.value == fromIndex) {
            _currentQueueIndex.value = toIndex
        } else if (fromIndex < _currentQueueIndex.value && _currentQueueIndex.value <= toIndex) {
            // Item moved from before current to after current
            _currentQueueIndex.value = _currentQueueIndex.value - 1
        } else if (toIndex <= _currentQueueIndex.value && _currentQueueIndex.value < fromIndex) {
            // Item moved from after current to before current
            _currentQueueIndex.value = _currentQueueIndex.value + 1
        }

        Log.d(TAG, "Moved song in queue from $fromIndex to $toIndex, new queue size: ${currentQueue.size}")
    }
}