package com.example.purrytify.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay

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
    val currentQueueIndex = remember { mutableStateOf(-1) }

    // State for confirm clear dialog
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // State for showing feedback when changes are made
    var showFeedback by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }

    // Find current song index in queue
    LaunchedEffect(currentSong, queue) {
        currentSong?.let { song ->
            val index = queue.indexOfFirst { it.id == song.id }
            currentQueueIndex.value = index
        }
    }

    // Calculate upcoming songs (only songs that haven't been played yet)
    val upcomingSongs = remember(queue, currentQueueIndex.value) {
        if (currentQueueIndex.value >= 0 && currentQueueIndex.value < queue.size) {
            // Get only songs after the current playing song
            queue.subList(currentQueueIndex.value + 1, queue.size)
        } else {
            queue
        }
    }

    // Scrolling state for the list
    val listState = rememberLazyListState()

    // Handle feedback display
    LaunchedEffect(showFeedback) {
        if (showFeedback) {
            delay(2000)
            showFeedback = false
        }
    }

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
                        if (queue.isNotEmpty()) {
                            showClearConfirmDialog = true
                        }
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

            // Queue list - now showing only upcoming songs
            if (upcomingSongs.isEmpty()) {
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
                    modifier = Modifier.weight(1f),
                    state = listState
                ) {
                    itemsIndexed(upcomingSongs) { index, song ->
                        // Calculate the actual queue index for removal
                        val actualQueueIndex = currentQueueIndex.value + 1 + index

                        SongItem(
                            song = song,
                            onSongClick = {
                                mainViewModel.playSong(it)
                            },
                            onRemoveSong = {
                                mainViewModel.removeFromQueue(actualQueueIndex)
                                feedbackMessage = "Song removed from queue"
                                showFeedback = true
                            }
                        )
                    }
                }
            }

            // Bottom space for mini player
            Spacer(modifier = Modifier.height(80.dp))
        }

        // Confirm clear queue dialog
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("Clear Queue") },
                text = { Text("Are you sure you want to clear the entire queue?") },
                confirmButton = {
                    Button(
                        onClick = {
                            mainViewModel.clearQueue()
                            showClearConfirmDialog = false
                            feedbackMessage = "Queue cleared"
                            showFeedback = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GREEN_COLOR)
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showClearConfirmDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Cancel")
                    }
                },
                containerColor = Color(0xFF2A2A2A),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }

        // Feedback toast
        AnimatedVisibility(
            visible = showFeedback,
            enter = fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(GREEN_COLOR.copy(alpha = 0.9f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = feedbackMessage,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}