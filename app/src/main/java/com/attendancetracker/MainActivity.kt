package com.attendancetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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
class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private var accountName: String? = null
    private var repository: SheetsRepository? = null
    private var viewModel: AttendanceViewModel? = null

    // Activity result launcher for sign-in
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the sign-in result launcher
        signInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task)
            }
        }

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check if already signed in
        val account = GoogleSignIn.getLastSignedInAccount(this)

        if (account != null && hasRequiredScopes(account)) {
            // Already signed in, initialize the app
            accountName = account.email
            initializeApp(account.email!!)
        } else {
            // Not signed in, show sign-in screen
            showSignInScreen()
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
        repository = SheetsRepository(applicationContext, email)
        viewModel = AttendanceViewModel(repository!!)
        val preferencesRepository = PreferencesRepository(applicationContext)

        setContent {
            AttendanceTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Navigation(
                        viewModel = viewModel!!,
                        preferencesRepository = preferencesRepository,
                        onSignOut = { signOut() }
                    )
                }
            }
        }
    }

    /**
     * Signs out the current user and shows the sign-in screen.
     */
    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            accountName = null
            repository = null
            viewModel = null
            showSignInScreen()
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
            if (account != null) {
                accountName = account.email
                initializeApp(account.email!!)
            }
        } catch (e: ApiException) {
            // Sign-in failed
            e.printStackTrace()
            // Stay on sign-in screen
            showSignInScreen()
        }
    }
}
