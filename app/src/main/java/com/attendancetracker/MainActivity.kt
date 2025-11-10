package com.attendancetracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.attendancetracker.data.auth.AuthManager
import com.attendancetracker.data.auth.BiometricHelper
import com.attendancetracker.data.repository.PreferencesRepository
import com.attendancetracker.data.repository.SheetsRepository
import com.attendancetracker.ui.Navigation
import com.attendancetracker.ui.screens.SignInScreen
import com.attendancetracker.ui.theme.AttendanceTrackerTheme
import com.attendancetracker.viewmodel.AttendanceViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Main activity for the Attendance Tracker app.
 *
 * Handles Google Sign-In authentication and initializes the app
 * with the appropriate repository and ViewModel once authenticated.
 *
 * SETUP REQUIREMENTS:
 * 1. Google Cloud Project with Sheets API enabled
 * 2. OAuth 2.0 Client ID configured for Android in Google Cloud Console
 * 3. SHA-1 fingerprint added to OAuth credentials
 * 4. User must have access to the Google Sheet specified in GoogleSheetsService
 */
class MainActivity : FragmentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var authManager: AuthManager
    private lateinit var biometricHelper: BiometricHelper
    private var repository: SheetsRepository? = null
    private var viewModel: AttendanceViewModel? = null

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check Google Play Services availability
        if (!isGooglePlayServicesAvailable()) {
            return
        }

        try {
            // Initialize managers
            authManager = AuthManager(applicationContext)
            biometricHelper = BiometricHelper(applicationContext)

            // Configure Google Sign-In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)

            // Check authentication state
            checkAuthenticationState()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
            showSignInScreen()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInResult(task)
            } else {
                Toast.makeText(this, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
                showSignInScreen()
            }
        }
    }

    /**
     * Checks the current authentication state and decides what to show.
     */
    private fun checkAuthenticationState() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        val hasValidSession = authManager.isAuthenticated()
        val hasValidGoogleAccount = authManager.hasValidGoogleAccount()

        if (BuildConfig.DEBUG) {
            android.util.Log.d(
                "MainActivity",
                "Auth state - Session: $hasValidSession, GoogleAccount: $hasValidGoogleAccount"
            )
        }

        when {
            // User has valid session and Google account
            hasValidSession && hasValidGoogleAccount && account != null -> {
                val email = account.email ?: authManager.getUserEmail()
                if (email != null) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("MainActivity", "Valid session found")
                    }
                    // Check if biometric is enabled
                    if (authManager.isBiometricEnabled() && canUseBiometric()) {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.d("MainActivity", "Showing biometric prompt")
                        }
                        showBiometricPrompt(email)
                    } else {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.d("MainActivity", "Initializing app directly")
                        }
                        initializeApp(email)
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("MainActivity", "No email found, showing sign-in screen")
                    }
                    showSignInScreen()
                }
            }
            // Google account exists but session expired
            hasValidGoogleAccount && account != null -> {
                val email = account.email
                if (email != null) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("MainActivity", "Restoring session")
                    }
                    // Restore session
                    authManager.saveAuthState(email)
                    initializeApp(email)
                } else {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("MainActivity", "No email in expired session, showing sign-in screen")
                    }
                    showSignInScreen()
                }
            }
            // No valid authentication
            else -> {
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("MainActivity", "No valid authentication, showing sign-in screen")
                }
                showSignInScreen()
            }
        }
    }

    /**
     * Shows biometric prompt for authentication.
     */
    private fun showBiometricPrompt(email: String) {
        biometricHelper.authenticate(
            activity = this,
            title = "Welcome Back",
            subtitle = "Unlock Attendance Tracker",
            description = "Use your fingerprint or face to unlock",
            onSuccess = {
                authManager.refreshSession()
                initializeApp(email)
            },
            onError = { _, errorString ->
                Toast.makeText(this, "Biometric error: $errorString", Toast.LENGTH_SHORT).show()
                // User has valid Google account, just skip biometric
                authManager.refreshSession()
                initializeApp(email)
            },
            onFailure = {
                Toast.makeText(this, "Biometric failed, please try again", Toast.LENGTH_SHORT).show()
                // User has valid Google account, just skip biometric
                authManager.refreshSession()
                initializeApp(email)
            }
        )
    }

    /**
     * Checks if biometric authentication can be used.
     */
    private fun canUseBiometric(): Boolean {
        return when (biometricHelper.canAuthenticateWithBiometrics()) {
            BiometricHelper.BiometricAvailability.Available -> true
            else -> false
        }
    }

    /**
     * Checks if Google Play Services is available and up-to-date.
     */
    private fun isGooglePlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
            } else {
                Toast.makeText(
                    this,
                    "This device doesn't support Google Play Services",
                    Toast.LENGTH_LONG
                ).show()
            }
            return false
        }
        return true
    }

    /**
     * Checks if the account has the required Google Sheets scope.
     */
    private fun hasRequiredScopes(account: GoogleSignInAccount): Boolean {
        val requiredScope = Scope(SheetsScopes.SPREADSHEETS)
        return account.grantedScopes.contains(requiredScope)
    }

    /**
     * Shows the sign-in screen.
     */
    private fun showSignInScreen() {
        setContent {
            AttendanceTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SignInScreen(
                        onSignIn = { startSignIn() }
                    )
                }
            }
        }
    }

    /**
     * Initializes the app with repository and ViewModel.
     */
    private fun initializeApp(email: String) {
        try {
            // Save auth state
            authManager.saveAuthState(email)

            // Initialize repository and ViewModel
            repository = SheetsRepository(applicationContext, email)
            val repo = repository ?: throw IllegalStateException("Failed to create repository")
            viewModel = AttendanceViewModel(repo)
            val vm = viewModel ?: throw IllegalStateException("Failed to create ViewModel")
            val preferencesRepository = PreferencesRepository(applicationContext)

            setContent {
                AttendanceTrackerTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Navigation(
                            viewModel = vm,
                            preferencesRepository = preferencesRepository,
                            onSignOut = { signOut() },
                            authManager = authManager,
                            biometricHelper = biometricHelper
                        )
                    }
                }
            }

            // Refresh session in background every 30 minutes
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (isActive && authManager.isAuthenticated()) {
                        try {
                            delay(30 * 60 * 1000L) // 30 minutes
                            authManager.refreshSession()
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Session refresh failed", e)
                            break // Exit loop on error
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
            showSignInScreen()
        }
    }

    /**
     * Signs out the current user and shows the sign-in screen.
     */
    private fun signOut() {
        try {
            // Clear auth state
            authManager.clearAuthState()

            // Sign out from Google
            googleSignInClient.signOut().addOnCompleteListener(this) {
                repository = null
                viewModel = null
                showSignInScreen()
                Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error signing out: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Starts the Google Sign-In flow using startActivityForResult.
     */
    @Suppress("DEPRECATION")
    private fun startSignIn() {
        if (!isGooglePlayServicesAvailable()) {
            return
        }

        try {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
            if (BuildConfig.DEBUG) {
                android.util.Log.d("MainActivity", "Sign-in intent launched with startActivityForResult")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("MainActivity", "Error starting sign-in", e)
            Toast.makeText(this, "Error starting sign-in: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Handles the result of the sign-in attempt.
     */
    private fun handleSignInResult(task: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        try {
            if (BuildConfig.DEBUG) {
                android.util.Log.d("MainActivity", "Handling sign-in result")
            }
            val account = task.getResult(ApiException::class.java)
            account?.email?.let { email ->
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("MainActivity", "Sign-in successful")
                }

                // Check if we have the required scope
                if (!hasRequiredScopes(account)) {
                    android.util.Log.e("MainActivity", "Missing required Google Sheets scope")
                    Toast.makeText(
                        this,
                        "Missing Google Sheets permission. Please grant access.",
                        Toast.LENGTH_LONG
                    ).show()
                    // Try signing in again to request scope
                    googleSignInClient.signOut()
                    showSignInScreen()
                    return
                }

                initializeApp(email)
                Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show()
            } ?: run {
                android.util.Log.e("MainActivity", "Sign-in failed: No account information")
                Toast.makeText(this, "Sign-in failed: No account information", Toast.LENGTH_SHORT).show()
                showSignInScreen()
            }
        } catch (e: ApiException) {
            // Sign-in failed
            e.printStackTrace()
            android.util.Log.e("MainActivity", "Sign-in ApiException: ${e.statusCode} - ${e.message}")
            val errorMessage = when (e.statusCode) {
                12501 -> "Sign-in cancelled"
                12500 -> "Sign-in error. Please try again"
                10 -> "Developer error - Check OAuth configuration"
                7 -> "Network error - Check internet connection"
                else -> "Sign-in failed (Code ${e.statusCode}): ${e.message}"
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            showSignInScreen()
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("MainActivity", "Sign-in unexpected error: ${e.message}", e)
            Toast.makeText(this, "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
            showSignInScreen()
        }
    }
}
