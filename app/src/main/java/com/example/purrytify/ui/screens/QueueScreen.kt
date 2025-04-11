package com.example.purrytify.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.models.Song
import com.example.purrytify.ui.components.SongItem
import com.example.purrytify.ui.theme.BACKGROUND_COLOR
import com.example.purrytify.ui.theme.GREEN_COLOR
import com.example.purrytify.viewmodels.MainViewModel
import com.example.purrytify.viewmodels.ViewModelFactory

@Composable
fun QueueScreen(
    onNavigateBack: () -> Unit,
    mainViewModel: MainViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = ViewModelFactory.getInstance(LocalContext.current)
    )
) {
    val queue by mainViewModel.queue.collectAsState()
    val currentSong by mainViewModel.currentSong.collectAsState()
    val isPlaying by mainViewModel.isPlaying.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND_COLOR)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Queue",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Clear queue button
                IconButton(
                    onClick = {
                        mainViewModel.clearQueue()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Queue",
                        tint = Color.White
                    )
                }
            }

            // Divider
            Divider(color = Color.DarkGray, thickness = 1.dp)

            // Now Playing section
            currentSong?.let { song ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "NOW PLAYING",
                        color = GREEN_COLOR,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    SongItem(
                        song = song.copy(isPlaying = isPlaying),
                        onSongClick = { mainViewModel.playSong(it) }
                    )
                }

                Divider(color = Color.DarkGray, thickness = 1.dp)
            }

            // Queue title
            Text(
                text = "NEXT FROM QUEUE",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            // Queue list
            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your queue is empty",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(queue) { index, song ->
                        // Skip the current song as it's already displayed in Now Playing
                        if (song.id != currentSong?.id) {
                            SongItem(
                                song = song,
                                onSongClick = {
                                    mainViewModel.playSong(it)
                                },
                                onRemoveSong = {
                                    mainViewModel.removeFromQueue(index)
                                }
                            )
                        }
                    }
                }
            }

            // Bottom space for mini player
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}