package com.example.purrytify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purrytify.R
import com.example.purrytify.models.Songs
import com.example.purrytify.ui.components.HorizontalScrollable
import com.example.purrytify.ui.components.RecentlyPlayed
import com.example.purrytify.ui.theme.BACKGROUND_COLOR

@Composable
fun HomeScreen()
{
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND_COLOR)
            .padding(16.dp)
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "New Songs",
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        val songs = listOf(
            Songs("Lagu A", "Artis 1", R.drawable.logo_aplikasi),
            Songs("Lagu B", "Artis 2", R.drawable.logo_aplikasi),
            Songs("Lagu C", "Artis 3", R.drawable.logo_aplikasi),
            Songs("Lagu D", "Artis 4", R.drawable.logo_aplikasi)
        )
        HorizontalScrollable(songs)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Recently Played",
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        RecentlyPlayed(songs)
    }
}