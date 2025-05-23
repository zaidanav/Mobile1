package com.example.purrytify.repository

import android.util.Log
import com.example.purrytify.data.dao.SongDao
import com.example.purrytify.data.entity.Song
import com.example.purrytify.models.OnlineSong
import com.example.purrytify.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnlineSongsRepository(private val songDao: SongDao) {

    private val TAG = "OnlineSongsRepository"

    // Get global top songs
    suspend fun getGlobalTopSongs(): Result<List<OnlineSong>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getGlobalTopSongs()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to load global top songs: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching global top songs", e)
                Result.failure(Exception("Error fetching global top songs: ${e.message}"))
            }
        }
    }

    // Get country top songs
    suspend fun getCountryTopSongs(countryCode: String): Result<List<OnlineSong>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getCountryTopSongs(countryCode)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to load country top songs: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching country top songs", e)
                Result.failure(Exception("Error fetching country top songs: ${e.message}"))
            }
        }
    }

    // Save downloaded song
    suspend fun saveDownloadedSong(onlineSong: OnlineSong, localFilePath: String, userId: Int): Long {
        return withContext(Dispatchers.IO) {
            try {
                val song = Song(
                    title = onlineSong.title,
                    artist = onlineSong.artist,
                    filePath = localFilePath,
                    artworkPath = onlineSong.artworkUrl,
                    duration = onlineSong.getDurationInMillis(),
                    addedAt = System.currentTimeMillis(),
                    userId = userId,
                    isOnline = true,
                    onlineId = onlineSong.id
                )

                songDao.insertSong(song)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving downloaded song", e)
                -1L
            }
        }
    }

    // Check if song is already downloaded
    suspend fun isSongDownloaded(onlineId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val count = songDao.getCountByOnlineId(onlineId)
                count > 0
            } catch (e: Exception) {
                Log.e(TAG, "Error checking if song is downloaded", e)
                false
            }
        }
    }

    // Get downloaded online songs
    fun getDownloadedOnlineSongs() = songDao.getDownloadedOnlineSongs()

    // Get song by online ID
    suspend fun getSongByOnlineId(onlineId: Int): Song? {
        return withContext(Dispatchers.IO) {
            try {
                songDao.getSongByOnlineId(onlineId)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting song by online ID", e)
                null
            }
        }
    }
}