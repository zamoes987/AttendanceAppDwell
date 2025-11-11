package com.attendancetracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.attendancetracker.data.models.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository for managing app preferences using DataStore.
 *
 * Provides a clean API for reading and writing app settings.
 */
class PreferencesRepository(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    companion object {
        val SPREADSHEET_ID = stringPreferencesKey("spreadsheet_id")
        val MORNING_NOTIFICATION = booleanPreferencesKey("morning_notification")
        val EVENING_NOTIFICATION = booleanPreferencesKey("evening_notification")
        private val STATISTICS_SORT_KEY = stringPreferencesKey("statistics_sort")
    }

    /**
     * Flow of app settings that emits whenever settings change.
     */
    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            spreadsheetId = preferences[SPREADSHEET_ID] ?: "",
            morningNotificationEnabled = preferences[MORNING_NOTIFICATION] ?: true,
            eveningNotificationEnabled = preferences[EVENING_NOTIFICATION] ?: true
        )
    }

    /**
     * Updates the spreadsheet ID.
     */
    suspend fun updateSpreadsheetId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[SPREADSHEET_ID] = id
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
     * Gets the current spreadsheet ID.
     * FIXED: Use first() instead of broken collect {} pattern.
     */
    suspend fun getSpreadsheetId(): String {
        return context.dataStore.data.map { preferences ->
            preferences[SPREADSHEET_ID] ?: ""
        }.first()
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
}
