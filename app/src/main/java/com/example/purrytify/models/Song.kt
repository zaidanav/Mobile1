package com.example.purrytify.models

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val albumArt: Int,
    val isPlaying: Boolean = false,
    val isLiked: Boolean = false
)