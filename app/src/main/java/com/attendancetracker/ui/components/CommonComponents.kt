package com.attendancetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.attendancetracker.ui.theme.AttendanceTrackerTheme
import com.attendancetracker.ui.theme.ErrorRed
import com.attendancetracker.ui.theme.Present

/**
 * Loading indicator composable.
 *
 * Displays a centered circular progress indicator with optional loading text.
 *
 * @param modifier Optional modifier for customization
 */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.size(16.dp))

            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Error message composable.
 *
 * Displays an error message in a card with a dismiss button.
 * Uses red color scheme to indicate error state.
 *
 * @param message The error message to display
 * @param onDismiss Callback when the dismiss button is clicked
 * @param modifier Optional modifier for customization
 */
@Composable
fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ErrorRed.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Error icon
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = ErrorRed,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Error message text
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorRed,
                modifier = Modifier.weight(1f)
            )

            // Dismiss button
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = ErrorRed
                )
            }
        }
    }
}

/**
 * Success message composable.
 *
 * Displays a success message in a card with a dismiss button.
 * Uses green color scheme to indicate success state.
 *
 * @param message The success message to display
 * @param onDismiss Callback when the dismiss button is clicked
 * @param modifier Optional modifier for customization
 */
@Composable
fun SuccessMessage(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Present.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Success checkmark icon
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Present,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Success message text
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Present,
                modifier = Modifier.weight(1f)
            )

            // Dismiss button
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Present
                )
            }
        }
    }
}

/**
 * Preview for LoadingIndicator.
 */
@Preview(showBackground = true)
@Composable
fun LoadingIndicatorPreview() {
    AttendanceTrackerTheme {
        LoadingIndicator()
    }
}

/**
 * Preview for ErrorMessage.
 */
@Preview(showBackground = true)
@Composable
fun ErrorMessagePreview() {
    AttendanceTrackerTheme {
        ErrorMessage(
            message = "Failed to load attendance data. Please check your connection.",
            onDismiss = {}
        )
    }
}

/**
 * Preview for SuccessMessage.
 */
@Preview(showBackground = true)
@Composable
fun SuccessMessagePreview() {
    AttendanceTrackerTheme {
        SuccessMessage(
            message = "Attendance saved successfully!",
            onDismiss = {}
        )
    }
}
