package com.attendancetracker.data.repository

import android.content.Context
import com.attendancetracker.data.api.GoogleSheetsService
import com.attendancetracker.data.models.AttendanceRecord
import com.attendancetracker.data.models.AttendanceSummary
import com.attendancetracker.data.models.Category
import com.attendancetracker.data.models.Member
import com.attendancetracker.data.models.groupByCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

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

    // State flows for reactive data management
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: Flow<List<Member>> = _members.asStateFlow()

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: Flow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: Flow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: Flow<String?> = _error.asStateFlow()

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
            _members.value = membersList

            // Load attendance history for all members
            val attendanceResult = sheetsService.readAllAttendance(membersList)
            if (attendanceResult.isFailure) {
                val exception = attendanceResult.exceptionOrNull()
                _error.value = "Failed to load attendance: ${exception?.message}"
                return Result.failure(exception ?: Exception("Unknown error loading attendance"))
            }

            val records = attendanceResult.getOrNull() ?: emptyList()
            _attendanceRecords.value = records

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
     */
    private fun updateLocalAttendanceCache(
        dateString: String,
        presentMemberIds: Set<String>,
        date: LocalDate
    ) {
        val membersList = _members.value

        // Update member attendance histories
        membersList.forEach { member ->
            member.markAttendance(dateString, member.id in presentMemberIds)
        }

        // Find or create attendance record
        val existingRecords = _attendanceRecords.value.toMutableList()
        val existingRecordIndex = existingRecords.indexOfFirst { it.dateString == dateString }

        val record = if (existingRecordIndex >= 0) {
            existingRecords[existingRecordIndex]
        } else {
            AttendanceRecord(
                date = date,
                dateString = dateString,
                columnIndex = -1 // Column index doesn't matter for cache
            ).also { existingRecords.add(it) }
        }

        // Clear and rebuild the record
        record.presentMembers.clear()
        record.categoryTotals.clear()

        membersList.forEach { member ->
            if (member.id in presentMemberIds) {
                record.markPresent(member.id, member.category)
            }
        }

        // Update state
        _attendanceRecords.value = existingRecords.sortedBy { it.date }
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
}
