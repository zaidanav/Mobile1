package com.example.purrytify.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.purrytify.data.entity.Song

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY added_at DESC")
    fun getAllSongs(): LiveData<List<Song>>

    @Query("SELECT * FROM songs WHERE is_liked = 1 ORDER BY title ASC")
    fun getLikedSongs(): LiveData<List<Song>>

    @Query("SELECT * FROM songs WHERE last_played IS NOT NULL ORDER BY last_played DESC LIMIT 10")
    fun getRecentlyPlayed(): LiveData<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    fun getSongById(songId: Long): LiveData<Song>

    @Insert
    fun insertSong(song: Song): Long

    @Update
    fun updateSong(song: Song): Int

    @Delete
    fun deleteSong(song: Song): Int

    @Query("UPDATE songs SET is_liked = :isLiked WHERE id = :songId")
    fun updateLikeStatus(songId: Long, isLiked: Boolean): Int

    @Query("UPDATE songs SET last_played = :timestamp WHERE id = :songId")
    fun updateLastPlayed(songId: Long, timestamp: Long): Int

    // New queries for statistics

    @Query("SELECT COUNT(*) FROM songs")
    fun countAllSongs(): Int

    @Query("SELECT COUNT(*) FROM songs WHERE is_liked = 1")
    fun countLikedSongs(): Int

    @Query("SELECT COUNT(*) FROM songs WHERE last_played IS NOT NULL")
    fun countListenedSongs(): Int
}