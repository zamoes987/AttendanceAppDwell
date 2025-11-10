package com.attendancetracker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
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
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.delay
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

    // Activity result launcher for sign-in
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Initialize managers
            authManager = AuthManager(applicationContext)
            biometricHelper = BiometricHelper(applicationContext)

            // Register the sign-in result launcher
            signInLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    handleSignInResult(task)
                } else {
                    showSignInScreen()
                }
            }

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

    /**
     * Checks the current authentication state and decides what to show.
     */
    private fun checkAuthenticationState() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        val hasValidSession = authManager.isAuthenticated()
        val hasValidGoogleAccount = authManager.hasValidGoogleAccount()

        when {
            // User has valid session and Google account
            hasValidSession && hasValidGoogleAccount && account != null -> {
                val email = account.email ?: authManager.getUserEmail()
                if (email != null) {
                    // Check if biometric is enabled
                    if (authManager.isBiometricEnabled() && canUseBiometric()) {
                        showBiometricPrompt(email)
                    } else {
                        initializeApp(email)
                    }
                } else {
                    showSignInScreen()
                }
            }
            // Google account exists but session expired
            hasValidGoogleAccount && account != null -> {
                val email = account.email
                if (email != null) {
                    // Restore session
                    authManager.saveAuthState(email)
                    initializeApp(email)
                } else {
                    showSignInScreen()
                }
            }
            // No valid authentication
            else -> {
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
            onError = { errorCode, errorString ->
                Toast.makeText(this, "Authentication error: $errorString", Toast.LENGTH_SHORT).show()
                // Fall back to normal sign-in
                showSignInScreen()
            },
            onFailure = {
                Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
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
                while (true) {
                    delay(30 * 60 * 1000L) // 30 minutes
                    authManager.refreshSession()
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
     * Starts the Google Sign-In flow.
     */
    private fun startSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    /**
     * Handles the result of the sign-in attempt.
     */
    private fun handleSignInResult(task: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null && account.email != null) {
                val email = account.email!!
                initializeApp(email)
                Toast.makeText(this, "Signed in as $email", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Sign-in failed: No account information", Toast.LENGTH_SHORT).show()
                showSignInScreen()
            }
        } catch (e: ApiException) {
            // Sign-in failed
            e.printStackTrace()
            val errorMessage = when (e.statusCode) {
                12501 -> "Sign-in cancelled"
                12500 -> "Sign-in error. Please try again"
                else -> "Sign-in failed: ${e.message}"
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            showSignInScreen()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
            showSignInScreen()
        }
    }
}
