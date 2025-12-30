package com.attendancetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Setup screen shown to new users who need to configure their Google Spreadsheet.
 *
 * This screen guides users through:
 * 1. Understanding what they need (a Google Sheet with the correct structure)
 * 2. Entering their Spreadsheet ID
 * 3. Completing setup to access the main app
 *
 * @param onSetupComplete Callback with the entered spreadsheet ID when setup is complete
 * @param onSignOut Callback to sign out and return to sign-in screen
 */
@Composable
fun SetupScreen(
    onSetupComplete: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var spreadsheetId by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Welcome icon
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Title
        Text(
            text = "Welcome to Attendance Tracker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Subtitle
        Text(
            text = "Let's set up your group's attendance sheet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Instructions card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Before you begin:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                SetupStep(
                    number = "1",
                    text = "Create a Google Sheet with your member list"
                )

                SetupStep(
                    number = "2",
                    text = "Structure it with: Column B = Names, Column C = Category (OM, XT, RN, FT, V)"
                )

                SetupStep(
                    number = "3",
                    text = "Make sure your Google account has edit access to the sheet"
                )

                SetupStep(
                    number = "4",
                    text = "Copy the Spreadsheet ID from the URL"
                )
            }
        }

        // Info about spreadsheet ID
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Where to find the Spreadsheet ID",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Open your Google Sheet and look at the URL:\nhttps://docs.google.com/spreadsheets/d/YOUR_ID_HERE/edit\n\nThe Spreadsheet ID is the long string of letters and numbers between /d/ and /edit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Spreadsheet ID input
        OutlinedTextField(
            value = spreadsheetId,
            onValueChange = {
                spreadsheetId = it
                showError = false
            },
            label = { Text("Spreadsheet ID") },
            placeholder = { Text("e.g., 1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = showError,
            supportingText = if (showError) {
                { Text("Please enter a valid Spreadsheet ID") }
            } else null,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (isValidSpreadsheetId(spreadsheetId)) {
                        onSetupComplete(spreadsheetId.trim())
                    } else {
                        showError = true
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Continue button
        Button(
            onClick = {
                if (isValidSpreadsheetId(spreadsheetId)) {
                    onSetupComplete(spreadsheetId.trim())
                } else {
                    showError = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = spreadsheetId.isNotBlank()
        ) {
            Text("Continue")
        }

        // Sign out option
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * A numbered setup step item.
 */
@Composable
private fun SetupStep(
    number: String,
    text: String
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Validates that the spreadsheet ID has a reasonable format.
 * Google Spreadsheet IDs are typically 44 characters, containing letters, numbers, underscores, and hyphens.
 */
private fun isValidSpreadsheetId(id: String): Boolean {
    val trimmed = id.trim()
    // Basic validation: should be non-empty, reasonable length, and contain only valid characters
    if (trimmed.isEmpty()) return false
    if (trimmed.length < 20 || trimmed.length > 60) return false
    // Only allow alphanumeric, underscore, and hyphen
    return trimmed.all { it.isLetterOrDigit() || it == '_' || it == '-' }
}
