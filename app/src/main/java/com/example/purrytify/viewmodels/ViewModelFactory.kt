package com.example.purrytify.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.PurrytifyApp
import com.example.purrytify.repository.AuthRepository
import com.example.purrytify.repository.UserRepository
import com.example.purrytify.util.TokenManager

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                val tokenManager = TokenManager(context)
                val authRepository = AuthRepository(tokenManager)
                LoginViewModel(authRepository, context.applicationContext as android.app.Application) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                val tokenManager = TokenManager(context)
                val authRepository = AuthRepository(tokenManager)
                val userRepository = UserRepository(tokenManager)  // Tambahkan baris ini
                ProfileViewModel(authRepository, userRepository) as T  // Update bagian ini untuk menyertakan userRepository
            }
            modelClass.isAssignableFrom(SongViewModel::class.java) -> {
                val app = context.applicationContext as PurrytifyApp
                SongViewModel(app.songRepository) as T
            }
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> {
                val app = context.applicationContext as PurrytifyApp
                LibraryViewModel(app.songRepository) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                val app = context.applicationContext as PurrytifyApp
                HomeViewModel(app.songRepository) as T
            }
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                // MainViewModel doesn't need any special dependencies
                MainViewModel() as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        // Singleton instance
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: ViewModelFactory? = null

        fun getInstance(context: Context): ViewModelFactory {
            return INSTANCE ?: synchronized(this) {
                val instance = ViewModelFactory(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}