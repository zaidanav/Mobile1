package com.example.purrytify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.purrytify.models.Song

@Composable
fun RecentlyPlayed(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            items = songs,
            key = { it.id }
        ) { song ->
            SongItem(
                song = song,
                onSongClick = { onSongClick(it) }
            )
        }
    }
}