package com.example.purrytify.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.entity.Song as EntitySong
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.models.Song
import com.example.purrytify.util.SongMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val songRepository: SongRepository) : ViewModel() {
    private val TAG = "HomeViewModel"

    // ID lagu yang sedang diputar
    private val _playingSongId = MutableStateFlow<Long>(-1)
    val playingSongId: StateFlow<Long> = _playingSongId

    // State untuk playing status
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    // LiveData untuk model UI
    private val _allSongs = MutableLiveData<List<Song>>(emptyList())
    val allSongs: LiveData<List<Song>> = _allSongs

    private val _recentlyPlayed = MutableLiveData<List<Song>>(emptyList())
    val recentlyPlayed: LiveData<List<Song>> = _recentlyPlayed

    // New songs (5 terbaru)
    private val _newSongs = MutableLiveData<List<Song>>(emptyList())
    val newSongs: LiveData<List<Song>> = _newSongs

    // Store observers so we can clean them up
    private val allSongsObserver = Observer<List<EntitySong>> { entityList ->
        Log.d(TAG, "All songs updated, count: ${entityList.size}")
        _allSongs.value = convertToModelSongs(entityList)
    }

    private val recentlyPlayedObserver = Observer<List<EntitySong>> { entityList ->
        Log.d(TAG, "Recently played updated, count: ${entityList.size}")
        _recentlyPlayed.value = convertToModelSongs(entityList)
    }

    private val newSongsObserver = Observer<List<EntitySong>> { entityList ->
        Log.d(TAG, "New songs updated, count: ${entityList.size}")
        _newSongs.value = convertToModelSongs(entityList)
    }

    init {
        // Set up observers for repository data
        setupObservers()
    }

    private fun setupObservers() {
        // Observe allSongs from repository
        songRepository.allSongs.observeForever(allSongsObserver)

        // Observe recentlyPlayed from repository
        songRepository.recentlyPlayed.observeForever(recentlyPlayedObserver)

        // Observe new songs
        songRepository.getNewestSongs().observeForever(newSongsObserver)
    }

    private fun convertToModelSongs(entityList: List<EntitySong>): List<Song> {
        return entityList.map { entity ->
            SongMapper.fromEntity(entity, entity.id == _playingSongId.value)
        }
    }

    /**
     * Memutar lagu yang dipilih
     * @param song Lagu yang akan diputar
     */
    fun playSong(song: Song) {
        Log.d(TAG, "Playing song: ${song.title}")
        _playingSongId.value = song.id
        _isPlaying.value = true

        // Update timestamp lagu terakhir diputar
        viewModelScope.launch {
            try {
                songRepository.updateLastPlayed(song.id)

                // Update UI dengan status playing yang baru
                updateSongPlayingStatus()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating last played timestamp", e)
            }
        }
    }

    /**
     * Update status playing pada semua lagu
     */
    private fun updateSongPlayingStatus() {
        val currentPlayingSongId = _playingSongId.value

        // Update allSongs
        _allSongs.value = _allSongs.value?.map { song ->
            song.copy(isPlaying = song.id == currentPlayingSongId)
        }

        // Update recentlyPlayed
        _recentlyPlayed.value = _recentlyPlayed.value?.map { song ->
            song.copy(isPlaying = song.id == currentPlayingSongId)
        }

        // Update newSongs
        _newSongs.value = _newSongs.value?.map { song ->
            song.copy(isPlaying = song.id == currentPlayingSongId)
        }
    }

    /**
     * Toggle playback status
     */
    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    /**
     * Toggle status like lagu
     * @param songId ID lagu
     * @param isLiked status like baru
     */
    fun toggleLike(songId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            try {
                songRepository.toggleLike(songId, isLiked)
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling like status", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Properly remove observers to prevent memory leaks
        songRepository.allSongs.removeObserver(allSongsObserver)
        songRepository.recentlyPlayed.removeObserver(recentlyPlayedObserver)
        try {
            songRepository.getNewestSongs().removeObserver(newSongsObserver)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing new songs observer", e)
        }
    }
}