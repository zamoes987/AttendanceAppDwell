package com.attendancetracker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendancetracker.data.models.AppSettings
import com.attendancetracker.data.notifications.NotificationHelper
import com.attendancetracker.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 *
 * Manages app settings and preferences.
 *
 * @property preferencesRepository Repository for managing preferences
 * @property notificationHelper Helper for scheduling notifications
 */
class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    /**
     * Current app settings.
     */
    val settings: StateFlow<AppSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    /**
     * Updates the Google Spreadsheet ID.
     */
    fun updateSpreadsheetId(id: String) {
        viewModelScope.launch {
            preferencesRepository.updateSpreadsheetId(id)
        }
    }

    /**
     * Toggles morning notification setting.
     */
    fun toggleMorningNotification(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateMorningNotification(enabled)

            // Schedule or cancel the notification based on the setting
            if (enabled) {
                notificationHelper.scheduleMorningNotification()
            } else {
                notificationHelper.cancelMorningNotification()
            }
        }
    }

    /**
     * Toggles evening notification setting.
     */
    fun toggleEveningNotification(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateEveningNotification(enabled)

            // Schedule or cancel the notification based on the setting
            if (enabled) {
                notificationHelper.scheduleEveningNotification()
            } else {
                notificationHelper.cancelEveningNotification()
            }
        }
    }

    /**
     * Shows a test notification immediately for testing purposes.
     */
    fun showTestNotification() {
        notificationHelper.showTestNotification()
    }

    /**
     * Checks if the app can schedule exact alarms (required for Android 12+).
     */
    fun canScheduleExactAlarms(): Boolean {
        return notificationHelper.canScheduleExactAlarms()
    }

    /**
     * Requests permission to schedule exact alarms.
     */
    fun requestExactAlarmPermission() {
        notificationHelper.requestExactAlarmPermission()
    }
}
