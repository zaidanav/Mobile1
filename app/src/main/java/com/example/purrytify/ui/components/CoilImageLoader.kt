package com.example.purrytify.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.purrytify.R

@Composable
fun SongCoverImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val painter = if (imageUrl.isNotEmpty()) {
        rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build()
        )
    } else {
        painterResource(id = R.drawable.ic_image_placeholder)
    }

    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier
            .size(50.dp)
            .clip(RoundedCornerShape(4.dp)),
        contentScale = ContentScale.Crop
    )
}