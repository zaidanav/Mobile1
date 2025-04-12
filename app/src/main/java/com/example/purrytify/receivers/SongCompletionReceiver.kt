package com.example.purrytify.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.purrytify.viewmodels.MainViewModel

/**
 * Broadcast receiver that listens for song completion events
 * and triggers the next song to play based on queue settings.
 */
class SongCompletionReceiver(private val mainViewModel: MainViewModel) : BroadcastReceiver() {

    private val TAG = "SongCompletionReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.example.purrytify.SONG_COMPLETED") {
            Log.d(TAG, "Received song completion broadcast")

            // Check if this is the end of the playlist without repeat mode
            val isEndOfPlayback = intent.getBooleanExtra("END_OF_PLAYBACK", false)

            // Notify the MainViewModel that the song has completed
            mainViewModel.onSongCompleted(isEndOfPlayback)
        }
    }
}