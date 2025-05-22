package com.example.purrytify.viewmodels

import androidx.lifecycle.ViewModel
import com.example.purrytify.R
import com.example.purrytify.models.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LibraryViewModel : ViewModel() {
    // Sample data for songs
    private val _songs = MutableStateFlow(createSampleSongs())
    val songs: StateFlow<List<Song>> = _songs

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

    // Play a song
    fun playSong(song: Song) {
        _currentSong.value = song
        _isPlaying.value = true

        // Update the list to mark the current song as playing
        _songs.value = _songs.value.map {
            it.copy(isPlaying = it.id == song.id)
        }
    }

    // Sample data
    private fun createSampleSongs(): List<Song> {
        return listOf(
            Song(
                id = "1",
                title = "Starboy",
                artist = "The Weeknd, Daft Punk",
                albumArt = R.drawable.ic_launcher_foreground,
                isLiked = true
            ),
            Song(
                id = "2",
                title = "Here Comes The Sun - Remastered",
                artist = "The Beatles",
                albumArt = R.drawable.ic_launcher_foreground,
                isLiked = true
            ),
            // Add more sample songs...
        )
    }
}