package com.attendancetracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.attendancetracker.data.models.Category
import com.attendancetracker.data.models.Member
import com.attendancetracker.ui.theme.AttendanceTrackerTheme
import com.attendancetracker.ui.theme.CategoryFT
import com.attendancetracker.ui.theme.CategoryOM
import com.attendancetracker.ui.theme.CategoryRN
import com.attendancetracker.ui.theme.CategoryV
import com.attendancetracker.ui.theme.CategoryXT
import com.attendancetracker.ui.theme.Present

/**
 * Composable component for displaying a single member in a list with attendance toggle.
 *
 * Shows the member's name, category, and a checkbox for marking attendance.
 * The card has a colored start border indicating the member's category.
 *
 * Tap behavior:
 * - Tap anywhere on the card: Toggles attendance selection
 * - Long press on the card: Shows member attendance history (if onLongClick provided)
 *
 * @param member The member to display
 * @param isSelected Whether this member is marked as present
 * @param onToggle Callback when the attendance checkbox is toggled
 * @param onLongClick Optional callback when member is long-pressed (for viewing history)
 * @param modifier Optional modifier for customization
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemberListItem(
    member: Member,
    isSelected: Boolean,
    onToggle: (String) -> Unit,
    onLongClick: ((Member) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Get category-specific color for the border
    val categoryColor = when (member.category) {
        Category.ORIGINAL_MEMBER -> CategoryOM
        Category.XENOS_TRANSFER -> CategoryXT
        Category.RETURNING_NEW -> CategoryRN
        Category.FIRST_TIMER -> CategoryFT
        Category.VISITOR -> CategoryV
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .semantics {
                contentDescription = if (onLongClick != null) {
                    "Mark ${member.name} as present. Long press to view attendance history."
                } else {
                    "Mark ${member.name} as present"
                }
                stateDescription = if (isSelected) "Selected, present" else "Not selected, absent"
                role = Role.Checkbox
            }
            .combinedClickable(
                onClick = {
                    onToggle(member.id)
                },
                onLongClick = {
                    onLongClick?.invoke(member)
                }
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                categoryColor.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 4.dp,
            color = categoryColor
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Member info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Member name
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Member category
                Text(
                    text = member.category.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Attendance checkbox (visual only - card handles tap)
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = Present,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Preview for MemberListItem showing a selected member.
 */
@Preview(showBackground = true)
@Composable
fun MemberListItemPreview() {
    AttendanceTrackerTheme {
        MemberListItem(
            member = Member(
                id = "2025_1",
                name = "John Doe",
                category = Category.ORIGINAL_MEMBER,
                rowIndex = 1
            ),
            isSelected = true,
            onToggle = {}
        )
    }
}

/**
 * Preview for MemberListItem showing an unselected member.
 */
@Preview(showBackground = true)
@Composable
fun MemberListItemUnselectedPreview() {
    AttendanceTrackerTheme {
        MemberListItem(
            member = Member(
                id = "2025_2",
                name = "Jane Smith",
                category = Category.XENOS_TRANSFER,
                rowIndex = 2
            ),
            isSelected = false,
            onToggle = {}
        )
    }
}
