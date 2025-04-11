package com.example.purrytify.ui.screens

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.QueueScreenActivity
import com.example.purrytify.R
import com.example.purrytify.models.Song
import com.example.purrytify.ui.theme.BACKGROUND_COLOR
import com.example.purrytify.ui.theme.GREEN_COLOR
import com.example.purrytify.viewmodels.MainViewModel
import com.example.purrytify.viewmodels.ViewModelFactory
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.activity.ComponentActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onDismiss: () -> Unit,
    mainViewModel: MainViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = ViewModelFactory.getInstance(LocalContext.current)
    )
) {
    val currentSong by mainViewModel.currentSong.collectAsState()
    val isPlaying by mainViewModel.isPlaying.collectAsState()
    val currentPosition by mainViewModel.currentPosition.collectAsState()
    val duration by mainViewModel.duration.collectAsState()

    // Modified to track if the song is liked
    val isLiked = remember { mutableStateOf(currentSong?.isLiked ?: false) }

    // Keep isLiked in sync with currentSong
    LaunchedEffect(currentSong) {
        currentSong?.let {
            isLiked.value = it.isLiked
        }
    }

    // For repeat mode (bonus feature)
    var repeatMode by remember { mutableStateOf(0) } // 0: Off, 1: Repeat All, 2: Repeat One

    // Get local context for intent
    val context = LocalContext.current

    // Calculate slider position
    val sliderPosition = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    val animatedSliderPosition by animateFloatAsState(targetValue = sliderPosition, label = "")

    if (currentSong == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BACKGROUND_COLOR),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No song is currently playing",
                color = Color.White,
                fontSize = 16.sp
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFF5A1E33) // Background color matching the Figma design
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Bar with back button and options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Queue button
                IconButton(onClick = {
                    val intent = Intent(context, QueueScreenActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_queue_music),
                        contentDescription = "Queue",
                        tint = Color.White
                    )
                }
            }

            // Album Artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Display the album artwork
                val coverImage = if (currentSong?.coverUrl?.startsWith("http") == true) {
                    // URL image
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(currentSong?.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Cover",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (currentSong?.coverUrl?.isNotEmpty() == true && File(currentSong?.coverUrl ?: "").exists()) {
                    // Local file
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(currentSong?.coverUrl ?: ""))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Cover",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback
                    Image(
                        painter = painterResource(id = R.drawable.logo_aplikasi),
                        contentDescription = "Album Cover",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Song title and artist
            currentSong?.let { song ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontFamily = FontFamily(Font(R.font.poppins_bold))
                        )

                        Text(
                            text = song.artist,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.poppins_regular))
                        )
                    }

                    // Like button
                    IconButton(
                        onClick = {
                            isLiked.value = !isLiked.value
                            mainViewModel.toggleLike(song.id, isLiked.value)
                        }
                    ) {
                        Icon(
                            imageVector = if (isLiked.value) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isLiked.value) "Unlike" else "Like",
                            tint = if (isLiked.value) Color.White else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Slider(
                    value = animatedSliderPosition,
                    onValueChange = { value ->
                        val seekPosition = (value * duration).toInt()
                        mainViewModel.seekTo(seekPosition)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = GREEN_COLOR,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Time indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Current position
                    Text(
                        text = formatDuration(currentPosition.toLong()),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    // Total duration
                    Text(
                        text = formatDuration(duration.toLong()),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle button (bonus feature)
                IconButton(onClick = {
                    // Placeholder for shuffle
                }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous button
                IconButton(
                    onClick = {
                        // Call previous function in view model
                        mainViewModel.playPrevious()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Play/Pause button
                IconButton(
                    onClick = {
                        mainViewModel.togglePlayPause()
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GREEN_COLOR)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Next button
                IconButton(
                    onClick = {
                        // Call next function in view model
                        mainViewModel.playNext()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Repeat button (bonus feature)
                IconButton(onClick = {
                    // Cycle through repeat modes: Off -> Repeat All -> Repeat One -> Off
                    repeatMode = (repeatMode + 1) % 3
                    // Implement repeat functionality in MainViewModel
                    mainViewModel.setRepeatMode(repeatMode)
                }) {
                    when (repeatMode) {
                        0 -> Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = "Repeat Off",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        1 -> Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = "Repeat All",
                            tint = GREEN_COLOR,
                            modifier = Modifier.size(24.dp)
                        )
                        2 -> Icon(
                            imageVector = Icons.Filled.RepeatOne,
                            contentDescription = "Repeat One",
                            tint = GREEN_COLOR,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }


        }
    }
}

// Helper function to format duration
private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%d:%02d", minutes, seconds)
}