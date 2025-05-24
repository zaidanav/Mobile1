package com.example.purrytify.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.R
import com.example.purrytify.models.Song
import com.example.purrytify.models.RecommendationPlaylist
import com.example.purrytify.ui.components.NewSongs
import com.example.purrytify.ui.components.SongItem
import com.example.purrytify.ui.components.RecommendationSection
import com.example.purrytify.ui.components.RecommendationPlaylistView
import com.example.purrytify.ui.theme.BACKGROUND_COLOR
import com.example.purrytify.viewmodels.HomeViewModel
import com.example.purrytify.viewmodels.MainViewModel
import com.example.purrytify.viewmodels.RecommendationViewModel
import com.example.purrytify.viewmodels.ViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    // HomeViewModel for managing new songs and recently played songs
    val homeViewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory.getInstance(context)
    )

    // MainViewModel for managing the current song and playback state
    val mainViewModel: MainViewModel = viewModel(
        viewModelStoreOwner = context as ComponentActivity,
        factory = ViewModelFactory.getInstance(context)
    )

    // RecommendationViewModel for managing recommendations
    val recommendationViewModel: RecommendationViewModel = viewModel(
        factory = ViewModelFactory.getInstance(context)
    )

    // Observe data dari ViewModels
    val newSongs by homeViewModel.newSongs.observeAsState(emptyList())
    val recentlyPlayed by homeViewModel.recentlyPlayed.observeAsState(emptyList())
    val currentSong by mainViewModel.currentSong.collectAsState()
    val isPlaying by mainViewModel.isPlaying.collectAsState()

    // Recommendation data
    val recommendations by recommendationViewModel.recommendations.collectAsState()
    val isRecommendationsLoading by recommendationViewModel.isLoading.collectAsState()
    val recommendationError by recommendationViewModel.error.collectAsState()

    // For showing feedback when adding to queue
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for showing playlist view
    var selectedPlaylist by remember { mutableStateOf<RecommendationPlaylist?>(null) }

    // Load recommendations on first composition
    LaunchedEffect(key1 = Unit) {
        recommendationViewModel.loadRecommendations()
    }

    // Show error message if any
    LaunchedEffect(recommendationError) {
        recommendationError?.let { error ->
            snackbarHostState.showSnackbar("Error loading recommendations: $error")
            recommendationViewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND_COLOR)
    ) {
        if (selectedPlaylist != null) {
            // Show playlist view
            RecommendationPlaylistView(
                playlist = selectedPlaylist!!,
                onSongClick = { song ->
                    // PERBAIKAN: Hanya panggil mainViewModel.playSong untuk menghindari konflik
                    mainViewModel.playSong(song)
                },
                onAddToQueue = { song ->
                    mainViewModel.addToQueue(song)
                    scope.launch {
                        snackbarHostState.showSnackbar("Added to queue: ${song.title}")
                    }
                },
                onBackClick = {
                    selectedPlaylist = null
                }
            )
        } else {
            // Show main home screen
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 20.dp)
            ) {
                // Recommendations Section
                item {
                    RecommendationSection(
                        recommendations = recommendations,
                        onPlaylistClick = { playlist ->
                            selectedPlaylist = playlist
                        },
                        onSongClick = { song ->
                            // PERBAIKAN: Hanya panggil mainViewModel.playSong untuk menghindari konflik
                            mainViewModel.playSong(song)
                        },
                        onRefreshClick = {
                            recommendationViewModel.refreshRecommendations()
                        },
                        isLoading = isRecommendationsLoading
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Section title: New Songs
                item {
                    Text(
                        text = "New Songs",
                        color = Color.White,
                        fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
                        fontSize = 24.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Horizontal scrollable songs
                item {
                    NewSongs(
                        songs = newSongs,
                        onSongClick = { song ->
                            // PERBAIKAN: Hanya panggil mainViewModel.playSong untuk menghindari konflik
                            mainViewModel.playSong(song)
                        },
                        onAddToQueue = { song ->
                            mainViewModel.addToQueue(song)
                            scope.launch {
                                snackbarHostState.showSnackbar("Added to queue: ${song.title}")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Section title: Recently Played
                item {
                    Text(
                        text = "Recently Played",
                        color = Color.White,
                        fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
                        fontSize = 24.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Show recently played songs as individual items
                if (recentlyPlayed.isNotEmpty()) {
                    items(recentlyPlayed) { song ->
                        SongItem(
                            song = song.copy(isPlaying = currentSong?.id == song.id && isPlaying),
                            onSongClick = { clickedSong ->
                                // PERBAIKAN: Hanya panggil mainViewModel.playSong untuk menghindari konflik
                                mainViewModel.playSong(clickedSong)
                            },
                            onAddToQueue = { queuedSong ->
                                mainViewModel.addToQueue(queuedSong)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Added to queue: ${queuedSong.title}")
                                }
                            },
                            onToggleLike = { likedSong, isLiked ->
                                mainViewModel.toggleLike(likedSong.id, isLiked)
                            }
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "No recently played songs",
                            color = Color.Gray,
                            fontFamily = FontFamily(Font(R.font.poppins_regular)),
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                // Bottom spacing for better scrolling experience
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Snackbar for showing queue messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}