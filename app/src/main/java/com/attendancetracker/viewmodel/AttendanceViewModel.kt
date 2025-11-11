package com.attendancetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendancetracker.data.models.AttendanceRecord
import com.attendancetracker.data.models.Category
import com.attendancetracker.data.models.CategoryStatistics
import com.attendancetracker.data.models.Member
import com.attendancetracker.data.models.MemberStatistics
import com.attendancetracker.data.models.OverallStatistics
import com.attendancetracker.data.models.TrendAnalysis
import com.attendancetracker.data.models.groupByCategory
import com.attendancetracker.data.repository.MemberStatisticsSortBy
import com.attendancetracker.data.repository.PreferencesRepository
import com.attendancetracker.data.repository.SheetsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/**
 * ViewModel for the Attendance Tracker app.
 *
 * Connects the UI layer to the repository and manages UI state.
 * Handles user interactions for marking attendance and manages the
 * currently selected members for the meeting.
 *
 * @property repository The data repository for accessing attendance data
 * @property preferencesRepository The preferences repository for persisting user settings
 */
class AttendanceViewModel(
    private val repository: SheetsRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    /**
     * Data class representing the complete UI state.
     *
     * Combines multiple state flows into a single state object
     * for easier consumption by the UI.
     */
    data class UiState(
        val membersByCategory: Map<Category, List<Member>> = emptyMap(),
        val selectedCount: Int = 0,
        val totalMembers: Int = 0,
        val todayDateString: String = "",
        val canSave: Boolean = false
    )

    // Expose repository state flows
    val members: StateFlow<List<Member>> = repository.members
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val attendanceRecords: StateFlow<List<AttendanceRecord>> = repository.attendanceRecords
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isLoading: StateFlow<Boolean> = repository.isLoading
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val error: StateFlow<String?> = repository.error
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Internal mutable state
    private val _selectedMembers = MutableStateFlow<Set<String>>(emptySet())
    val selectedMembers: StateFlow<Set<String>> = _selectedMembers.asStateFlow()

    private val _currentDate = MutableStateFlow(repository.getCurrentThursday())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    private val _showSaveSuccess = MutableStateFlow(false)
    val showSaveSuccess: StateFlow<Boolean> = _showSaveSuccess.asStateFlow()

    private val _memberOperationMessage = MutableStateFlow<String?>(null)
    val memberOperationMessage: StateFlow<String?> = _memberOperationMessage.asStateFlow()

    // Statistics state flows
    private val _overallStatistics = MutableStateFlow<OverallStatistics?>(null)
    val overallStatistics: StateFlow<OverallStatistics?> = _overallStatistics.asStateFlow()

    private val _memberStatistics = MutableStateFlow<List<MemberStatistics>>(emptyList())
    val memberStatistics: StateFlow<List<MemberStatistics>> = _memberStatistics.asStateFlow()

    private val _categoryStatistics = MutableStateFlow<List<CategoryStatistics>>(emptyList())
    val categoryStatistics: StateFlow<List<CategoryStatistics>> = _categoryStatistics.asStateFlow()

    private val _trendAnalysis = MutableStateFlow<TrendAnalysis?>(null)
    val trendAnalysis: StateFlow<TrendAnalysis?> = _trendAnalysis.asStateFlow()

    private val _statisticsLoading = MutableStateFlow(false)
    val statisticsLoading: StateFlow<Boolean> = _statisticsLoading.asStateFlow()

    private val _memberStatisticsSortBy = MutableStateFlow(MemberStatisticsSortBy.ATTENDANCE_HIGH)
    val memberStatisticsSortBy: StateFlow<MemberStatisticsSortBy> = _memberStatisticsSortBy.asStateFlow()

    // Job reference for cancelling previous statistics calculations
    private var statisticsJob: Job? = null

    /**
     * Combined UI state derived from multiple state flows.
     *
     * Automatically updates when any of the source flows change.
     *
     * PERFORMANCE FIX: Grouping by category is now done only when members list changes,
     * not on every selection/date change. This prevents expensive groupBy operations
     * on every recomposition with 75+ members.
     */
    val uiState: StateFlow<UiState> = combine(
        members.map { it.groupByCategory() }, // Group once when members change
        _selectedMembers,
        _currentDate
    ) { membersByCategory, selected, date ->
        UiState(
            membersByCategory = membersByCategory,
            selectedCount = selected.size,
            totalMembers = membersByCategory.values.sumOf { it.size },
            todayDateString = AttendanceRecord.formatDateForSheet(date),
            canSave = selected.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = UiState()
    )

    init {
        // Load saved sort preference
        viewModelScope.launch {
            preferencesRepository.statisticsSortPreference.first().let { savedSort ->
                _memberStatisticsSortBy.value = savedSort
            }
        }

        // Load data when ViewModel is created
        loadData()
    }

    /**
     * Loads all data from Google Sheets.
     *
     * Fetches members and attendance history, then pre-populates
     * the selected members with those already marked present for today.
     */
    fun loadData() {
        viewModelScope.launch {
            try {
                val result = repository.loadAllData()

                if (result.isSuccess) {
                    // Wait for StateFlows to actually update with data
                    // This ensures the UI doesn't try to populate before data is ready
                    withTimeoutOrNull(10000) { // 10 second timeout
                        members.first { it.isNotEmpty() }
                    }
                    // FIXED: Add timeout to prevent hanging forever
                    withTimeoutOrNull(10000) { // 10 second timeout
                        attendanceRecords.first()
                    }

                    // Pre-populate selected members with today's attendance
                    loadTodayAttendance()
                } else {
                    // Error already handled by repository
                    android.util.Log.e("AttendanceViewModel", "Failed to load data: ${result.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("AttendanceViewModel", "Error loading data", e)
            }
        }
    }

    /**
     * Loads today's attendance and pre-selects members who are already marked present.
     */
    private fun loadTodayAttendance() {
        loadAttendanceForDate(_currentDate.value)
    }

    /**
     * Toggles the attendance status for a specific member.
     *
     * If the member is currently selected, removes them from the selection.
     * If not selected, adds them to the selection.
     *
     * @param memberId The unique identifier of the member to toggle
     */
    fun toggleMemberSelection(memberId: String) {
        _selectedMembers.value = if (memberId in _selectedMembers.value) {
            _selectedMembers.value - memberId
        } else {
            _selectedMembers.value + memberId
        }
    }

    /**
     * Selects all members for attendance.
     */
    fun selectAll() {
        _selectedMembers.value = members.value.map { it.id }.toSet()
    }

    /**
     * Clears all member selections.
     */
    fun clearAll() {
        _selectedMembers.value = emptySet()
    }

    /**
     * Selects all members of a specific category.
     *
     * @param category The category to select all members from
     */
    fun selectCategory(category: Category) {
        val categoryMemberIds = members.value
            .filter { it.category == category }
            .map { it.id }

        _selectedMembers.value = _selectedMembers.value + categoryMemberIds
    }

    /**
     * Saves the current attendance to Google Sheets.
     *
     * Writes attendance for the selected date and shows
     * a success message on completion.
     */
    fun saveAttendance() {
        viewModelScope.launch {
            val result = repository.saveAttendance(_selectedMembers.value, _currentDate.value)

            if (result.isSuccess) {
                _showSaveSuccess.value = true
            }
        }
    }

    /**
     * Dismisses the success message.
     */
    fun dismissSuccessMessage() {
        _showSaveSuccess.value = false
    }

    /**
     * Clears the current error state.
     */
    fun clearError() {
        repository.clearError()
    }

    /**
     * Refreshes all data from Google Sheets.
     */
    fun refreshData() {
        viewModelScope.launch {
            repository.loadAllData()
        }
    }

    /**
     * Adds a new member to the system.
     *
     * @param name The name of the new member
     * @param category The category for the new member
     */
    fun addMember(name: String, category: Category) {
        viewModelScope.launch {
            val result = repository.addMember(name, category)
            _memberOperationMessage.value = if (result.isSuccess) {
                "$name added successfully!"
            } else {
                "Failed to add member: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    /**
     * Updates an existing member's information.
     *
     * @param memberId The ID of the member to update
     * @param name The new name
     * @param category The new category
     */
    fun updateMember(memberId: String, name: String, category: Category) {
        viewModelScope.launch {
            val result = repository.updateMember(memberId, name, category)
            _memberOperationMessage.value = if (result.isSuccess) {
                "$name updated successfully!"
            } else {
                "Failed to update member: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    /**
     * Deletes a member from the system.
     *
     * @param memberId The ID of the member to delete
     */
    fun deleteMember(memberId: String) {
        viewModelScope.launch {
            val result = repository.deleteMember(memberId)
            _memberOperationMessage.value = if (result.isSuccess) {
                "Member deleted successfully!"
            } else {
                "Failed to delete member: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    /**
     * Clears the member operation message.
     */
    fun clearMemberOperationMessage() {
        _memberOperationMessage.value = null
    }

    /**
     * Changes the selected date and loads attendance for that date.
     *
     * @param date The new date to select
     */
    fun setSelectedDate(date: LocalDate) {
        _currentDate.value = date
        loadAttendanceForDate(date)
    }

    /**
     * Loads attendance for a specific date and pre-selects members.
     *
     * Smart auto-select: If no attendance exists for the date, pre-selects members
     * who were present exactly 1 week ago. This saves leaders ~60 clicks per week
     * by automatically selecting consistent attendees.
     *
     * @param date The date to load attendance for
     */
    private fun loadAttendanceForDate(date: LocalDate) {
        viewModelScope.launch {
            // Wait for the latest attendance records from the StateFlow
            val records = attendanceRecords.value

            // Find record by matching the actual date, not the string
            // (handles different date string formats like "1/9/25" vs "01/09/25")
            val todayRecord = records.find { it.date == date }

            if (todayRecord != null) {
                // Found existing attendance for this date - pre-select those members
                _selectedMembers.value = todayRecord.presentMembers.toSet()
            } else {
                // Smart pre-select: use last week's attendance (1 week = 7 days)
                val lastWeek = date.minusWeeks(1)
                val lastWeekRecord = records.find { it.date == lastWeek }
                _selectedMembers.value = lastWeekRecord?.presentMembers?.toSet() ?: emptySet()
            }
        }
    }

    /**
     * Calculates and updates all statistics.
     *
     * Should be called when navigating to statistics screen or after data refresh.
     */
    fun calculateStatistics() {
        viewModelScope.launch {
            _statisticsLoading.value = true

            try {
                // Wait for data to be loaded (with timeout)
                withTimeoutOrNull(5000) {
                    attendanceRecords.first { it.isNotEmpty() }
                }

                // Calculate on background thread
                withContext(Dispatchers.Default) {
                    val overall = repository.calculateOverallStatistics()
                    val member = repository.calculateMemberStatistics(_memberStatisticsSortBy.value)
                    val category = repository.calculateCategoryStatistics()
                    val trend = repository.calculateAttendanceTrend(meetingsCount = 10)

                    // Update UI on main thread
                    withContext(Dispatchers.Main) {
                        _overallStatistics.value = overall
                        _memberStatistics.value = member
                        _categoryStatistics.value = category
                        _trendAnalysis.value = trend
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("AttendanceViewModel", "Error calculating statistics", e)
                // Statistics remain null/empty - UI will show empty state
            } finally {
                _statisticsLoading.value = false
            }
        }
    }

    /**
     * Changes the sort order for member statistics.
     * Persists the preference to DataStore for future sessions.
     */
    fun setMemberStatisticsSort(sortBy: MemberStatisticsSortBy) {
        _memberStatisticsSortBy.value = sortBy
        statisticsJob?.cancel()
        statisticsJob = viewModelScope.launch {
            // Save preference
            preferencesRepository.setStatisticsSortPreference(sortBy)

            _memberStatistics.value = repository.calculateMemberStatistics(sortBy)
        }
    }

    /**
     * Refreshes statistics (recalculates from current data).
     */
    fun refreshStatistics() {
        calculateStatistics()
    }
}
