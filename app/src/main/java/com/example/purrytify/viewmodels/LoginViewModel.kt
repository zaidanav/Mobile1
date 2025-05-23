package com.example.purrytify.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.PurrytifyApp
import com.example.purrytify.repository.AuthRepository
import com.example.purrytify.repository.UserRepository
import com.example.purrytify.util.NetworkUtils
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val application: android.app.Application
) : ViewModel() {

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    private val userRepository by lazy {
        (application as? PurrytifyApp)?.let {
            UserRepository(it.tokenManager)
        }
    }

    fun login(email: String, password: String) {
        _loginResult.value = LoginResult.Loading

        viewModelScope.launch {
            try {
                if (!NetworkUtils.isNetworkAvailable(application)) {
                    _loginResult.value = LoginResult.Error("No internet connection. Check your network and try again.")
                    return@launch
                }

                val result = authRepository.login(email, password)

                result.fold(
                    onSuccess = { token ->
                        fetchUserProfileAndSetUserId(token)
                    },
                    onFailure = { exception ->
                        _loginResult.value = LoginResult.Error(exception.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _loginResult.value = LoginResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun fetchUserProfileAndSetUserId(token: String) {
        try {
            userRepository?.getUserProfile()?.onSuccess { userProfile ->
                val userId = userProfile.id
                Log.d("LoginViewModel", "Setting user ID: $userId")

                // Save user ID in TokenManager and SongRepository
                (application as? PurrytifyApp)?.tokenManager?.saveUserId(userId)
                (application as? PurrytifyApp)?.songRepository?.setCurrentUserId(userId)

                _loginResult.value = LoginResult.Success(token)
            }?.onFailure { error ->
                Log.w("LoginViewModel", "Login successful but failed to get user profile: ${error.message}")
                _loginResult.value = LoginResult.Success(token)
            }
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Error fetching user profile: ${e.message}")
            _loginResult.value = LoginResult.Success(token)
        }
    }

    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }
}

sealed class LoginResult {
    data object Loading : LoginResult()
    data class Success(val token: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}