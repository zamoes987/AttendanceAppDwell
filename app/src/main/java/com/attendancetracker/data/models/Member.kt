package com.attendancetracker.data.models

/**
 * Data class representing a member in the attendance tracking system.
 *
 * Each member corresponds to a row in the Google Sheet and tracks their
 * attendance history across multiple meetings.
 *
 * FIXED: Changed attendanceHistory from MutableMap to immutable Map for thread safety.
 * Use withAttendance() to create new Member instances with updated attendance.
 *
 * @property id Unique identifier for the member (format: "2025_rowNumber")
 * @property name Full name of the member as it appears in the sheet
 * @property category Member's category (OM, XT, RN, FT, V)
 * @property rowIndex Actual row number in the Google Sheet (1-indexed, where row 1 is the header)
 * @property attendanceHistory Immutable map of date strings to attendance status (true = present, false = absent)
 */
data class Member(
    val id: String,
    val name: String,
    val category: Category,
    val rowIndex: Int,
    val attendanceHistory: Map<String, Boolean> = emptyMap()
) {
    /**
     * Returns the total number of meetings this member has attended.
     *
     * @return Count of all dates where attendance was marked as present
     */
    fun getTotalAttendance(): Int {
        return attendanceHistory.count { it.value }
    }

    /**
     * Checks if the member was present on a specific date.
     *
     * @param dateString The date in sheet format (e.g., "11/06/25")
     * @return True if the member was present, false if absent or no record exists
     */
    fun wasPresent(dateString: String): Boolean {
        return attendanceHistory[dateString] ?: false
    }

    /**
     * Creates a new Member instance with updated attendance for a specific date.
     *
     * This method follows the immutable pattern - it returns a new Member object
     * rather than modifying the existing one, preventing thread safety issues.
     *
     * @param dateString The date in sheet format (e.g., "11/06/25")
     * @param present True if present, false if absent
     * @return New Member instance with updated attendance history
     */
    fun withAttendance(dateString: String, present: Boolean): Member {
        return this.copy(
            attendanceHistory = attendanceHistory + (dateString to present)
        )
    }

    /**
     * Calculates the member's attendance percentage across all recorded meetings.
     *
     * @return Percentage (0.0 to 100.0) of meetings attended, or 0.0 if no history exists
     */
    fun getAttendancePercentage(): Double {
        if (attendanceHistory.isEmpty()) return 0.0
        val presentCount = attendanceHistory.count { it.value }
        return (presentCount.toDouble() / attendanceHistory.size) * 100.0
    }
}

/**
 * Extension function to group a list of members by their category.
 *
 * @return Map where keys are Category enums and values are lists of members in that category
 *
 * Example:
 * ```
 * val grouped = members.groupByCategory()
 * val omMembers = grouped[Category.ORIGINAL_MEMBER] // List of OM members
 * ```
 */
fun List<Member>.groupByCategory(): Map<Category, List<Member>> {
    return this.groupBy { it.category }
}

/**
 * Extension function to sort members first by category, then alphabetically by name.
 *
 * Category sort order: OM, XT, RN, FT, V
 * Within each category, members are sorted alphabetically by name.
 *
 * @return Sorted list of members
 */
fun List<Member>.sortByNameAndCategory(): List<Member> {
    val categoryOrder = listOf(
        Category.ORIGINAL_MEMBER,
        Category.XENOS_TRANSFER,
        Category.RETURNING_NEW,
        Category.FIRST_TIMER,
        Category.VISITOR
    )

    return this.sortedWith(
        compareBy<Member> { categoryOrder.indexOf(it.category) }
            .thenBy { it.name }
    )
}
