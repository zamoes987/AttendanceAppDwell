package com.attendancetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.attendancetracker.data.models.AttendanceRecord
import com.attendancetracker.data.models.Category
import com.attendancetracker.data.models.Member
import com.attendancetracker.ui.theme.CategoryFT
import com.attendancetracker.ui.theme.CategoryOM
import com.attendancetracker.ui.theme.CategoryRN
import com.attendancetracker.ui.theme.CategoryV
import com.attendancetracker.ui.theme.CategoryXT
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Dialog displaying a member's attendance history.
 *
 * Shows the member's name, category, attendance statistics,
 * and a list of all dates they attended (most recent first).
 *
 * @param member The member whose history to display
 * @param totalMeetings Total number of meetings tracked (for accurate percentage calculation)
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun MemberAttendanceHistoryDialog(
    member: Member,
    totalMeetings: Int,
    onDismiss: () -> Unit
) {
    // Get category color for theming
    val categoryColor = when (member.category) {
        Category.ORIGINAL_MEMBER -> CategoryOM
        Category.XENOS_TRANSFER -> CategoryXT
        Category.RETURNING_NEW -> CategoryRN
        Category.FIRST_TIMER -> CategoryFT
        Category.VISITOR -> CategoryV
    }

    // Parse and sort dates attended (most recent first)
    val datesAttended = member.attendanceHistory
        .filter { it.value } // Only dates where present
        .keys
        .mapNotNull { dateString ->
            AttendanceRecord.parseDateFromSheet(dateString)?.let { date ->
                dateString to date
            }
        }
        .sortedByDescending { it.second }
        .map { it.first }

    // Calculate stats using the passed totalMeetings for consistency with Statistics page
    val attendedCount = member.getTotalAttendance()
    val attendancePercentage = if (totalMeetings > 0) {
        (attendedCount.toDouble() / totalMeetings) * 100.0
    } else {
        0.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = member.category.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = categoryColor
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Stats card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = categoryColor.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Meetings Attended",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$attendedCount of $totalMeetings",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Attendance Rate",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${attendancePercentage.toInt()}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        attendancePercentage >= 80 -> MaterialTheme.colorScheme.primary
                                        attendancePercentage >= 50 -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dates header
                Text(
                    text = "Dates Attended",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dates list
                if (datesAttended.isEmpty()) {
                    Text(
                        text = "No attendance recorded yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(datesAttended) { dateString ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = categoryColor
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = formatDateForDisplay(dateString),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Formats a date string from sheet format (M/d/yy) to a more readable format.
 */
private fun formatDateForDisplay(dateString: String): String {
    return try {
        val date = AttendanceRecord.parseDateFromSheet(dateString)
        date?.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")) ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
