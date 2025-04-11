package com.example.purrytify.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.entity.Song as EntitySong
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.models.Song
import com.example.purrytify.util.SongMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel untuk mengelola data HomeScreen
 * Menyediakan akses ke daftar lagu terbaru dan recently played
 */
class HomeViewModel(private val songRepository: SongRepository) : ViewModel() {

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

    init {
        // Load initial data
        loadSongs()

        // Set up observers for repository data
        setupObservers()
    }

    private fun setupObservers() {
        // Observe allSongs from repository
        songRepository.allSongs.observeForever { entityList ->
            Log.d("HomeViewModel", "All songs updated, count: ${entityList.size}")
            _allSongs.value = convertToModelSongs(entityList)
        }

        // Observe recentlyPlayed from repository
        songRepository.recentlyPlayed.observeForever { entityList ->
            Log.d("HomeViewModel", "Recently played updated, count: ${entityList.size}")
            _recentlyPlayed.value = convertToModelSongs(entityList)
        }
    }

    private fun loadSongs() {
        viewModelScope.launch {
            try {
                // We'll just wait for the observers to update the LiveData
                Log.d("HomeViewModel", "Loading songs")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading songs", e)
            }
        }
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
        Log.d("HomeViewModel", "Playing song: ${song.title}")
        _playingSongId.value = song.id
        _isPlaying.value = true

        // Update timestamp lagu terakhir diputar
        viewModelScope.launch {
            try {
                songRepository.updateLastPlayed(song.id)

                // Update UI dengan status playing yang baru
                updateSongPlayingStatus()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error updating last played timestamp", e)
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
                Log.e("HomeViewModel", "Error toggling like status", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Remove observers to prevent memory leaks
        songRepository.allSongs.removeObserver { }
        songRepository.recentlyPlayed.removeObserver { }
    }
}