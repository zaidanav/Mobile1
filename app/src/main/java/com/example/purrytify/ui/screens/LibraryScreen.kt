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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.models.Song
import com.example.purrytify.ui.components.DeleteSongDialog
import com.example.purrytify.ui.components.EditSongDialog
import com.example.purrytify.ui.components.LibraryTabs
import com.example.purrytify.ui.components.SongItem
import com.example.purrytify.ui.components.UploadSongDialog
import com.example.purrytify.viewmodels.LibraryViewModel
import com.example.purrytify.viewmodels.MainViewModel
import com.example.purrytify.viewmodels.OperationStatus
import com.example.purrytify.viewmodels.ViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel = viewModel(
        factory = ViewModelFactory.getInstance(LocalContext.current)
    ),
    mainViewModel: MainViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = ViewModelFactory.getInstance(LocalContext.current)
    ),
    onSongSelected: (Song) -> Unit = {}
) {
    // State for tab that is selected
    var selectedTab by remember { mutableStateOf("All") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedSongForEdit by remember { mutableStateOf<Song?>(null) }
    var selectedSongForDelete by remember { mutableStateOf<Song?>(null) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // For showing feedback when adding to queue
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Get data from ViewModel
    val songs by libraryViewModel.songs.collectAsState()
    val currentSong by mainViewModel.currentSong.collectAsState()
    val isPlaying by mainViewModel.isPlaying.collectAsState()
    val operationStatus by libraryViewModel.operationStatus.collectAsState()

    // Handle operation status
    operationStatus?.let { status ->
        when (status) {
            is OperationStatus.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar(status.message)
                    libraryViewModel.clearOperationStatus()
                }
            }
            is OperationStatus.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(status.message)
                    libraryViewModel.clearOperationStatus()
                }
            }
        }
    }

    // Filter songs based on search query
    val filteredSongs = remember(songs, searchQuery, selectedTab) {
        val filteredByTab = if (selectedTab == "All") songs else songs.filter { it.isLiked }

        if (searchQuery.isBlank()) {
            filteredByTab
        } else {
            filteredByTab.filter { song ->
                song.title.contains(searchQuery, ignoreCase = true) ||
                        song.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

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

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    isSearchActive = it.isNotEmpty()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search songs or artists", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray
                    )
                },
                trailingIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            searchQuery = ""
                            isSearchActive = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                    .padding(top = 8.dp)
            ) {
                // Display filtered songs
                if (filteredSongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSearchActive)
                                    "No songs found matching \"$searchQuery\""
                                else
                                    "No songs in your library",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    items(filteredSongs) { song ->
                        SongItem(
                            song = song.copy(isPlaying = currentSong?.id == song.id && isPlaying),
                            onSongClick = { clickedSong ->
                                libraryViewModel.playSong(clickedSong)
                                onSongSelected(clickedSong)
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
                            },
                            onEditSong = { songToEdit ->
                                selectedSongForEdit = songToEdit
                                showEditDialog = true
                            },
                            onDeleteSong = { songToDelete ->
                                selectedSongForDelete = songToDelete
                                showDeleteDialog = true
                            }
                        )
                    }
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
                    scope.launch {
                        snackbarHostState.showSnackbar("Song added: $title")
                    }
                    showUploadDialog = false
                }
            )
        }

        // Edit Song Dialog
        if (showEditDialog && selectedSongForEdit != null) {
            EditSongDialog(
                song = selectedSongForEdit!!,
                onDismiss = { showEditDialog = false },
                onSaveClick = { title, artist, filePath, artworkPath, duration ->
                    libraryViewModel.editSong(
                        selectedSongForEdit!!.id,
                        title,
                        artist,
                        filePath,
                        artworkPath,
                        duration,
                        mainViewModel
                    )
                    showEditDialog = false
                }
            )
        }

        // Delete Song Dialog
        if (showDeleteDialog && selectedSongForDelete != null) {
            DeleteSongDialog(
                song = selectedSongForDelete!!,
                onDismiss = { showDeleteDialog = false },
                onConfirmDelete = { songToDelete ->
                    libraryViewModel.deleteSong(songToDelete.id, mainViewModel)
                    showDeleteDialog = false
                }
            )
        }

        // Snackbar for showing messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}