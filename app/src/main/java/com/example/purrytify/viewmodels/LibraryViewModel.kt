package com.example.purrytify.viewmodels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.models.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: SongRepository) : ViewModel() {

    // StateFlow untuk daftar lagu
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    // StateFlow untuk lagu yang sedang diputar
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    // Status pemutaran
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allSongs.asFlow().collect { dbSongs ->
                _songs.value = dbSongs.map { dbSong ->
                    Song(
                        id = dbSong.id,
                        title = dbSong.title,
                        artist = dbSong.artist,
                        coverUrl = dbSong.artworkPath,
                        filePath = dbSong.filePath,
                        duration = dbSong.duration,
                        isLiked = dbSong.isLiked,
                        isPlaying = false
                    )
                }
            }
        }
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _isPlaying.value = true

        // Update last played timestamp in database
        viewModelScope.launch {
            repository.updateLastPlayed(song.id)
        }
    }

    fun pauseSong() {
        _isPlaying.value = false
    }

    fun resumeSong() {
        _isPlaying.value = true
    }

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            repository.toggleLike(song.id, !song.isLiked)
        }
    }

    fun addSong(title: String, artist: String, filePath: String, artworkPath: String, duration: Long) {
        viewModelScope.launch {
            val newSong = com.example.purrytify.data.entity.Song(
                title = title,
                artist = artist,
                filePath = filePath,
                artworkPath = artworkPath,
                duration = duration,
                isLiked = false,
                lastPlayed = null,
                addedAt = System.currentTimeMillis()
            )
            repository.insert(newSong)
        }
    }
}