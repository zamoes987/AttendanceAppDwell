package com.attendancetracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.attendancetracker.data.auth.AuthManager
import com.attendancetracker.data.auth.BiometricHelper
import com.attendancetracker.viewmodel.SettingsViewModel

/**
 * Settings screen for app configuration.
 *
 * Allows users to configure:
 * - Google Sheet ID
 * - Notification preferences
 * - Biometric authentication
 * - Other app settings
 *
 * @param viewModel The SettingsViewModel managing the settings state
 * @param onNavigateBack Callback to navigate back
 * @param onSignOut Callback to sign out the current user
 * @param authManager Authentication manager for biometric settings
 * @param biometricHelper Helper for biometric authentication
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit = {},
    authManager: AuthManager? = null,
    biometricHelper: BiometricHelper? = null
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    // Biometric state
    var biometricEnabled by remember { mutableStateOf(authManager?.isBiometricEnabled() ?: false) }
    val biometricAvailable = remember {
        biometricHelper?.canAuthenticateWithBiometrics() == BiometricHelper.BiometricAvailability.Available
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Google Sheet Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Google Sheet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    OutlinedTextField(
                        value = settings.spreadsheetId,
                        onValueChange = { viewModel.updateSpreadsheetId(it) },
                        label = { Text("Spreadsheet ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.size(8.dp))

                    Text(
                        text = "Find this in your Google Sheet URL",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            // Notification Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    // Morning notification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Thursday Morning Reminder",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Remind at 8:00 AM",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.morningNotificationEnabled,
                            onCheckedChange = { viewModel.toggleMorningNotification(it) }
                        )
                    }

                    Spacer(modifier = Modifier.size(16.dp))

                    // Evening notification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Thursday Evening Reminder",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Remind at 7:00 PM",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.eveningNotificationEnabled,
                            onCheckedChange = { viewModel.toggleEveningNotification(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            // Biometric Authentication Section
            if (authManager != null && biometricHelper != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Security",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.size(16.dp))

                        // Biometric toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Biometric Unlock",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (biometricAvailable) {
                                        "Use fingerprint or face to unlock"
                                    } else {
                                        "Biometric authentication not available"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = biometricEnabled,
                                enabled = biometricAvailable,
                                onCheckedChange = { enabled ->
                                    biometricEnabled = enabled
                                    authManager.setBiometricEnabled(enabled)
                                    android.widget.Toast.makeText(
                                        context,
                                        if (enabled) "Biometric unlock enabled" else "Biometric unlock disabled",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(16.dp))
            }

            // About Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Text(
                        text = "Dwell Community Church",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "Attendance Tracker v1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            // Account Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Button(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Sign Out")
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    Text(
                        text = "Sign out and return to the sign-in screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
