package com.attendancetracker.data.models

import java.time.LocalDate

/**
 * Overall statistics for all members across all recorded meetings.
 *
 * Provides high-level metrics for the entire group's attendance history.
 *
 * @property totalMembers Total number of members in the system
 * @property totalMeetings Total number of recorded meetings
 * @property averageAttendance Average attendance across all meetings
 * @property averageAttendancePercentage Average attendance as a percentage of total members
 * @property highestAttendance Highest attendance count in a single meeting
 * @property lowestAttendance Lowest attendance count in a single meeting
 * @property mostRecentMeetingDate Date of the most recent meeting, or null if no meetings
 */
data class OverallStatistics(
    val totalMembers: Int,
    val totalMeetings: Int,
    val averageAttendance: Double,
    val averageAttendancePercentage: Double,
    val highestAttendance: Int,
    val lowestAttendance: Int,
    val mostRecentMeetingDate: LocalDate?
)

/**
 * Individual member statistics.
 *
 * Tracks attendance metrics for a single member including streaks and percentages.
 *
 * @property memberId Unique identifier for the member
 * @property memberName Name of the member
 * @property category Member's category
 * @property totalMeetings Total number of meetings the member has records for
 * @property meetingsAttended Number of meetings the member attended
 * @property attendancePercentage Percentage of meetings attended (0.0 to 100.0)
 * @property currentStreak Current consecutive attendance streak (from most recent meeting backwards)
 * @property longestStreak Longest consecutive attendance streak in history
 */
data class MemberStatistics(
    val memberId: String,
    val memberName: String,
    val category: Category,
    val totalMeetings: Int,
    val meetingsAttended: Int,
    val attendancePercentage: Double,
    val currentStreak: Int,
    val longestStreak: Int
) {
    companion object {
        /**
         * Creates MemberStatistics from a Member object and all attendance records.
         *
         * This function calculates streaks by analyzing the member's attendance history
         * against all recorded meetings.
         *
         * CRITICAL STREAK CALCULATION LOGIC:
         * - Current streak: Counts consecutive meetings from most recent backwards
         * - Longest streak: Finds the best consecutive run in entire history
         * - Both streaks count consecutive ATTENDED meetings only
         *
         * @param member The member to calculate statistics for
         * @param allRecords All attendance records sorted by date (newest first recommended)
         * @return MemberStatistics object with calculated metrics
         */
        fun fromMember(member: Member, allRecords: List<AttendanceRecord>): MemberStatistics {
            val totalMeetings = allRecords.size
            val meetingsAttended = member.getTotalAttendance()

            val attendancePercentage = if (totalMeetings > 0) {
                (meetingsAttended.toDouble() / totalMeetings) * 100.0
            } else {
                0.0
            }

            // Calculate streaks - requires sorted records (newest first)
            val sortedRecords = allRecords.sortedByDescending { it.date }

            // DEBUG: Log for Stormie Harlan
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.BASE &&
                member.name.contains("Stormie", ignoreCase = true)) {
                android.util.Log.d("Statistics", "Stormie - Total records: ${sortedRecords.size}, Total attendance from history: ${member.getTotalAttendance()}")
                val presentCount = sortedRecords.count { it.isPresent(member.id) }
                android.util.Log.d("Statistics", "Stormie - Present in records: $presentCount")
            }

            // Current streak: count from most recent meeting backwards
            var currentStreak = 0
            for (record in sortedRecords) {
                if (record.isPresent(member.id)) {
                    currentStreak++
                } else {
                    break // Stop at first absence
                }
            }

            // Longest streak: find best consecutive run in entire history
            var longestStreak = 0
            var tempStreak = 0
            for (record in sortedRecords.asReversed()) { // Go chronologically (oldest to newest)
                if (record.isPresent(member.id)) {
                    tempStreak++
                    if (tempStreak > longestStreak) {
                        longestStreak = tempStreak
                    }
                } else {
                    tempStreak = 0 // Reset on absence
                }
            }

            return MemberStatistics(
                memberId = member.id,
                memberName = member.name,
                category = member.category,
                totalMeetings = totalMeetings,
                meetingsAttended = meetingsAttended,
                attendancePercentage = attendancePercentage,
                currentStreak = currentStreak,
                longestStreak = longestStreak
            )
        }
    }
}

/**
 * Statistics for a specific category.
 *
 * Aggregates attendance metrics across all members in a category.
 *
 * @property category The category these statistics represent
 * @property memberCount Number of members in this category
 * @property averageAttendancePercentage Average attendance percentage for the category
 * @property totalMeetingsAttended Sum of all meetings attended by category members
 * @property totalPossibleAttendances Total possible attendances (memberCount * totalMeetings)
 */
data class CategoryStatistics(
    val category: Category,
    val memberCount: Int,
    val averageAttendancePercentage: Double,
    val totalMeetingsAttended: Int,
    val totalPossibleAttendances: Int
)

/**
 * A single data point in an attendance trend analysis.
 *
 * Represents attendance for one meeting in the trend.
 *
 * @property date The meeting date
 * @property attendanceCount Number of attendees at this meeting
 * @property attendancePercentage Percentage of total members present
 */
data class AttendanceTrend(
    val date: LocalDate,
    val attendanceCount: Int,
    val attendancePercentage: Double
)

/**
 * Direction of an attendance trend.
 */
enum class TrendDirection {
    IMPROVING,  // Attendance is increasing
    STABLE,     // Attendance is staying roughly the same
    DECLINING   // Attendance is decreasing
}

/**
 * Analysis of attendance trends over recent meetings.
 *
 * Provides trend data points and an overall direction assessment.
 *
 * @property trendPoints List of attendance data points sorted chronologically
 * @property direction Overall trend direction (IMPROVING, STABLE, or DECLINING)
 * @property changePercentage Percentage change from first to last meeting in trend
 */
data class TrendAnalysis(
    val trendPoints: List<AttendanceTrend>,
    val direction: TrendDirection,
    val changePercentage: Double
)
