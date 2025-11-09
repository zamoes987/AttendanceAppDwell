package com.attendancetracker.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.services.sheets.v4.SheetsScopes

/**
 * Manages authentication state and provides persistent login functionality.
 * 
 * Features:
 * - Secure credential storage using EncryptedSharedPreferences
 * - Biometric authentication support
 * - Session management
 * - Token refresh capability
 */
class AuthManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create encrypted preferences, falling back to standard", e)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "AuthManager"
        private const val PREFS_NAME = "attendance_tracker_auth"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LAST_AUTH_TIME = "last_auth_time"
        private const val SESSION_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Checks if user is currently authenticated with a valid session.
     */
    fun isAuthenticated(): Boolean {
        val isAuth = sharedPreferences.getBoolean(KEY_IS_AUTHENTICATED, false)
        val lastAuthTime = sharedPreferences.getLong(KEY_LAST_AUTH_TIME, 0)
        val currentTime = System.currentTimeMillis()

        // Check if session is still valid (within 24 hours)
        val sessionValid = (currentTime - lastAuthTime) < SESSION_DURATION_MS

        return isAuth && sessionValid
    }

    /**
     * Checks if Google Sign-In account is still valid.
     */
    fun hasValidGoogleAccount(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) return false

        val requiredScope = Scope(SheetsScopes.SPREADSHEETS)
        return account.grantedScopes.contains(requiredScope)
    }

    /**
     * Saves authentication state after successful sign-in.
     */
    fun saveAuthState(email: String) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_EMAIL, email)
            putBoolean(KEY_IS_AUTHENTICATED, true)
            putLong(KEY_LAST_AUTH_TIME, System.currentTimeMillis())
            apply()
        }
        Log.d(TAG, "Auth state saved for: $email")
    }

    /**
     * Clears authentication state on sign-out.
     */
    fun clearAuthState() {
        sharedPreferences.edit().apply {
            remove(KEY_USER_EMAIL)
            putBoolean(KEY_IS_AUTHENTICATED, false)
            remove(KEY_LAST_AUTH_TIME)
            apply()
        }
        Log.d(TAG, "Auth state cleared")
    }

    /**
     * Gets the stored user email.
     */
    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Checks if biometric authentication is enabled.
     */
    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    /**
     * Enables or disables biometric authentication.
     */
    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        Log.d(TAG, "Biometric authentication ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Refreshes the session timestamp to extend the session.
     */
    fun refreshSession() {
        if (isAuthenticated()) {
            sharedPreferences.edit()
                .putLong(KEY_LAST_AUTH_TIME, System.currentTimeMillis())
                .apply()
            Log.d(TAG, "Session refreshed")
        }
    }

    /**
     * Gets the time remaining in the current session in milliseconds.
     */
    fun getSessionTimeRemaining(): Long {
        val lastAuthTime = sharedPreferences.getLong(KEY_LAST_AUTH_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastAuthTime
        return (SESSION_DURATION_MS - elapsed).coerceAtLeast(0)
    }
}
