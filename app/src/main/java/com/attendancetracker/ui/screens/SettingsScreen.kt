package com.attendancetracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.Button
import com.attendancetracker.ui.components.TutorialDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.attendancetracker.data.auth.AuthManager
import com.attendancetracker.data.auth.BiometricHelper
import com.attendancetracker.data.logging.LogExporter
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

    // Tutorial dialog state
    var showTutorialDialog by remember { mutableStateOf(false) }

    // Biometric state
    var biometricEnabled by remember { mutableStateOf(authManager?.isBiometricEnabled() ?: false) }
    val biometricAvailable = remember {
        biometricHelper?.canAuthenticateWithBiometrics() == BiometricHelper.BiometricAvailability.Available
    }

    // Notification permission state (Android 13+)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // No runtime permission needed on older Android
            }
        )
    }

    // Exact alarm permission state (Android 12+)
    val canScheduleExactAlarms = remember { viewModel.canScheduleExactAlarms() }

    // Permission launcher for POST_NOTIFICATIONS
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            android.widget.Toast.makeText(
                context,
                "Notification permission granted!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            android.widget.Toast.makeText(
                context,
                "Notification permission denied. Reminders won't work.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    // Helper function to request permission if needed
    val requestPermissionIfNeeded = {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    // Request notification permission first
                                    requestPermissionIfNeeded()

                                    // Check exact alarm permission before scheduling
                                    if (!canScheduleExactAlarms) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Please allow exact alarms in the next screen",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        viewModel.requestExactAlarmPermission()
                                        return@Switch
                                    }
                                }
                                viewModel.toggleMorningNotification(enabled)
                            }
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
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    // Request notification permission first
                                    requestPermissionIfNeeded()

                                    // Check exact alarm permission before scheduling
                                    if (!canScheduleExactAlarms) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Please allow exact alarms in the next screen",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        viewModel.requestExactAlarmPermission()
                                        return@Switch
                                    }
                                }
                                viewModel.toggleEveningNotification(enabled)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.size(16.dp))

                    // Warning if exact alarm permission is needed (Android 12+)
                    if (!canScheduleExactAlarms) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permission Required",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Tap to allow exact alarms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { viewModel.requestExactAlarmPermission() }) {
                                Text("Fix")
                            }
                        }
                        Spacer(modifier = Modifier.size(16.dp))
                    }

                    // Test notification button
                    OutlinedButton(
                        onClick = {
                            // Check permissions before sending test notification
                            if (!hasNotificationPermission) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Please grant notification permission first",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                requestPermissionIfNeeded()
                                return@OutlinedButton
                            }

                            if (!canScheduleExactAlarms) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Please allow exact alarms first. Click the 'Fix' button above.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                return@OutlinedButton
                            }

                            viewModel.showTestNotification()
                            android.widget.Toast.makeText(
                                context,
                                "Test notification will appear in 3 seconds",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Test"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Test Notification")
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

            // Tutorial Section
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
                        text = "Tutorial",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.size(8.dp))

                    Text(
                        text = "Review the app tutorial to learn about features and how to use the app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Button(
                        onClick = { showTutorialDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Tutorial")
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            // Debug Logs Section
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
                        text = "Debug Logs",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.size(8.dp))

                    Text(
                        text = "Export logs for troubleshooting. Log size: ${LogExporter.getLogFileSize(context)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { LogExporter.shareLogFile(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Export"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Logs")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = { LogExporter.clearLogs(context) }
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

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

                    // TODO: CUSTOMIZATION - Replace with your organization name
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

    // Tutorial Dialog
    if (showTutorialDialog) {
        TutorialDialog(
            onDismiss = { showTutorialDialog = false }
        )
    }
}
