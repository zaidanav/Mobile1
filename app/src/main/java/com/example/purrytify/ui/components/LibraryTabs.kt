package com.example.purrytify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LibraryTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // All Songs Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (selectedTab == "All") Color(0xFF1DB954) else Color(0xFF333333)
                )
                .clickable { onTabSelected("All") }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "All Songs",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = if (selectedTab == "All") FontWeight.Bold else FontWeight.Normal
            )
        }

        // Liked Songs Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (selectedTab == "Liked") Color(0xFF1DB954) else Color(0xFF333333)
                )
                .clickable { onTabSelected("Liked") }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Liked Songs",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = if (selectedTab == "Liked") FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}