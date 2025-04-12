package com.example.purrytify.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.style.TextAlign
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.purrytify.R
import com.example.purrytify.models.Song
import com.example.purrytify.ui.theme.BACKGROUND_COLOR
import java.io.File

@Composable
fun NewSongItem(
    song: Song,
    onClick: () -> Unit,
    onAddToQueue: (() -> Unit)? = null
) {
    var showOptions by remember { mutableStateOf(false) }
    var showAddToQueueDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Cover Art with controls overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(BACKGROUND_COLOR)
                .clickable { onClick() }
        ) {
            val context = LocalContext.current
            val painter = if (song.coverUrl.startsWith("http")) {
                // URL image
                rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(song.coverUrl)
                        .crossfade(true)
                        .build()
                )
            } else if (File(song.coverUrl).exists()) {
                // Local file
                rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(File(song.coverUrl))
                        .crossfade(true)
                        .build()
                )
            } else {
                // Fallback
                painterResource(id = R.drawable.logo_aplikasi)
            }

            Image(
                painter = painter,
                contentDescription = "Album Cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Playing Indicator
            if (song.isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Now Playing",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Queue button overlay (only shown when onAddToQueue is provided)
            if (onAddToQueue != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = { showOptions = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Dropdown menu
                    DropdownMenu(
                        expanded = showOptions,
                        onDismissRequest = { showOptions = false },
                        modifier = Modifier.background(Color(0xFF333333))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Add to Queue",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            onClick = {
                                if (showAddToQueueDialog) {
                                    showAddToQueueDialog = true
                                    showOptions = false
                                } else {
                                    onAddToQueue()
                                    showOptions = false
                                }
                            }
                        )
                    }
                }
            }
        }

        // Song Info
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = song.title,
                color = if (song.isPlaying) Color.Green else Color.White,
                fontFamily = FontFamily(Font(R.font.poppins_medium)),
                fontSize = 14.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                color = Color(0xFFB3B3B3),
                textAlign = TextAlign.Center,
                fontFamily = FontFamily(Font(R.font.poppins_regular)),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(15.dp))
        }
    }

    // Show Add to Queue dialog if requested
    if (showAddToQueueDialog && onAddToQueue != null) {
        AddToQueueDialog(
            song = song,
            onDismiss = { showAddToQueueDialog = false },
            onAddToQueue = {
                onAddToQueue()
                showAddToQueueDialog = false
            }
        )
    }
}