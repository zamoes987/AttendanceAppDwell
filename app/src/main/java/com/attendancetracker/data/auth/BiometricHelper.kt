package com.attendancetracker.data.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Helper class for biometric authentication (fingerprint, face recognition).
 * 
 * Provides methods to check biometric availability and authenticate users.
 */
class BiometricHelper(private val context: Context) {

    companion object {
        private const val TAG = "BiometricHelper"
    }

    /**
     * Checks if biometric authentication is available on this device.
     */
    fun canAuthenticateWithBiometrics(): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                BiometricAvailability.Available

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                BiometricAvailability.NoHardware

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricAvailability.HardwareUnavailable

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                BiometricAvailability.NoneEnrolled

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricAvailability.SecurityUpdateRequired

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                BiometricAvailability.Unsupported

            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                BiometricAvailability.Unknown

            else -> BiometricAvailability.Unknown
        }
    }

    /**
     * Shows the biometric authentication prompt.
     * 
     * @param activity The FragmentActivity to show the prompt on
     * @param title Title for the prompt
     * @param subtitle Subtitle for the prompt
     * @param description Description text for the prompt
     * @param onSuccess Callback when authentication succeeds
     * @param onError Callback when authentication fails
     * @param onFailure Callback when user cancels or fails too many times
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Verify your identity",
        subtitle: String = "Use your fingerprint or face to continue",
        description: String = "Authenticate to access the Attendance Tracker app",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errorString: String) -> Unit,
        onFailure: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailure()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Enum representing biometric authentication availability.
     */
    enum class BiometricAvailability {
        Available,
        NoHardware,
        HardwareUnavailable,
        NoneEnrolled,
        SecurityUpdateRequired,
        Unsupported,
        Unknown
    }
}
