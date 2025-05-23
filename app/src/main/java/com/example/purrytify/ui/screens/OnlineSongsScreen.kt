package com.example.purrytify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.models.OnlineSong
import com.example.purrytify.ui.components.NoInternetScreen
import com.example.purrytify.ui.components.OnlineSongItem
import com.example.purrytify.ui.theme.BACKGROUND_COLOR
import com.example.purrytify.ui.theme.GREEN_COLOR
import com.example.purrytify.util.NetworkUtils
import com.example.purrytify.viewmodels.OnlineSongsViewModel
import com.example.purrytify.viewmodels.OnlineSongsViewModelFactory

@Composable
fun OnlineSongsScreen(
    onSongSelected: (OnlineSong) -> Unit,
    onDownloadClick: ((OnlineSong) -> Unit)? = null,
    viewModel: OnlineSongsViewModel = viewModel(
        factory = OnlineSongsViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current

    // Check network connection
    val isNetworkAvailable = remember { mutableStateOf(NetworkUtils.isNetworkAvailable(context)) }

    // Observe data from ViewModel
    val globalTopSongs by viewModel.globalTopSongs.observeAsState(emptyList())
    val countryTopSongs by viewModel.countryTopSongs.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState(null)
    val downloadedSongIds by viewModel.downloadedSongIds.observeAsState(emptySet())
    val downloadingSongs by viewModel.downloadingSongs.observeAsState(emptySet())

    // Tab selection state
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Global Top 50", "Country Top 10")

    // Load data on first composition
    LaunchedEffect(key1 = Unit) {
        viewModel.loadData()
    }

    // Show no internet screen if not connected
    if (!isNetworkAvailable.value) {
        NoInternetScreen(modifier = Modifier.fillMaxSize())
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND_COLOR)
    ) {
        Column {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF121212),
                contentColor = GREEN_COLOR
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }

            // Content based on selected tab
            when (selectedTabIndex) {
                0 -> {
                    // Global Top 50
                    if (isLoading && globalTopSongs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = GREEN_COLOR)
                        }
                    } else if (error != null && globalTopSongs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Error: $error",
                                color = Color.Red,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text(
                                    text = "Global Top 50",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            itemsIndexed(globalTopSongs) { index, song ->
                                val isDownloaded = viewModel.isSongDownloaded(song.id)
                                val isDownloading = viewModel.isSongDownloading(song.id)

                                OnlineSongItem(
                                    song = song,
                                    isPlaying = false, // Update based on currently playing song
                                    isDownloaded = isDownloaded,
                                    isDownloading = isDownloading,
                                    showRank = true,
                                    rank = index + 1,
                                    onSongClick = onSongSelected,
                                    onDownloadClick = onDownloadClick
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Country Top 10
                    if (isLoading && countryTopSongs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = GREEN_COLOR)
                        }
                    } else if (error != null && countryTopSongs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Error: $error",
                                color = Color.Red,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text(
                                    text = "Country Top 10",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            itemsIndexed(countryTopSongs) { index, song ->
                                val isDownloaded = viewModel.isSongDownloaded(song.id)
                                val isDownloading = viewModel.isSongDownloading(song.id)

                                OnlineSongItem(
                                    song = song,
                                    isPlaying = false, // Update based on currently playing song
                                    isDownloaded = isDownloaded,
                                    isDownloading = isDownloading,
                                    showRank = true,
                                    rank = index + 1,
                                    onSongClick = onSongSelected,
                                    onDownloadClick = onDownloadClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}