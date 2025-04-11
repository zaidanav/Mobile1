package com.example.purrytify.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.models.Song
import com.example.purrytify.ui.components.LibraryTabs
import com.example.purrytify.ui.components.SongItem
import com.example.purrytify.ui.components.UploadSongDialog
import com.example.purrytify.viewmodels.LibraryViewModel
import com.example.purrytify.viewmodels.MainViewModel
import com.example.purrytify.viewmodels.ViewModelFactory

@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel = viewModel(
        factory = ViewModelFactory.getInstance(LocalContext.current)
    ),
    mainViewModel: MainViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = ViewModelFactory.getInstance(LocalContext.current)
    ),
    onSongSelected: (Song) -> Unit ={}
) {
    // State untuk tab yang dipilih
    var selectedTab by remember { mutableStateOf("All") }
    var showUploadDialog by remember { mutableStateOf(false) }

    // Ambil data dari ViewModel
    val songs by libraryViewModel.songs.collectAsState()
    val currentSong by mainViewModel.currentSong.collectAsState()
    val isPlaying by mainViewModel.isPlaying.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Library Header with Add Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Your Library",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { showUploadDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Song",
                        tint = Color.White
                    )
                }
            }

            // Tabs (All/Liked)
            LibraryTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Songs List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val displayedSongs = if (selectedTab == "All") songs else songs.filter { it.isLiked }

                items(displayedSongs) { song ->
                    SongItem(
                        song = song.copy(isPlaying = currentSong?.id == song.id && isPlaying),
                        onSongClick = { clickedSong ->
                            libraryViewModel.playSong(clickedSong)
                            onSongSelected(clickedSong)
                            mainViewModel.playSong(clickedSong)
                        }
                    )
                }
            }

            // Space for Mini Player if needed
            if (currentSong != null) {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // Upload Song Dialog
        if (showUploadDialog) {
            UploadSongDialog(
                onDismiss = { showUploadDialog = false },
                onSaveClick = { title, artist, filePath, artworkPath, duration ->
                    libraryViewModel.addSong(title, artist, filePath, artworkPath, duration)
                    showUploadDialog = false
                }
            )
        }
    }
}