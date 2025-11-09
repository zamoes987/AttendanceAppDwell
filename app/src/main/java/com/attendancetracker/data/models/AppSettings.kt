package com.attendancetracker.data.models

/**
 * Data class representing app settings.
 *
 * @property spreadsheetId The Google Sheets ID
 * @property morningNotificationEnabled Whether morning notifications are enabled
 * @property eveningNotificationEnabled Whether evening notifications are enabled
 */
data class AppSettings(
    val spreadsheetId: String = "",
    val morningNotificationEnabled: Boolean = true,
    val eveningNotificationEnabled: Boolean = true
)
