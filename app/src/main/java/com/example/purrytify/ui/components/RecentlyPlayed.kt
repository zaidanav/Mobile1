package com.example.purrytify.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purrytify.models.Songs
import com.example.purrytify.R
import com.example.purrytify.ui.theme.BACKGROUND_COLOR

@Composable
fun RecentlyPlayed(songs: List<Songs>) {
    for(song in songs){
        Row{
            Image(
                painter = painterResource(id = R.drawable.logo_aplikasi),
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp, 80.dp).background(Color.Gray)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column (modifier = Modifier.align(alignment = Alignment.CenterVertically))
            {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontFamily = FontFamily(Font(R.font.poppins_semi_bold)),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                Text(
                    text = song.artist,
                    color = Color(0xFFB3B3B3),
                    fontFamily = FontFamily(Font(R.font.poppins_regular)),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

            }
        }
    }
}