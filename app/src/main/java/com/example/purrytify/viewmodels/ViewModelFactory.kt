package com.example.purrytify.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
                LoginViewModel(authRepository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                val tokenManager = TokenManager(context)
                val authRepository = AuthRepository(tokenManager)
                val userRepository = UserRepository(tokenManager)  // Tambahkan baris ini
                ProfileViewModel(authRepository, userRepository) as T  // Update bagian ini untuk menyertakan userRepository
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}