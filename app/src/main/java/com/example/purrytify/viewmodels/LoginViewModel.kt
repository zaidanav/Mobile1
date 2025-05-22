package com.example.purrytify.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun login(email: String, password: String) {
        _loginResult.value = LoginResult.Loading

        viewModelScope.launch {
            try {
                val result = authRepository.login(email, password)

                result.fold(
                    onSuccess = { token ->
                        _loginResult.value = LoginResult.Success(token)
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

    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }
}

sealed class LoginResult {
    data object Loading : LoginResult()
    data class Success(val token: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}