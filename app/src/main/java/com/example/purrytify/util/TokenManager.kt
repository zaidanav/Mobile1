package com.example.purrytify.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private  const val EXPIRY_TIME = 300000 // 5 minutes in milliseconds
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_JWT_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun isTokenExpiring(): Boolean {
        // Check if the token is expiring in the next 30 seconds
        val expiryTime = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0)
        return System.currentTimeMillis() + 30000 > expiryTime
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        sharedPreferences.edit {
            putString(KEY_JWT_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + EXPIRY_TIME)
        }
    }

    fun deleteTokens() {
        sharedPreferences.edit {
            remove(KEY_JWT_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_TOKEN_EXPIRY)
        }
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null
    }
}