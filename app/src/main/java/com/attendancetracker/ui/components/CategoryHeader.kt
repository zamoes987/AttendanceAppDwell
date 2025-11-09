package com.attendancetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.attendancetracker.data.models.Category
import com.attendancetracker.ui.theme.AttendanceTrackerTheme
import com.attendancetracker.ui.theme.CategoryFT
import com.attendancetracker.ui.theme.CategoryOM
import com.attendancetracker.ui.theme.CategoryRN
import com.attendancetracker.ui.theme.CategoryV
import com.attendancetracker.ui.theme.CategoryXT

/**
 * Composable header for a category section in the member list.
 *
 * Displays the category name, member count, and a "Select All" button
 * to quickly mark all members of this category as present.
 *
 * @param category The category this header represents
 * @param memberCount Total number of members in this category
 * @param selectedCount Number of selected members in this category
 * @param onSelectAll Callback when "Select All" button is clicked
 * @param modifier Optional modifier for customization
 */
@Composable
fun CategoryHeader(
    category: Category,
    memberCount: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Get category-specific color with transparency for background
    val categoryColor = when (category) {
        Category.ORIGINAL_MEMBER -> CategoryOM
        Category.XENOS_TRANSFER -> CategoryXT
        Category.RETURNING_NEW -> CategoryRN
        Category.FIRST_TIMER -> CategoryFT
        Category.VISITOR -> CategoryV
    }

    // Use a tinted version of the category color as background
    val backgroundColor = categoryColor.copy(alpha = 0.15f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category info column
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                // Category name
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = categoryColor
                )

                // Member count
                Text(
                    text = "$selectedCount / $memberCount present",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Select All button
            TextButton(
                onClick = onSelectAll
            ) {
                Text(
                    text = "Select All",
                    color = categoryColor
                )
            }
        }
    }
}

/**
 * Preview for CategoryHeader with partial selection.
 */
@Preview(showBackground = true)
@Composable
fun CategoryHeaderPreview() {
    AttendanceTrackerTheme {
        CategoryHeader(
            category = Category.ORIGINAL_MEMBER,
            memberCount = 10,
            selectedCount = 6,
            onSelectAll = {}
        )
    }
}

/**
 * Preview for CategoryHeader with no selection.
 */
@Preview(showBackground = true)
@Composable
fun CategoryHeaderNoSelectionPreview() {
    AttendanceTrackerTheme {
        CategoryHeader(
            category = Category.XENOS_TRANSFER,
            memberCount = 5,
            selectedCount = 0,
            onSelectAll = {}
        )
    }
}

/**
 * Preview for CategoryHeader with all selected.
 */
@Preview(showBackground = true)
@Composable
fun CategoryHeaderAllSelectedPreview() {
    AttendanceTrackerTheme {
        CategoryHeader(
            category = Category.RETURNING_NEW,
            memberCount = 8,
            selectedCount = 8,
            onSelectAll = {}
        )
    }
}
