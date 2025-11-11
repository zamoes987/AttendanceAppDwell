@file:OptIn(ExperimentalMaterial3Api::class)

package com.attendancetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.attendancetracker.data.models.CategoryStatistics
import com.attendancetracker.data.models.MemberStatistics
import com.attendancetracker.data.models.OverallStatistics
import com.attendancetracker.data.models.TrendAnalysis
import com.attendancetracker.data.models.TrendDirection
import com.attendancetracker.data.repository.MemberStatisticsSortBy
import com.attendancetracker.ui.components.LoadingIndicator
import com.attendancetracker.ui.theme.CategoryFT
import com.attendancetracker.ui.theme.CategoryOM
import com.attendancetracker.ui.theme.CategoryRN
import com.attendancetracker.ui.theme.CategoryV
import com.attendancetracker.ui.theme.CategoryXT
import com.attendancetracker.ui.theme.ErrorRed
import com.attendancetracker.ui.theme.Present
import com.attendancetracker.viewmodel.AttendanceViewModel
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Statistics Dashboard Screen.
 *
 * Displays comprehensive attendance statistics including overall metrics,
 * trend analysis, category comparisons, and individual member statistics.
 *
 * @param viewModel The AttendanceViewModel managing the screen state
 * @param onNavigateBack Callback to navigate back to the previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit
) {
    // Collect state from ViewModel
    val overallStats by viewModel.overallStatistics.collectAsState()
    val memberStats by viewModel.memberStatistics.collectAsState()
    val categoryStats by viewModel.categoryStatistics.collectAsState()
    val trendAnalysis by viewModel.trendAnalysis.collectAsState()
    val isLoading by viewModel.statisticsLoading.collectAsState()
    val currentSort by viewModel.memberStatisticsSortBy.collectAsState()

    // Calculate statistics on screen launch
    LaunchedEffect(Unit) {
        viewModel.calculateStatistics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Statistics")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(onClick = { viewModel.refreshStatistics() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Statistics"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    LoadingIndicator()
                }
                overallStats == null || memberStats.isEmpty() -> {
                    EmptyStatisticsState()
                }
                else -> {
                    // Show statistics content
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Overall Statistics Card
                        item {
                            OverallStatisticsCard(overallStats!!)
                        }

                        // Trend Card
                        item {
                            trendAnalysis?.let { trend ->
                                TrendCard(trend)
                            }
                        }

                        // Category Comparison Card
                        item {
                            CategoryComparisonCard(categoryStats)
                        }

                        // Member Statistics Header with Sorting
                        item {
                            MemberStatisticsHeader(
                                currentSort = currentSort,
                                onSortChange = { viewModel.setMemberStatisticsSort(it) }
                            )
                        }

                        // Member Statistics Items
                        items(
                            items = memberStats,
                            key = { it.memberId }
                        ) { memberStat ->
                            MemberStatisticsItem(memberStat)
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
}

/**
 * Card displaying overall statistics.
 *
 * Shows total meetings, average attendance, and active members.
 *
 * @param stats The overall statistics to display
 */
@Composable
fun OverallStatisticsCard(stats: OverallStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "Overall Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Three statistics in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    icon = Icons.Default.CalendarToday,
                    value = stats.totalMeetings.toString(),
                    label = "Total Meetings",
                    modifier = Modifier.weight(1f)
                )

                StatisticItem(
                    icon = Icons.Default.TrendingUp,
                    value = "${stats.averageAttendance.roundToInt()}",
                    label = "Avg Attendance",
                    modifier = Modifier.weight(1f)
                )

                StatisticItem(
                    icon = Icons.Default.People,
                    value = stats.totalMembers.toString(),
                    label = "Active Members",
                    modifier = Modifier.weight(1f)
                )
            }

            // Date range if available
            stats.mostRecentMeetingDate?.let { mostRecent ->
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Most Recent: ${mostRecent.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Individual statistic item with icon, value, and label.
 *
 * @param icon The icon to display
 * @param value The statistic value
 * @param label The statistic label
 * @param modifier Optional modifier
 */
@Composable
fun StatisticItem(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Card displaying trend analysis with direction indicator.
 *
 * @param trend The trend analysis data
 */
@Composable
fun TrendCard(trend: TrendAnalysis) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title with trend direction icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Attendance Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Trend direction indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (icon, color, text) = when (trend.direction) {
                        TrendDirection.IMPROVING -> Triple(
                            Icons.Default.TrendingUp,
                            Present,
                            "Improving"
                        )
                        TrendDirection.DECLINING -> Triple(
                            Icons.Default.TrendingDown,
                            ErrorRed,
                            "Declining"
                        )
                        TrendDirection.STABLE -> Triple(
                            Icons.Default.TrendingFlat,
                            MaterialTheme.colorScheme.tertiary,
                            "Stable"
                        )
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Change percentage
            Text(
                text = "Change: ${if (trend.changePercentage >= 0) "+" else ""}${trend.changePercentage.roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Simple bar chart visualization
            TrendChart(trend.trendPoints)
        }
    }
}

/**
 * Simple bar chart visualization for trend data.
 *
 * @param trendPoints List of attendance trend data points
 */
@Composable
fun TrendChart(trendPoints: List<com.attendancetracker.data.models.AttendanceTrend>) {
    if (trendPoints.isEmpty()) return

    val maxAttendance = trendPoints.maxOfOrNull { it.attendanceCount } ?: 1

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        trendPoints.takeLast(10).forEach { point ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date label
                Text(
                    text = point.date.format(DateTimeFormatter.ofPattern("M/d")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Progress bar representing attendance
                LinearProgressIndicator(
                    progress = point.attendanceCount.toFloat() / maxAttendance.toFloat(),
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Attendance count
                Text(
                    text = point.attendanceCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(30.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * Card displaying category-level statistics comparison.
 *
 * @param categoryStats List of category statistics
 */
@Composable
fun CategoryComparisonCard(categoryStats: List<CategoryStatistics>) {
    if (categoryStats.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "Category Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category rows
            categoryStats.forEach { stat ->
                CategoryStatisticsRow(stat)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Single category statistics row with progress indicator.
 *
 * @param stat The category statistics to display
 */
@Composable
fun CategoryStatisticsRow(stat: CategoryStatistics) {
    val categoryColor = when (stat.category) {
        com.attendancetracker.data.models.Category.ORIGINAL_MEMBER -> CategoryOM
        com.attendancetracker.data.models.Category.XENOS_TRANSFER -> CategoryXT
        com.attendancetracker.data.models.Category.RETURNING_NEW -> CategoryRN
        com.attendancetracker.data.models.Category.FIRST_TIMER -> CategoryFT
        com.attendancetracker.data.models.Category.VISITOR -> CategoryV
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Category name and percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stat.category.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "${stat.averageAttendancePercentage.roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = categoryColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = (stat.averageAttendancePercentage / 100.0).toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = categoryColor,
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Member count
        Text(
            text = "${stat.memberCount} members",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Section header for member statistics with sorting options.
 *
 * @param currentSort The currently selected sort option
 * @param onSortChange Callback when sort option changes
 */
@Composable
fun MemberStatisticsHeader(
    currentSort: MemberStatisticsSortBy,
    onSortChange: (MemberStatisticsSortBy) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Member Statistics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sorting chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SortChip(
                    label = "Attendance",
                    isSelected = currentSort == MemberStatisticsSortBy.ATTENDANCE_HIGH,
                    onClick = { onSortChange(MemberStatisticsSortBy.ATTENDANCE_HIGH) }
                )
            }

            item {
                SortChip(
                    label = "Name",
                    isSelected = currentSort == MemberStatisticsSortBy.NAME_ASC,
                    onClick = { onSortChange(MemberStatisticsSortBy.NAME_ASC) }
                )
            }

            item {
                SortChip(
                    label = "Current Streak",
                    isSelected = currentSort == MemberStatisticsSortBy.CURRENT_STREAK,
                    onClick = { onSortChange(MemberStatisticsSortBy.CURRENT_STREAK) }
                )
            }

            item {
                SortChip(
                    label = "Category",
                    isSelected = currentSort == MemberStatisticsSortBy.CATEGORY,
                    onClick = { onSortChange(MemberStatisticsSortBy.CATEGORY) }
                )
            }
        }
    }
}

/**
 * Sort chip for filtering member statistics.
 *
 * @param label The chip label
 * @param isSelected Whether this chip is currently selected
 * @param onClick Callback when chip is clicked
 */
@Composable
fun SortChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(label)
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

/**
 * Individual member statistics card.
 *
 * @param stat The member statistics to display
 */
@Composable
fun MemberStatisticsItem(stat: MemberStatistics) {
    val attendanceColor = when {
        stat.attendancePercentage >= 80.0 -> Present
        stat.attendancePercentage >= 50.0 -> MaterialTheme.colorScheme.tertiary
        else -> ErrorRed
    }

    val categoryColor = when (stat.category) {
        com.attendancetracker.data.models.Category.ORIGINAL_MEMBER -> CategoryOM
        com.attendancetracker.data.models.Category.XENOS_TRANSFER -> CategoryXT
        com.attendancetracker.data.models.Category.RETURNING_NEW -> CategoryRN
        com.attendancetracker.data.models.Category.FIRST_TIMER -> CategoryFT
        com.attendancetracker.data.models.Category.VISITOR -> CategoryV
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Name and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stat.memberName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stat.category.abbreviation,
                    style = MaterialTheme.typography.bodySmall,
                    color = categoryColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Statistics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticLabel(
                    label = "Attendance",
                    value = "${stat.attendancePercentage.roundToInt()}%",
                    valueColor = attendanceColor
                )

                StatisticLabel(
                    label = "Attended",
                    value = "${stat.meetingsAttended}/${stat.totalMeetings}"
                )

                StatisticLabel(
                    label = "Streak",
                    value = if (stat.currentStreak >= 3) "${stat.currentStreak} 🔥" else "${stat.currentStreak}"
                )
            }
        }
    }
}

/**
 * Helper composable for statistic labels in member cards.
 *
 * @param label The statistic label
 * @param value The statistic value
 * @param valueColor Optional color for the value text
 */
@Composable
fun StatisticLabel(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Empty state displayed when no statistics data is available.
 */
@Composable
fun EmptyStatisticsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Analytics,
                contentDescription = "No Statistics",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.size(16.dp))

            Text(
                text = "No statistics available",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "Start marking attendance to see statistics here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
