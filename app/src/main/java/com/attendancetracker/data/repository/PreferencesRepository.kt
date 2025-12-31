package com.attendancetracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.attendancetracker.data.models.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Repository for managing app preferences using DataStore.
 *
 * Provides a clean API for reading and writing app settings.
 */
class PreferencesRepository(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    companion object {
        /**
         * Default spreadsheet ID for the app owner (Dwell CC).
         * Other users will need to configure their own spreadsheet ID.
         */
        const val DEFAULT_SPREADSHEET_ID = "1HD2ybg4ko2L9DpILk5Gi12iSH_IDRccT6TR4Job6oDM"

        /**
         * Email address of the app owner. Used for first-run detection.
         * If the signed-in user matches this email, the default spreadsheet is used automatically.
         */
        const val OWNER_EMAIL = "zanee40@gmail.com"

        val SPREADSHEET_ID_KEY = stringPreferencesKey("spreadsheet_id")
        val MORNING_NOTIFICATION = booleanPreferencesKey("morning_notification")
        val EVENING_NOTIFICATION = booleanPreferencesKey("evening_notification")
        private val STATISTICS_SORT_KEY = stringPreferencesKey("statistics_sort")
        private val SKIPPED_DATES_KEY = stringSetPreferencesKey("skipped_dates")
        private val HIDE_INFREQUENT_KEY = booleanPreferencesKey("hide_infrequent_members")
        private val TUTORIAL_COMPLETED_KEY = booleanPreferencesKey("tutorial_completed")
    }

    /**
     * Flow of app settings that emits whenever settings change.
     */
    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            spreadsheetId = preferences[SPREADSHEET_ID_KEY] ?: "",
            morningNotificationEnabled = preferences[MORNING_NOTIFICATION] ?: true,
            eveningNotificationEnabled = preferences[EVENING_NOTIFICATION] ?: true
        )
    }

    /**
     * Updates the spreadsheet ID.
     */
    suspend fun updateSpreadsheetId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[SPREADSHEET_ID_KEY] = id
        }
    }

    /**
     * Updates morning notification preference.
     */
    suspend fun updateMorningNotification(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MORNING_NOTIFICATION] = enabled
        }
    }

    /**
     * Updates evening notification preference.
     */
    suspend fun updateEveningNotification(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EVENING_NOTIFICATION] = enabled
        }
    }

    /**
     * Gets the current spreadsheet ID from preferences.
     * Returns empty string if not set.
     */
    suspend fun getSpreadsheetId(): String {
        return context.dataStore.data.map { preferences ->
            preferences[SPREADSHEET_ID_KEY] ?: ""
        }.first()
    }

    /**
     * Gets the effective spreadsheet ID for a given user email.
     * - If the user is the owner, returns the default spreadsheet ID.
     * - If the user has a saved spreadsheet ID, returns that.
     * - Otherwise, returns empty string (user needs to set up).
     *
     * @param userEmail The signed-in user's email address
     * @return The spreadsheet ID to use, or empty string if setup needed
     */
    suspend fun getEffectiveSpreadsheetId(userEmail: String): String {
        // Owner always uses default spreadsheet
        if (userEmail.equals(OWNER_EMAIL, ignoreCase = true)) {
            return DEFAULT_SPREADSHEET_ID
        }
        // Other users use their saved spreadsheet ID
        return getSpreadsheetId()
    }

    /**
     * Checks if a user needs to complete setup (configure spreadsheet ID).
     *
     * @param userEmail The signed-in user's email address
     * @return true if setup is needed, false otherwise
     */
    suspend fun needsSetup(userEmail: String): Boolean {
        // Owner never needs setup
        if (userEmail.equals(OWNER_EMAIL, ignoreCase = true)) {
            return false
        }
        // Other users need setup if they don't have a spreadsheet ID configured
        return getSpreadsheetId().isBlank()
    }

    /**
     * Flow of statistics sort preference that emits whenever the preference changes.
     */
    val statisticsSortPreference: Flow<MemberStatisticsSortBy> = context.dataStore.data
        .map { preferences ->
            val sortName = preferences[STATISTICS_SORT_KEY] ?: MemberStatisticsSortBy.ATTENDANCE_HIGH.name
            try {
                MemberStatisticsSortBy.valueOf(sortName)
            } catch (e: IllegalArgumentException) {
                MemberStatisticsSortBy.ATTENDANCE_HIGH // Default fallback
            }
        }

    /**
     * Sets the statistics sort preference.
     */
    suspend fun setStatisticsSortPreference(sort: MemberStatisticsSortBy) {
        context.dataStore.edit { preferences ->
            preferences[STATISTICS_SORT_KEY] = sort.name
        }
    }

    /**
     * Flow of hide infrequent members preference.
     * When true, members with <40% attendance are hidden from the member list.
     */
    val hideInfrequentMembersFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HIDE_INFREQUENT_KEY] ?: false
        }

    /**
     * Sets the hide infrequent members preference.
     *
     * @param hide Whether to hide infrequent members
     */
    suspend fun setHideInfrequentMembers(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_INFREQUENT_KEY] = hide
        }
    }

    /**
     * Flow of skipped dates (dates marked as "No Meeting").
     * Emits whenever the set of skipped dates changes.
     * Dates are stored as ISO-8601 strings (yyyy-MM-dd).
     */
    val skippedDatesFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[SKIPPED_DATES_KEY] ?: emptySet()
        }

    /**
     * Marks a date as skipped ("No Meeting").
     * The date will be excluded from all statistics calculations.
     *
     * @param date The date to mark as skipped
     */
    suspend fun addSkippedDate(date: LocalDate) {
        context.dataStore.edit { preferences ->
            val current = preferences[SKIPPED_DATES_KEY] ?: emptySet()
            preferences[SKIPPED_DATES_KEY] = current + date.toString()
        }
    }

    /**
     * Removes a date from the skipped list.
     * The date will be included in statistics calculations again.
     *
     * @param date The date to unskip
     */
    suspend fun removeSkippedDate(date: LocalDate) {
        context.dataStore.edit { preferences ->
            val current = preferences[SKIPPED_DATES_KEY] ?: emptySet()
            preferences[SKIPPED_DATES_KEY] = current - date.toString()
        }
    }

    /**
     * Checks if a specific date is marked as skipped.
     *
     * @param date The date to check
     * @return true if the date is skipped, false otherwise
     */
    suspend fun isDateSkipped(date: LocalDate): Boolean {
        return context.dataStore.data.first()[SKIPPED_DATES_KEY]?.contains(date.toString()) ?: false
    }

    /**
     * Clears all skipped dates.
     * Useful for reset functionality or troubleshooting.
     */
    suspend fun clearSkippedDates() {
        context.dataStore.edit { preferences ->
            preferences.remove(SKIPPED_DATES_KEY)
        }
    }

    // ============ Tutorial Preferences ============

    /**
     * Flow of tutorial completion status.
     * Emits true if the user has completed the tutorial.
     */
    val tutorialCompletedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[TUTORIAL_COMPLETED_KEY] ?: false
        }

    /**
     * Checks if the tutorial has been completed.
     *
     * @return true if tutorial was completed, false otherwise
     */
    suspend fun hasTutorialBeenCompleted(): Boolean {
        return context.dataStore.data.first()[TUTORIAL_COMPLETED_KEY] ?: false
    }

    /**
     * Marks the tutorial as completed or not completed.
     *
     * @param completed Whether the tutorial has been completed
     */
    suspend fun setTutorialCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TUTORIAL_COMPLETED_KEY] = completed
        }
    }
}
