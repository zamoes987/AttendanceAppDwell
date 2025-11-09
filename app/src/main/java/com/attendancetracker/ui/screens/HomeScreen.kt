package com.attendancetracker.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.attendancetracker.data.models.Category
import com.attendancetracker.ui.components.CategoryHeader
import com.attendancetracker.ui.components.ErrorMessage
import com.attendancetracker.ui.components.LoadingIndicator
import com.attendancetracker.ui.components.MemberListItem
import com.attendancetracker.ui.components.SuccessMessage
import com.attendancetracker.viewmodel.AttendanceViewModel
import kotlinx.coroutines.delay

/**
 * Main home screen for marking attendance.
 *
 * Displays a list of all members grouped by category with checkboxes
 * for marking attendance. Includes quick actions for selecting/clearing
 * all members and a floating action button to save attendance.
 *
 * @param viewModel The AttendanceViewModel managing the screen state
 * @param onNavigateToHistory Callback to navigate to the history screen
 * @param onNavigateToSettings Callback to navigate to settings screen
 * @param onNavigateToMembers Callback to navigate to members screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AttendanceViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMembers: () -> Unit = {}
) {
    // Collect state from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val showSuccess by viewModel.showSaveSuccess.collectAsState()
    val selectedMembers by viewModel.selectedMembers.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()

    // Date picker dialog state
    var showDatePicker by remember { mutableStateOf(false) }

    // Auto-dismiss success message after 2 seconds
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(2000)
            viewModel.dismissSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Dwell CC branding
                        Column {
                            Text(
                                text = "DWELL",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Attendance",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // Date selector button
                        TextButton(
                            onClick = { showDatePicker = true },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Date",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(uiState.todayDateString)
                        }
                    }
                },
                actions = {
                    // Members button
                    IconButton(onClick = onNavigateToMembers) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Members"
                        )
                    }

                    // History button
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View History"
                        )
                    }

                    // Settings button
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }

                    // Refresh button
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.saveAttendance() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Attendance (${uiState.selectedCount})")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Show loading indicator
            if (isLoading) {
                LoadingIndicator()
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Show error message if present
                    error?.let { errorMessage ->
                        ErrorMessage(
                            message = errorMessage,
                            onDismiss = { viewModel.clearError() }
                        )
                    }

                    // Show success message if present
                    if (showSuccess) {
                        SuccessMessage(
                            message = "Attendance saved successfully!",
                            onDismiss = { viewModel.dismissSuccessMessage() }
                        )
                    }

                    // Bottom action bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Select All button
                            Button(
                                onClick = { viewModel.selectAll() }
                            ) {
                                Text("Select All")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Clear All button
                            OutlinedButton(
                                onClick = { viewModel.clearAll() }
                            ) {
                                Text("Clear All")
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Count display
                            Text(
                                text = "${uiState.selectedCount} / ${uiState.totalMembers}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Member list grouped by category
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        // Iterate through categories in order
                        val orderedCategories = listOf(
                            Category.ORIGINAL_MEMBER,
                            Category.XENOS_TRANSFER,
                            Category.RETURNING_NEW,
                            Category.FIRST_TIMER,
                            Category.VISITOR
                        )

                        orderedCategories.forEach { category ->
                            val membersInCategory = uiState.membersByCategory[category] ?: emptyList()

                            if (membersInCategory.isNotEmpty()) {
                                // Category header
                                item {
                                    val selectedInCategory = membersInCategory.count { member ->
                                        member.id in selectedMembers
                                    }

                                    CategoryHeader(
                                        category = category,
                                        memberCount = membersInCategory.size,
                                        selectedCount = selectedInCategory,
                                        onSelectAll = { viewModel.selectCategory(category) }
                                    )
                                }

                                // Members in this category
                                items(
                                    items = membersInCategory,
                                    key = { member -> member.id }
                                ) { member ->
                                    MemberListItem(
                                        member = member,
                                        isSelected = member.id in selectedMembers,
                                        onToggle = { viewModel.toggleMemberSelection(it) }
                                    )
                                }
                            }
                        }

                        // Add spacing at bottom for FAB
                        item {
                            Spacer(modifier = Modifier.size(80.dp))
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            viewModel.setSelectedDate(selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Select Date",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            )
        }
    }
}
