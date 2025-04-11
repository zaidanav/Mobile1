package com.example.purrytify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purrytify.models.Song

@Composable
fun SongItem(
    song: Song,
    onSongClick: (Song) -> Unit,
    onAddToQueue: ((Song) -> Unit)? = null,
    onRemoveSong: ((Song) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAddToQueueDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSongClick(song) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Art using our custom component
        SongCoverImage(
            imageUrl = song.coverUrl,
            contentDescription = "${song.title} album art",
            modifier = Modifier.size(50.dp)
        )

        // Song Info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = song.title,
                color = if (song.isPlaying) Color.Green else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = song.artist,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        // Only show if this is the currently playing song
        if (song.isPlaying) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Now Playing",
                tint = Color.Green,
                modifier = Modifier.size(24.dp)
            )
        }

        // More options button
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More Options",
                tint = Color.White
            )
        }

        // Dropdown menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(Color(0xFF333333))
        ) {
            // Add to Queue option (if handler is provided)
            if (onAddToQueue != null) {
                DropdownMenuItem(
                    text = { Text("Add to Queue", color = Color.White) },
                    onClick = {
                        showMenu = false
                        showAddToQueueDialog = true
                    }
                )
            }

            // Like/Unlike option
            DropdownMenuItem(
                text = {
                    Text(
                        if (song.isLiked) "Remove from Liked" else "Add to Liked",
                        color = Color.White
                    )
                },
                onClick = {
                    // This will be implemented later
                    showMenu = false
                }
            )

            // Remove song option (if handler is provided)
            if (onRemoveSong != null) {
                DropdownMenuItem(
                    text = { Text("Remove Song", color = Color.Red) },
                    onClick = {
                        showMenu = false
                        onRemoveSong(song)
                    }
                )
            }
        }
    }

    // Show Add to Queue dialog if requested
    if (showAddToQueueDialog && onAddToQueue != null) {
        AddToQueueDialog(
            song = song,
            onDismiss = { showAddToQueueDialog = false },
            onAddToQueue = onAddToQueue
        )
    }
}