package com.example.purrytify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.purrytify.models.Song
import com.example.purrytify.ui.theme.BACKGROUND_COLOR

@Composable
fun NewSongs(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = songs,
            key = { it.id }
        ) { song ->
            Card(
                modifier = Modifier
                    .width(160.dp)
                    .padding(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BACKGROUND_COLOR
                )
            ) {
                NewSongItem(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}