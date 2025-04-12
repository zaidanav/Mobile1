package com.example.purrytify.data.repository

import androidx.lifecycle.LiveData
import com.example.purrytify.data.dao.SongDao
import com.example.purrytify.data.entity.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class SongRepository(private val songDao: SongDao) {
    val allSongs: LiveData<List<Song>> = songDao.getAllSongs()
    val likedSongs: LiveData<List<Song>> = songDao.getLikedSongs()
    val recentlyPlayed: LiveData<List<Song>> = songDao.getRecentlyPlayed()

    suspend fun insert(song: Song): Long {
        return withContext(Dispatchers.IO) {
            songDao.insertSong(song)
        }
    }

    suspend fun update(song: Song): Int {
        return withContext(Dispatchers.IO) {
            songDao.updateSong(song)
        }
    }

    suspend fun delete(song: Song): Int {
        return withContext(Dispatchers.IO) {
            songDao.deleteSong(song)
        }
    }

    suspend fun toggleLike(songId: Long, isLiked: Boolean): Int {
        return withContext(Dispatchers.IO) {
            songDao.updateLikeStatus(songId, isLiked)
        }
    }

    suspend fun updateLastPlayed(songId: Long, timestamp: Long = System.currentTimeMillis()): Int {
        return withContext(Dispatchers.IO) {
            songDao.updateLastPlayed(songId, timestamp)
        }
    }

    fun getSongById(songId: Long): LiveData<Song> {
        return songDao.getSongById(songId)
    }

    // New methods for statistics

    // Count all songs
    fun getAllSongsCount(): Flow<Int> = flow {
        val count = withContext(Dispatchers.IO) {
            songDao.countAllSongs()
        }
        emit(count)
    }.flowOn(Dispatchers.IO)

    // Count liked songs
    fun getLikedSongsCount(): Flow<Int> = flow {
        val count = withContext(Dispatchers.IO) {
            songDao.countLikedSongs()
        }
        emit(count)
    }.flowOn(Dispatchers.IO)

    // Count songs that have been played
    fun getListenedSongsCount(): Flow<Int> = flow {
        val count = withContext(Dispatchers.IO) {
            songDao.countListenedSongs()
        }
        emit(count)
    }.flowOn(Dispatchers.IO)
}