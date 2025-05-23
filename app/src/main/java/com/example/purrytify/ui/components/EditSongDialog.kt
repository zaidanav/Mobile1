package com.example.purrytify.ui.components

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.purrytify.R
import com.example.purrytify.models.Song
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun EditSongDialog(
    song: Song,
    onDismiss: () -> Unit,
    onSaveClick: (title: String, artist: String, filePath: String, artworkPath: String, duration: Long) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var duration by remember { mutableLongStateOf(song.duration) }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isAudioSelected by remember { mutableStateOf(false) }
    var isImageSelected by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val filePath by remember { mutableStateOf(song.filePath) }
    val artworkPath by remember { mutableStateOf(song.coverUrl) }

    // Format duration to display
    val formattedDuration = remember(duration) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes)
        String.format("%d:%02d", minutes, seconds)
    }

    // Activity launcher for audio file
    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedAudioUri = it
            isAudioSelected = true

            // Extract metadata from audio file
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, it)

                // Get audio title if available
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { extractedTitle ->
                    if (extractedTitle.isNotEmpty()) {
                        title = extractedTitle
                    }
                }

                // Get artist if available
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { extractedArtist ->
                    if (extractedArtist.isNotEmpty()) {
                        artist = extractedArtist
                    }
                }

                // Get duration
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let { durationStr ->
                    duration = durationStr.toLongOrNull() ?: 0
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error reading audio file: ${e.message}"
            } finally {
                retriever.release()
            }
        }
    }

    // Activity launcher for image file
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            isImageSelected = true
        }
    }

    // Function to copy URI content to internal storage
    fun copyUriToInternalStorage(uri: Uri, fileName: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, fileName)

            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Error copying file: ${e.message}"
            null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121212)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Edit Song",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Display error message if any
                errorMessage?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Upload fields container
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Upload Photo Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .padding(end = 8.dp)
                            .border(
                                border = BorderStroke(1.dp, Color.Gray),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E1E))
                            .clickable { imageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Change Photo",
                                color = Color.White,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isImageSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Color.Green,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_image_placeholder),
                                    contentDescription = "Upload Photo",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title field
                Text(
                    text = "Title",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Title", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Artist field
                Text(
                    text = "Artist",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Artist", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Display duration
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Duration",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formattedDuration,
                    color = Color.White,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cancel button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF333333)
                        ),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White
                        )
                    }

                    // Save button
                    Button(
                        onClick = {
                            var newFilePath = filePath
                            var newArtworkPath = artworkPath

                            // Handle audio file if selected
                            if (isAudioSelected && selectedAudioUri != null) {
                                val audioFileName = "song_edit_${System.currentTimeMillis()}.mp3"
                                val savedPath = copyUriToInternalStorage(selectedAudioUri!!, audioFileName)
                                if (savedPath != null) {
                                    newFilePath = savedPath
                                }
                            }

                            // Handle artwork if selected
                            if (isImageSelected && selectedImageUri != null) {
                                val artworkFileName = "artwork_edit_${System.currentTimeMillis()}.jpg"
                                val savedPath = copyUriToInternalStorage(selectedImageUri!!, artworkFileName)
                                if (savedPath != null) {
                                    newArtworkPath = savedPath
                                }
                            }

                            onSaveClick(title, artist, newFilePath, newArtworkPath, duration)
                            onDismiss()
                        },
                        enabled = title.isNotBlank() && artist.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1DB954)
                        ),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            text = "Save",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}