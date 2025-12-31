package com.attendancetracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.attendancetracker.data.models.AttendanceRecord
import com.attendancetracker.data.models.Category
import com.attendancetracker.data.models.Member
import com.attendancetracker.ui.components.AddEditMemberDialog
import com.attendancetracker.ui.components.CategoryHeader
import com.attendancetracker.ui.components.MemberAttendanceHistoryDialog
import com.attendancetracker.ui.theme.CategoryFT
import com.attendancetracker.ui.theme.CategoryOM
import com.attendancetracker.ui.theme.CategoryRN
import com.attendancetracker.ui.theme.CategoryV
import com.attendancetracker.ui.theme.CategoryXT
import com.attendancetracker.viewmodel.AttendanceViewModel

/**
 * Members management screen.
 *
 * Displays all members grouped by category with options to
 * add, edit, or remove members.
 *
 * @param viewModel The AttendanceViewModel managing the member data
 * @param onNavigateBack Callback to navigate back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val members by viewModel.members.collectAsState()
    val memberOperationMessage by viewModel.memberOperationMessage.collectAsState()
    val hideInfrequentMembers by viewModel.hideInfrequentMembers.collectAsState()

    // Dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var memberToEdit by remember { mutableStateOf<Member?>(null) }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }
    var showFilterExplanation by remember { mutableStateOf(false) }
    var memberToShowHistory by remember { mutableStateOf<Member?>(null) }

    // Auto-dismiss operation message after 2 seconds
    androidx.compose.runtime.LaunchedEffect(memberOperationMessage) {
        if (memberOperationMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMemberOperationMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Members (${members.size})")
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
                    // Info button to explain the filter
                    IconButton(onClick = { showFilterExplanation = true }) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Filter info",
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                    // Toggle to hide/show infrequent members (<40% attendance)
                    IconButton(onClick = { viewModel.toggleHideInfrequentMembers() }) {
                        Icon(
                            imageVector = if (hideInfrequentMembers) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (hideInfrequentMembers) "Show infrequent members" else "Hide infrequent members",
                            tint = MaterialTheme.colorScheme.onPrimary
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Member"
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Group members by category and display
                val membersByCategory = uiState.membersByCategory

                membersByCategory.forEach { (category, membersInCategory) ->
                    // Category header
                    item(key = "header_${category.name}") {
                        Spacer(modifier = Modifier.size(16.dp))
                        CategoryHeader(
                            category = category,
                            memberCount = membersInCategory.size,
                            selectedCount = membersInCategory.size,
                            onSelectAll = {}
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }

                    // Members in category
                    items(
                        items = membersInCategory,
                        key = { member -> member.id }
                    ) { member ->
                        MemberCard(
                            member = member,
                            onClick = { memberToShowHistory = member },
                            onLongClick = { memberToEdit = member }
                        )
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.size(80.dp))
                }
            }

            // Success/Error Message Snackbar
            memberOperationMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    androidx.compose.material3.Snackbar(
                        modifier = Modifier.padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearMemberOperationMessage() }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(message)
                    }
                }
            }
        }

        // Add Member Dialog
        if (showAddDialog) {
            AddEditMemberDialog(
                member = null,
                onDismiss = { showAddDialog = false },
                onSave = { name, category ->
                    viewModel.addMember(name, category)
                }
            )
        }

        // Edit Member Dialog
        memberToEdit?.let { member ->
            AddEditMemberDialog(
                member = member,
                onDismiss = { memberToEdit = null },
                onSave = { name, category ->
                    viewModel.updateMember(member.id, name, category)
                },
                onDelete = {
                    memberToDelete = member
                    memberToEdit = null
                }
            )
        }

        // Delete Confirmation Dialog
        memberToDelete?.let { member ->
            AlertDialog(
                onDismissRequest = { memberToDelete = null },
                title = { Text("Delete Member") },
                text = { Text("Are you sure you want to delete ${member.name}? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteMember(member.id)
                            memberToDelete = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { memberToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Filter Explanation Dialog
        if (showFilterExplanation) {
            AlertDialog(
                onDismissRequest = { showFilterExplanation = false },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("Hide Infrequent Members") },
                text = {
                    Column {
                        Text(
                            "The eye icon filters the member list based on attendance rate.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            "When enabled (crossed eye), members with less than 40% attendance are hidden. This helps you focus on regular attendees.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            "Tap the eye icon anytime to toggle this filter on or off.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showFilterExplanation = false }) {
                        Text("Got it")
                    }
                }
            )
        }

        // Member Attendance History Dialog
        memberToShowHistory?.let { member ->
            MemberAttendanceHistoryDialog(
                member = member,
                onDismiss = { memberToShowHistory = null }
            )
        }
    }
}

/**
 * Card displaying a single member's information.
 *
 * @param member The member to display
 * @param onClick Callback when the card is tapped (shows history)
 * @param onLongClick Callback when the card is long-pressed (edit member)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemberCard(
    member: Member,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val categoryColor = when (member.category) {
        Category.ORIGINAL_MEMBER -> CategoryOM
        Category.XENOS_TRANSFER -> CategoryXT
        Category.RETURNING_NEW -> CategoryRN
        Category.FIRST_TIMER -> CategoryFT
        Category.VISITOR -> CategoryV
    }

    // Calculate last attended date from attendance history
    val lastAttendedDate = member.attendanceHistory
        .filter { it.value } // Only dates where present
        .keys
        .mapNotNull { AttendanceRecord.parseDateFromSheet(it) }
        .maxOrNull() // Most recent date

    val lastAttendedText = lastAttendedDate?.let {
        AttendanceRecord.formatDateForSheet(it)
    } ?: "Never"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = categoryColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = member.category.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Last: $lastAttendedText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
