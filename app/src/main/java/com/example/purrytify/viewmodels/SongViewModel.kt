package com.example.purrytify.viewmodels


import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.purrytify.data.entity.Song
import com.example.purrytify.data.repository.SongRepository
import kotlinx.coroutines.launch

class SongViewModel(private val repository: SongRepository) : ViewModel() {
    // LiveData untuk semua lagu
    val allSongs: LiveData<List<Song>> = repository.allSongs
    val likedSongs: LiveData<List<Song>> = repository.likedSongs
    val recentlyPlayedSongs: LiveData<List<Song>> = repository.recentlyPlayed

    // Fungsi untuk mendapatkan lagu berdasarkan ID
    fun getSongById(id: Long): LiveData<Song> {
        return repository.getSongById(id)
    }

    // Fungsi untuk menambahkan lagu baru
    fun insertSong(title: String, artist: String, filePath: String, artworkPath: String, duration: Long) = viewModelScope.launch {
        val song = Song(
            title = title,
            artist = artist,
            filePath = filePath,
            artworkPath = artworkPath,
            duration = duration,
            isLiked = false,
            addedAt = System.currentTimeMillis()
        )
        repository.insert(song)
    }

    // Fungsi untuk mengupdate lagu
    fun updateSong(song: Song) = viewModelScope.launch {
        repository.update(song)
    }

    // Fungsi untuk menghapus lagu
    fun deleteSong(song: Song) = viewModelScope.launch {
        repository.delete(song)
    }

    // Fungsi untuk toggle like lagu
    fun toggleLikeSong(songId: Long, isLiked: Boolean) = viewModelScope.launch {
        repository.toggleLike(songId, isLiked)
    }

    // Fungsi untuk update last played
    fun updateLastPlayed(songId: Long) = viewModelScope.launch {
        repository.updateLastPlayed(songId)
    }
}