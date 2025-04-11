package com.example.purrytify.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.purrytify.viewmodels.MainViewModel

/**
 * Broadcast receiver to handle song completion events from the MediaPlayerService
 * This receiver is registered via LocalBroadcastManager and only receives internal app broadcasts
 */
class SongCompletionReceiver(private val mainViewModel: MainViewModel) : BroadcastReceiver() {
    private val TAG = "SongCompletionReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.purrytify.SONG_COMPLETED") {
            Log.d(TAG, "Received song completion broadcast")
            // Play the next song
            mainViewModel.playNext()
        }
    }
}