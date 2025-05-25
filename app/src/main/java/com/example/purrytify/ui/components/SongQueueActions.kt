package com.example.purrytify.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
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
import androidx.compose.ui.unit.dp
import com.example.purrytify.models.Song

/**
 * Queue-related actions for songs
 * Can be used in multiple places throughout the app
 */
@Composable
fun SongQueueActions(
    song: Song,
    onAddToQueue: (Song) -> Unit,
    showAddToQueueDialog: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    // Click handler for adding to queue
    val handleAddToQueue = {
        if (showAddToQueueDialog) {
            showDialog = true
        } else {
            onAddToQueue(song)
        }
    }

    // Show the add to queue dialog if requested
    if (showDialog) {
        AddToQueueDialog(
            song = song,
            onDismiss = { showDialog = false },
            onAddToQueue = {
                onAddToQueue(it)
                showDialog = false
            }
        )
    }

    // Queue action icon
    IconButton(onClick = handleAddToQueue) {
        Icon(
            imageVector = Icons.Default.QueueMusic,
            contentDescription = "Add to Queue",
            tint = Color.White
        )
    }
}

/**
 * Queue action for use in dropdown menus
 */
@Composable
fun QueueDropdownItem(
    song: Song,
    onAddToQueue: (Song) -> Unit,
    onDropdownDismiss: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Queue", color = Color.White)
            }
        },
        onClick = {
            onAddToQueue(song)
            onDropdownDismiss()
        }
    )
}