package com.example.purrytify.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.models.UserProfile
import com.example.purrytify.repository.AuthRepository
import com.example.purrytify.repository.UserRepository
import kotlinx.coroutines.launch

// Tambahkan UserRepository sebagai parameter tambahan
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // LiveData untuk data profil
    private val _profileData = MutableLiveData<UserProfile>()
    val profileData: LiveData<UserProfile> = _profileData

    // LiveData untuk status loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData untuk pesan error
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Fungsi untuk memuat profil pengguna
    fun loadUserProfile() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = userRepository.getUserProfile()
                result.onSuccess {
                    _profileData.value = it
                }.onFailure {
                    _error.value = it.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Fungsi logout yang sudah ada
    fun logout() {
        authRepository.logout()
    }
}