package com.attendancetracker.data.models

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Data class representing attendance data for a single meeting.
 *
 * Tracks which members were present and provides category-based statistics
 * for the meeting.
 *
 * **Thread Safety Note**: This class uses mutable collections (MutableSet, MutableMap)
 * for building records during sheet parsing. Once created and populated, records should
 * be treated as effectively immutable. Avoid modifying records concurrently from
 * multiple threads.
 *
 * @property date The meeting date as a LocalDate
 * @property dateString The date formatted as it appears in the sheet (e.g., "11/06/25")
 * @property columnIndex The column position of this date in the Google Sheet
 * @property presentMembers Set of member IDs who were marked present
 * @property categoryTotals Map of attendance counts per category
 */
data class AttendanceRecord(
    val date: LocalDate,
    val dateString: String,
    val columnIndex: Int,
    val presentMembers: MutableSet<String> = mutableSetOf(),
    val categoryTotals: MutableMap<Category, Int> = mutableMapOf()
) {
    /**
     * Returns the total attendance count for "regular" members only.
     *
     * Excludes First Timers and Visitors from the count, focusing on
     * core group attendance (OM, XT, RN).
     *
     * @return Sum of attendance for regular categories
     */
    fun getRegularTotal(): Int {
        return Category.getRegularCategories().sumOf { category ->
            categoryTotals[category] ?: 0
        }
    }

    /**
     * Returns the total attendance count across all categories.
     *
     * @return Sum of all category attendance counts
     */
    fun getTotalAttendance(): Int {
        return categoryTotals.values.sum()
    }

    /**
     * Marks a member as present for this meeting.
     *
     * Updates both the presentMembers set and the category totals.
     *
     * @param memberId The unique identifier of the member
     * @param category The member's category
     */
    fun markPresent(memberId: String, category: Category) {
        presentMembers.add(memberId)
        categoryTotals[category] = (categoryTotals[category] ?: 0) + 1
    }

    /**
     * Marks a member as absent for this meeting.
     *
     * Removes the member from presentMembers and decrements the category total
     * if they were previously marked present.
     *
     * @param memberId The unique identifier of the member
     * @param category The member's category
     */
    fun markAbsent(memberId: String, category: Category) {
        if (presentMembers.remove(memberId)) {
            val currentTotal = categoryTotals[category] ?: 0
            if (currentTotal > 0) {
                categoryTotals[category] = currentTotal - 1
            }
        }
    }

    /**
     * Checks if a specific member was present at this meeting.
     *
     * @param memberId The unique identifier of the member
     * @return True if the member was marked present, false otherwise
     */
    fun isPresent(memberId: String): Boolean {
        return memberId in presentMembers
    }

    companion object {
        /**
         * Formats a LocalDate into the sheet date format.
         *
         * Converts dates to "M/d/yy" format (e.g., "11/6/25" for November 6, 2025)
         *
         * @param date The date to format
         * @return Formatted date string for Google Sheets
         */
        fun formatDateForSheet(date: LocalDate): String {
            val formatter = DateTimeFormatter.ofPattern("M/d/yy")
            return date.format(formatter)
        }

        /**
         * Gets today's date formatted for the sheet.
         *
         * @return Today's date in sheet format (e.g., "11/6/25")
         */
        fun getTodayDateString(): String {
            return formatDateForSheet(LocalDate.now())
        }

        /**
         * Parses a date string from the sheet format into a LocalDate.
         *
         * Handles various date formats that might appear in the sheet.
         *
         * @param dateString The date string from the sheet (e.g., "11/06/25" or "11/6/25")
         * @return LocalDate object, or null if parsing fails
         */
        fun parseDateFromSheet(dateString: String): LocalDate? {
            return try {
                // Try parsing with flexible format (handles both "11/6/25" and "11/06/25")
                val parts = dateString.split("/")
                if (parts.size != 3) return null

                val month = parts[0].toIntOrNull() ?: return null
                val day = parts[1].toIntOrNull() ?: return null
                val year = parts[2].toIntOrNull() ?: return null

                // Assume 2000s for 2-digit years (25 = 2025)
                val fullYear = if (year < 100) 2000 + year else year

                LocalDate.of(fullYear, month, day)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Summary data for a single meeting's attendance.
 *
 * Provides a high-level overview of attendance with category breakdowns
 * and comparison to the previous meeting.
 *
 * @property weekOf The date of the meeting
 * @property totalPresent Total number of attendees
 * @property categoryBreakdown Map of attendance counts per category
 * @property comparisonToPrevious Change in attendance compared to previous meeting (nullable)
 */
data class AttendanceSummary(
    val weekOf: LocalDate,
    val totalPresent: Int,
    val categoryBreakdown: Map<Category, Int>,
    val comparisonToPrevious: Int? = null
)
