package com.example.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.purrytify.ui.screens.QueueScreen
import com.example.purrytify.ui.theme.PurrytifyTheme

class QueueScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PurrytifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    QueueScreen(
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}