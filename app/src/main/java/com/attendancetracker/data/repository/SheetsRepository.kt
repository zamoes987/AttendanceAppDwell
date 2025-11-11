package com.attendancetracker.data.repository

import android.content.Context
import com.attendancetracker.data.api.GoogleSheetsService
import com.attendancetracker.data.models.AttendanceRecord
import com.attendancetracker.data.models.AttendanceSummary
import com.attendancetracker.data.models.Category
import com.attendancetracker.data.models.Member
import com.attendancetracker.data.models.groupByCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import com.attendancetracker.data.models.AttendanceTrend
import com.attendancetracker.data.models.CategoryStatistics
import com.attendancetracker.data.models.MemberStatistics
import com.attendancetracker.data.models.OverallStatistics
import com.attendancetracker.data.models.TrendAnalysis
import com.attendancetracker.data.models.TrendDirection

/**
 * Enum for sorting member statistics.
 */
enum class MemberStatisticsSortBy {
    NAME_ASC,           // Alphabetically by name (A-Z)
    NAME_DESC,          // Reverse alphabetically (Z-A)
    ATTENDANCE_HIGH,    // Highest attendance percentage first
    ATTENDANCE_LOW,     // Lowest attendance percentage first
    CURRENT_STREAK,     // Highest current streak first
    LONGEST_STREAK,     // Highest longest streak first
    CATEGORY            // Grouped by category (OM, XT, RN, FT, V)
}

/**
 * Repository layer providing a clean API between UI and Google Sheets service.
 *
 * Manages data state using Kotlin Flows and provides caching for better performance.
 * All data operations are exposed through this repository to maintain separation of concerns.
 *
 * @property context Application context
 * @property accountName Google account email for authentication
 */
class SheetsRepository(
    context: Context,
    accountName: String
) {
    private val sheetsService = GoogleSheetsService(context, accountName)
    private val preferencesRepository = PreferencesRepository(context)

    // Coroutine scope for collecting flows
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State flows for reactive data management
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: Flow<List<Member>> = _members.asStateFlow()

    // Raw attendance records (before filtering skipped dates)
    private val _rawAttendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())

    // Filtered attendance records (exposed to ViewModel - excludes skipped dates)
    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: Flow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    // Skipped dates state
    private val _skippedDates = MutableStateFlow<Set<String>>(emptySet())
    val skippedDates: Flow<Set<String>> = _skippedDates.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: Flow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: Flow<String?> = _error.asStateFlow()

    init {
        // Collect skipped dates from PreferencesRepository
        repositoryScope.launch {
            preferencesRepository.skippedDatesFlow.collect { skippedDateStrings ->
                _skippedDates.value = skippedDateStrings
                // Re-filter attendance records when skipped dates change
                _attendanceRecords.value = _rawAttendanceRecords.value.excludingSkippedDates()
            }
        }
    }

    /**
     * Loads all data from Google Sheets (members and attendance history).
     *
     * This is the primary data loading function that should be called when
     * the app starts or when a full refresh is needed.
     *
     * Updates the members and attendanceRecords state flows on success.
     * Sets error state on failure.
     *
     * @return Result indicating success or failure
     */
    suspend fun loadAllData(): Result<Unit> {
        _isLoading.value = true
        _error.value = null

        return try {
            // Load members first
            val membersResult = sheetsService.readMembers()
            if (membersResult.isFailure) {
                val exception = membersResult.exceptionOrNull()
                _error.value = "Failed to load members: ${exception?.message}"
                return Result.failure(exception ?: Exception("Unknown error loading members"))
            }

            val membersList = membersResult.getOrNull() ?: emptyList()

            // Load attendance history for all members
            val attendanceResult = sheetsService.readAllAttendance(membersList)
            if (attendanceResult.isFailure) {
                val exception = attendanceResult.exceptionOrNull()
                _error.value = "Failed to load attendance: ${exception?.message}"
                return Result.failure(exception ?: Exception("Unknown error loading attendance"))
            }

            // CRITICAL FIX: Extract both updated members and attendance records
            val (updatedMembers, records) = attendanceResult.getOrNull()
                ?: (emptyList<Member>() to emptyList())

            _members.value = updatedMembers  // Members now have populated attendanceHistory
            _rawAttendanceRecords.value = records  // Store raw records
            _attendanceRecords.value = records.excludingSkippedDates()  // Store filtered records

            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = "Error loading data: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Refreshes just the member list from the sheet.
     *
     * Use this when you know only the member list has changed
     * and don't need to reload all attendance history.
     *
     * @return Result indicating success or failure
     */
    suspend fun refreshMembers(): Result<Unit> {
        _isLoading.value = true
        _error.value = null

        return try {
            val result = sheetsService.readMembers()
            if (result.isSuccess) {
                _members.value = result.getOrNull() ?: emptyList()
                Result.success(Unit)
            } else {
                val exception = result.exceptionOrNull()
                _error.value = "Failed to refresh members: ${exception?.message}"
                Result.failure(exception ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            _error.value = "Error refreshing members: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Saves attendance for the current Thursday.
     *
     * Writes attendance data to Google Sheets and updates local cache.
     *
     * @param presentMemberIds Set of member IDs who were present
     * @return Result indicating success or failure
     */
    suspend fun saveAttendance(presentMemberIds: Set<String>, date: LocalDate = getCurrentThursday()): Result<Unit> {
        _isLoading.value = true
        _error.value = null

        return try {
            val dateString = AttendanceRecord.formatDateForSheet(date)
            val membersList = _members.value

            // Write to Google Sheets
            val result = sheetsService.writeAttendance(dateString, presentMemberIds, membersList)

            if (result.isSuccess) {
                // Update local cache
                updateLocalAttendanceCache(dateString, presentMemberIds, date)
                Result.success(Unit)
            } else {
                val exception = result.exceptionOrNull()
                _error.value = "Failed to save attendance: ${exception?.message}"
                Result.failure(exception ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            _error.value = "Error saving attendance: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Updates the local cache after saving attendance.
     *
     * This ensures the UI reflects changes immediately without reloading from the sheet.
     * FIXED: Creates new immutable objects instead of mutating existing ones to prevent
     * ConcurrentModificationException in multi-threaded environment.
     */
    private fun updateLocalAttendanceCache(
        dateString: String,
        presentMemberIds: Set<String>,
        date: LocalDate
    ) {
        val membersList = _members.value

        // Create NEW member objects with updated attendance history (immutable pattern)
        val updatedMembers = membersList.map { member ->
            val isPresent = member.id in presentMemberIds
            // Create new attendance history map with the updated value
            val newHistory = member.attendanceHistory.toMutableMap().apply {
                put(dateString, isPresent)
            }
            // Return new Member instance with updated history
            member.copy(attendanceHistory = newHistory)
        }

        // Update members StateFlow with new list
        _members.value = updatedMembers

        // Find or create attendance record
        val existingRecords = _attendanceRecords.value
        val existingRecordIndex = existingRecords.indexOfFirst { it.dateString == dateString }

        // Create NEW AttendanceRecord instead of mutating existing one
        val newRecord = AttendanceRecord(
            date = date,
            dateString = dateString,
            columnIndex = if (existingRecordIndex >= 0) existingRecords[existingRecordIndex].columnIndex else -1,
            presentMembers = presentMemberIds.toMutableSet(), // New mutable set
            categoryTotals = mutableMapOf() // New mutable map
        )

        // Populate category totals
        updatedMembers.forEach { member ->
            if (member.id in presentMemberIds) {
                newRecord.categoryTotals[member.category] =
                    (newRecord.categoryTotals[member.category] ?: 0) + 1
            }
        }

        // Create new records list with the updated record
        val updatedRecords = if (existingRecordIndex >= 0) {
            // Replace existing record
            existingRecords.toMutableList().apply {
                set(existingRecordIndex, newRecord)
            }
        } else {
            // Add new record
            existingRecords + newRecord
        }

        // Update state with new sorted list (both raw and filtered)
        val sortedRecords = updatedRecords.sortedBy { it.date }
        _rawAttendanceRecords.value = sortedRecords
        _attendanceRecords.value = sortedRecords.excludingSkippedDates()
    }

    /**
     * Tests the connection to Google Sheets.
     *
     * @return Result with true if connection successful, or failure with exception
     */
    suspend fun testConnection(): Result<Boolean> {
        return sheetsService.testConnection()
    }

    /**
     * Returns the current Thursday date.
     *
     * If today is Thursday, returns today.
     * Otherwise, returns the next Thursday.
     *
     * @return LocalDate of current or next Thursday
     */
    fun getCurrentThursday(): LocalDate {
        val today = LocalDate.now()
        return if (today.dayOfWeek == DayOfWeek.THURSDAY) {
            today
        } else {
            today.with(TemporalAdjusters.next(DayOfWeek.THURSDAY))
        }
    }

    /**
     * Gets the attendance record for the current Thursday.
     *
     * @return AttendanceRecord for today's meeting, or null if not found
     */
    fun getTodayAttendance(): AttendanceRecord? {
        val thursday = getCurrentThursday()
        val dateString = AttendanceRecord.formatDateForSheet(thursday)
        return _attendanceRecords.value.find { it.dateString == dateString }
    }

    /**
     * Groups current members by their category.
     *
     * @return Map of categories to member lists
     */
    fun getMembersByCategory(): Map<Category, List<Member>> {
        return _members.value.groupByCategory()
    }

    /**
     * Gets an attendance summary for a specific date.
     *
     * @param date The date to get summary for
     * @return AttendanceSummary or null if no record exists for that date
     */
    fun getAttendanceSummary(date: LocalDate): AttendanceSummary? {
        val record = _attendanceRecords.value.find { it.date == date } ?: return null

        return AttendanceSummary(
            weekOf = record.date,
            totalPresent = record.getTotalAttendance(),
            categoryBreakdown = record.categoryTotals.toMap(),
            comparisonToPrevious = null // Could be calculated if needed
        )
    }

    /**
     * Gets summaries for all attendance records.
     *
     * @return List of AttendanceSummary objects with comparison data
     */
    fun getAllSummaries(): List<AttendanceSummary> {
        val records = _attendanceRecords.value.sortedByDescending { it.date }

        return records.mapIndexed { index, record ->
            val comparison = if (index < records.size - 1) {
                val previousTotal = records[index + 1].getTotalAttendance()
                record.getTotalAttendance() - previousTotal
            } else {
                null
            }

            AttendanceSummary(
                weekOf = record.date,
                totalPresent = record.getTotalAttendance(),
                categoryBreakdown = record.categoryTotals.toMap(),
                comparisonToPrevious = comparison
            )
        }
    }

    /**
     * Searches for members by name (case-insensitive).
     *
     * @param query Search query string
     * @return List of members whose names contain the query
     */
    fun searchMembers(query: String): List<Member> {
        if (query.isBlank()) return _members.value

        return _members.value.filter { member ->
            member.name.contains(query, ignoreCase = true)
        }
    }

    /**
     * Gets a specific member by ID.
     *
     * @param id Member ID to search for
     * @return Member object or null if not found
     */
    fun getMemberById(id: String): Member? {
        return _members.value.find { it.id == id }
    }

    /**
     * Calculates category attendance trends over recent weeks.
     *
     * @param weeksBack Number of weeks to look back (default: 4)
     * @return Map of categories to average attendance percentages
     */
    fun getCategoryTrends(weeksBack: Int = 4): Map<Category, Double> {
        val recentRecords = _attendanceRecords.value
            .sortedByDescending { it.date }
            .take(weeksBack)

        if (recentRecords.isEmpty()) return emptyMap()

        val categoryAverages = mutableMapOf<Category, Double>()

        Category.entries.forEach { category ->
            val categoryMembers = _members.value.filter { it.category == category }
            if (categoryMembers.isEmpty()) return@forEach

            val totalPossible = categoryMembers.size * recentRecords.size
            val totalPresent = recentRecords.sumOf { record ->
                record.categoryTotals[category] ?: 0
            }

            val percentage = if (totalPossible > 0) {
                (totalPresent.toDouble() / totalPossible) * 100.0
            } else {
                0.0
            }

            categoryAverages[category] = percentage
        }

        return categoryAverages
    }

    /**
     * Adds a new member to the system.
     *
     * Writes the member to Google Sheets and updates local cache.
     *
     * @param name The member's name
     * @param category The member's category
     * @return Result indicating success or failure
     */
    suspend fun addMember(name: String, category: Category): Result<Unit> {
        _isLoading.value = true
        _error.value = null

        return try {
            val result = sheetsService.addMember(name, category)

            if (result.isSuccess) {
                // Refresh members list to get the new member with proper row index
                refreshMembers()
                Result.success(Unit)
            } else {
                val exception = result.exceptionOrNull()
                _error.value = "Failed to add member: ${exception?.message}"
                Result.failure(exception ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            _error.value = "Error adding member: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Updates an existing member's information.
     *
     * Updates the member in Google Sheets and local cache.
     *
     * @param memberId The member's ID
     * @param name The new name
     * @param category The new category
     * @return Result indicating success or failure
     */
    suspend fun updateMember(memberId: String, name: String, category: Category): Result<Unit> {
        _isLoading.value = true
        _error.value = null

        return try {
            val member = getMemberById(memberId)
            if (member == null) {
                _error.value = "Member not found"
                return Result.failure(Exception("Member not found"))
            }

            val result = sheetsService.updateMember(member, name, category)

            if (result.isSuccess) {
                // Refresh members list to get updated data
                refreshMembers()
                Result.success(Unit)
            } else {
                val exception = result.exceptionOrNull()
                _error.value = "Failed to update member: ${exception?.message}"
                Result.failure(exception ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            _error.value = "Error updating member: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Deletes a member from the system.
     *
     * Removes the member from Google Sheets and local cache.
     *
     * @param memberId The member's ID
     * @return Result indicating success or failure
     */
    suspend fun deleteMember(memberId: String): Result<Unit> {
        _isLoading.value = true
        _error.value = null

        return try {
            val member = getMemberById(memberId)
            if (member == null) {
                _error.value = "Member not found"
                return Result.failure(Exception("Member not found"))
            }

            val result = sheetsService.deleteMember(member)

            if (result.isSuccess) {
                // Refresh members list to remove deleted member
                refreshMembers()
                Result.success(Unit)
            } else {
                val exception = result.exceptionOrNull()
                _error.value = "Failed to delete member: ${exception?.message}"
                Result.failure(exception ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            _error.value = "Error deleting member: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Clears the current error state.
     */
    fun clearError() {
        _error.value = null
    }

    // ========== SKIPPED DATES METHODS ==========

    /**
     * Toggles whether a date is marked as skipped ("No Meeting").
     * If the date is currently skipped, it will be unskipped.
     * If the date is not skipped, it will be marked as skipped.
     *
     * @param date The date to toggle
     */
    suspend fun toggleDateSkipped(date: LocalDate) {
        val dateString = date.toString() // ISO-8601 format (yyyy-MM-dd)

        if (dateString in _skippedDates.value) {
            // Unskip the date
            preferencesRepository.removeSkippedDate(date)
        } else {
            // Skip the date
            preferencesRepository.addSkippedDate(date)
        }
    }

    /**
     * Extension function to filter out skipped dates from attendance records.
     * Used internally to create the filtered view of attendance records.
     */
    private fun List<AttendanceRecord>.excludingSkippedDates(): List<AttendanceRecord> {
        val skipped = _skippedDates.value
        return this.filter { record ->
            !skipped.contains(record.date.toString())
        }
    }

    // ========== STATISTICS METHODS ==========

    /**
     * Calculates overall statistics for all members across all meetings.
     *
     * Operates on cached data (no network calls).
     * Returns metrics about total members, meetings, and attendance patterns.
     *
     * @return OverallStatistics with aggregated metrics, or default values if no data
     */
    fun calculateOverallStatistics(): OverallStatistics {
        val members = _members.value
        val records = _attendanceRecords.value

        if (members.isEmpty() || records.isEmpty()) {
            return OverallStatistics(
                totalMembers = members.size,
                totalMeetings = records.size,
                averageAttendance = 0.0,
                averageAttendancePercentage = 0.0,
                highestAttendance = 0,
                lowestAttendance = 0,
                mostRecentMeetingDate = null
            )
        }

        // Calculate average attendance across all meetings
        val totalAttendances = records.sumOf { it.getTotalAttendance() }
        val averageAttendance = totalAttendances.toDouble() / records.size

        // Calculate average attendance percentage
        val averageAttendancePercentage = if (members.size > 0) {
            (averageAttendance / members.size) * 100.0
        } else {
            0.0
        }

        // Find highest and lowest attendance
        val highestAttendance = records.maxOfOrNull { it.getTotalAttendance() } ?: 0
        val lowestAttendance = records.minOfOrNull { it.getTotalAttendance() } ?: 0

        // Get most recent meeting date
        val mostRecentMeetingDate = records.maxByOrNull { it.date }?.date

        return OverallStatistics(
            totalMembers = members.size,
            totalMeetings = records.size,
            averageAttendance = averageAttendance,
            averageAttendancePercentage = averageAttendancePercentage,
            highestAttendance = highestAttendance,
            lowestAttendance = lowestAttendance,
            mostRecentMeetingDate = mostRecentMeetingDate
        )
    }

    /**
     * Calculates statistics for each member.
     *
     * Operates on cached data (no network calls).
     * Returns a list of member statistics with attendance percentages and streaks.
     *
     * @param sortBy How to sort the results (default: NAME_ASC)
     * @return List of MemberStatistics sorted according to sortBy parameter
     */
    fun calculateMemberStatistics(sortBy: MemberStatisticsSortBy = MemberStatisticsSortBy.NAME_ASC): List<MemberStatistics> {
        val members = _members.value
        val records = _attendanceRecords.value

        if (members.isEmpty()) {
            return emptyList()
        }

        // Calculate statistics for each member
        val memberStats = members.map { member ->
            MemberStatistics.fromMember(member, records)
        }

        // Sort according to the specified criteria
        return when (sortBy) {
            MemberStatisticsSortBy.NAME_ASC -> memberStats.sortedBy { it.memberName }
            MemberStatisticsSortBy.NAME_DESC -> memberStats.sortedByDescending { it.memberName }
            MemberStatisticsSortBy.ATTENDANCE_HIGH -> memberStats.sortedByDescending { it.attendancePercentage }
            MemberStatisticsSortBy.ATTENDANCE_LOW -> memberStats.sortedBy { it.attendancePercentage }
            MemberStatisticsSortBy.CURRENT_STREAK -> memberStats.sortedByDescending { it.currentStreak }
            MemberStatisticsSortBy.LONGEST_STREAK -> memberStats.sortedByDescending { it.longestStreak }
            MemberStatisticsSortBy.CATEGORY -> {
                val categoryOrder = listOf(
                    Category.ORIGINAL_MEMBER,
                    Category.XENOS_TRANSFER,
                    Category.RETURNING_NEW,
                    Category.FIRST_TIMER,
                    Category.VISITOR
                )
                memberStats.sortedWith(
                    compareBy<MemberStatistics> { categoryOrder.indexOf(it.category) }
                        .thenBy { it.memberName }
                )
            }
        }
    }

    /**
     * Calculates statistics for each category.
     *
     * Operates on cached data (no network calls).
     * Returns aggregated attendance metrics for each category.
     *
     * @return List of CategoryStatistics, one per category that has members
     */
    fun calculateCategoryStatistics(): List<CategoryStatistics> {
        val members = _members.value
        val records = _attendanceRecords.value

        if (members.isEmpty()) {
            return emptyList()
        }

        val totalMeetings = records.size

        // Group members by category
        val membersByCategory = members.groupByCategory()

        // Calculate statistics for each category
        return membersByCategory.map { (category, categoryMembers) ->
            val memberCount = categoryMembers.size
            val totalPossibleAttendances = memberCount * totalMeetings

            // Count total meetings attended by all members in this category
            val totalMeetingsAttended = categoryMembers.sumOf { member ->
                member.getTotalAttendance()
            }

            // Calculate average attendance percentage for the category
            val averageAttendancePercentage = if (totalPossibleAttendances > 0) {
                (totalMeetingsAttended.toDouble() / totalPossibleAttendances) * 100.0
            } else {
                0.0
            }

            CategoryStatistics(
                category = category,
                memberCount = memberCount,
                averageAttendancePercentage = averageAttendancePercentage,
                totalMeetingsAttended = totalMeetingsAttended,
                totalPossibleAttendances = totalPossibleAttendances
            )
        }.sortedBy { it.category.ordinal } // Sort by category order (OM, XT, RN, FT, V)
    }

    /**
     * Calculates attendance trend over recent meetings.
     *
     * Operates on cached data (no network calls).
     * Analyzes the last N meetings to determine if attendance is improving,
     * stable, or declining.
     *
     * @param meetingsCount Number of recent meetings to analyze (default: 10)
     * @return TrendAnalysis with trend points and direction assessment
     */
    fun calculateAttendanceTrend(meetingsCount: Int = 10): TrendAnalysis {
        val members = _members.value
        val records = _attendanceRecords.value

        if (records.isEmpty()) {
            return TrendAnalysis(
                trendPoints = emptyList(),
                direction = TrendDirection.STABLE,
                changePercentage = 0.0
            )
        }

        // Get the most recent N meetings
        val recentRecords = records.sortedByDescending { it.date }.take(meetingsCount)

        // Calculate trend points
        val trendPoints = recentRecords.map { record ->
            val attendanceCount = record.getTotalAttendance()
            val attendancePercentage = if (members.size > 0) {
                (attendanceCount.toDouble() / members.size) * 100.0
            } else {
                0.0
            }

            AttendanceTrend(
                date = record.date,
                attendanceCount = attendanceCount,
                attendancePercentage = attendancePercentage
            )
        }.sortedBy { it.date } // Sort chronologically for trend analysis

        // Determine trend direction
        if (trendPoints.size < 2) {
            return TrendAnalysis(
                trendPoints = trendPoints,
                direction = TrendDirection.STABLE,
                changePercentage = 0.0
            )
        }

        // Calculate percentage change from first to last meeting
        val firstAttendance = trendPoints.first().attendancePercentage
        val lastAttendance = trendPoints.last().attendancePercentage

        val changePercentage = if (firstAttendance > 0) {
            ((lastAttendance - firstAttendance) / firstAttendance) * 100.0
        } else {
            0.0
        }

        // Determine direction based on change percentage
        // Using thresholds: >5% = improving, <-5% = declining, otherwise stable
        val direction = when {
            changePercentage > 5.0 -> TrendDirection.IMPROVING
            changePercentage < -5.0 -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }

        return TrendAnalysis(
            trendPoints = trendPoints,
            direction = direction,
            changePercentage = changePercentage
        )
    }
}
