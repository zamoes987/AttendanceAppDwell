package com.attendancetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.attendancetracker.data.models.AttendanceSummary
import com.attendancetracker.data.models.Category
import com.attendancetracker.ui.theme.CategoryFT
import com.attendancetracker.ui.theme.CategoryOM
import com.attendancetracker.ui.theme.CategoryRN
import com.attendancetracker.ui.theme.CategoryV
import com.attendancetracker.ui.theme.CategoryXT
import com.attendancetracker.ui.theme.Present
import com.attendancetracker.viewmodel.AttendanceViewModel
import java.time.format.DateTimeFormatter

/**
 * Screen displaying historical attendance records.
 *
 * Shows a list of past meetings with attendance summaries including
 * total attendance, category breakdowns, and trends compared to previous meetings.
 *
 * @param viewModel The AttendanceViewModel managing the screen state
 * @param onNavigateBack Callback to navigate back to the home screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit
) {
    // Collect state from ViewModel
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    val skippedDates by viewModel.skippedDates.collectAsState()

    // Get summaries from repository (accessing repository through viewModel is not ideal
    // but works for this implementation)
    val summaries = remember(attendanceRecords) {
        // Remove duplicate dates by grouping and taking the first of each date
        attendanceRecords
            .groupBy { it.date }
            .mapNotNull { (_, records) -> records.firstOrNull() }
            .sortedByDescending { it.date }
            .map { record ->
                AttendanceSummary(
                    weekOf = record.date,
                    totalPresent = record.getTotalAttendance(),
                    categoryBreakdown = record.categoryTotals.toMap(),
                    comparisonToPrevious = null // Comparison calculation would go here
                )
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Attendance History")
                },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (summaries.isEmpty()) {
                // Empty state
                EmptyHistoryState()
            } else {
                // History list
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = summaries,
                        key = { summary -> summary.weekOf.toString() }
                    ) { summary ->
                        val isSkipped = skippedDates.contains(summary.weekOf.toString())
                        AttendanceSummaryCard(
                            summary = summary,
                            isSkipped = isSkipped,
                            onToggleSkipped = {
                                viewModel.toggleDateSkipped(summary.weekOf)
                            }
                        )
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Card displaying a summary of a single meeting's attendance.
 *
 * Shows the date, total attendance, category breakdown chips,
 * and trend indicator if applicable.
 *
 * @param summary The attendance summary to display
 * @param isSkipped Whether this date is marked as "No Meeting"
 * @param onToggleSkipped Callback to toggle the skipped status
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceSummaryCard(
    summary: AttendanceSummary,
    isSkipped: Boolean = false,
    onToggleSkipped: () -> Unit = {}
) {
    // Format date for display
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
    val formattedDate = summary.weekOf.format(dateFormatter)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSkipped) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Date header with skip/unskip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isSkipped) {
                        Text(
                            text = "⊘ No Meeting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Skip/Unskip button
                IconButton(onClick = onToggleSkipped) {
                    Icon(
                        imageVector = if (isSkipped) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = if (isSkipped) "Mark as Meeting" else "Mark as No Meeting",
                        tint = if (isSkipped) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            // Total attendance with icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "Total Attendance",
                    tint = Present,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Total: ${summary.totalPresent}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Trend indicator if available
                summary.comparisonToPrevious?.let { comparison ->
                    Spacer(modifier = Modifier.width(8.dp))

                    if (comparison > 0) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Increased",
                            tint = Present,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "+$comparison",
                            style = MaterialTheme.typography.bodySmall,
                            color = Present
                        )
                    } else if (comparison < 0) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = "Decreased",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$comparison",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            // Category breakdown chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                summary.categoryBreakdown.forEach { (category, count) ->
                    if (count > 0) {
                        CategoryChip(category, count)
                    }
                }
            }
        }
    }
}

/**
 * Chip displaying attendance count for a specific category.
 *
 * Uses category-specific colors for visual distinction.
 *
 * @param category The member category
 * @param count The number of attendees in this category
 */
@Composable
fun CategoryChip(category: Category, count: Int) {
    val categoryColor = when (category) {
        Category.ORIGINAL_MEMBER -> CategoryOM
        Category.XENOS_TRANSFER -> CategoryXT
        Category.RETURNING_NEW -> CategoryRN
        Category.FIRST_TIMER -> CategoryFT
        Category.VISITOR -> CategoryV
    }

    SuggestionChip(
        onClick = { /* No action on click */ },
        label = {
            Text("${category.abbreviation}: $count")
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = categoryColor.copy(alpha = 0.2f),
            labelColor = categoryColor
        )
    )
}

/**
 * Empty state displayed when there are no attendance records.
 */
@Composable
fun EmptyHistoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = "No History",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.size(16.dp))

            Text(
                text = "No attendance records yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "Start marking attendance to see history here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
