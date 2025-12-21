package com.attendancetracker.data.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.attendancetracker.BuildConfig
import com.attendancetracker.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Helper class for managing attendance reminder notifications.
 *
 * Handles:
 * - Scheduling recurring weekly notifications for Thursdays
 * - Canceling scheduled notifications
 * - Requesting necessary permissions
 * - Restoring alarms after device reboot
 * - Test mode for immediate notifications
 */
class NotificationHelper(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val MORNING_REQUEST_CODE = 1001
        private const val EVENING_REQUEST_CODE = 1002
        private const val CHANNEL_ID = "attendance_reminders"

        // Meeting schedule: Thursdays
        private const val MEETING_DAY_OF_WEEK = Calendar.THURSDAY
        private const val MORNING_HOUR = 8  // 8:00 AM
        private const val MORNING_MINUTE = 0
        private const val EVENING_HOUR = 19 // 7:00 PM
        private const val EVENING_MINUTE = 0
    }

    /**
     * Creates the notification channel for attendance reminders.
     * Must be called before showing any notifications (Android 8.0+).
     * Safe to call multiple times.
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Attendance Reminders"
            val descriptionText = "Reminders to mark attendance for meetings"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            if (BuildConfig.DEBUG) {
                Log.d("NotificationHelper", "Notification channel created")
            }
        }
    }

    /**
     * Schedules the morning reminder notification for Thursdays at 8:00 AM.
     */
    fun scheduleMorningNotification() {
        try {
            if (!canScheduleExactAlarms()) {
                Log.w("NotificationHelper", "Cannot schedule exact alarms - permission denied")
                return
            }

            val calendar = getNextThursdayAt(MORNING_HOUR, MORNING_MINUTE)
            val intent = createNotificationIntent(NotificationReceiver.TYPE_MORNING)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                MORNING_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Schedule repeating alarm for every Thursday at 8:00 AM
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7, // Repeat weekly
                pendingIntent
            )

            if (BuildConfig.DEBUG) {
                Log.d("NotificationHelper", "Morning notification scheduled for: ${calendar.time}")
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Security exception scheduling morning notification", e)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error scheduling morning notification", e)
        }
    }

    /**
     * Schedules the evening reminder notification for Thursdays at 7:00 PM.
     */
    fun scheduleEveningNotification() {
        try {
            if (!canScheduleExactAlarms()) {
                Log.w("NotificationHelper", "Cannot schedule exact alarms - permission denied")
                return
            }

            val calendar = getNextThursdayAt(EVENING_HOUR, EVENING_MINUTE)
            val intent = createNotificationIntent(NotificationReceiver.TYPE_EVENING)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                EVENING_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Schedule repeating alarm for every Thursday at 7:00 PM
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7, // Repeat weekly
                pendingIntent
            )

            if (BuildConfig.DEBUG) {
                Log.d("NotificationHelper", "Evening notification scheduled for: ${calendar.time}")
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Security exception scheduling evening notification", e)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error scheduling evening notification", e)
        }
    }

    /**
     * Cancels the morning reminder notification.
     */
    fun cancelMorningNotification() {
        val intent = createNotificationIntent(NotificationReceiver.TYPE_MORNING)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        if (BuildConfig.DEBUG) {
            Log.d("NotificationHelper", "Morning notification canceled")
        }
    }

    /**
     * Cancels the evening reminder notification.
     */
    fun cancelEveningNotification() {
        val intent = createNotificationIntent(NotificationReceiver.TYPE_EVENING)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            EVENING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        if (BuildConfig.DEBUG) {
            Log.d("NotificationHelper", "Evening notification canceled")
        }
    }

    /**
     * Shows a test notification immediately (for testing purposes).
     * This bypasses the alarm schedule and triggers a notification right away.
     *
     * @param type The notification type (morning or evening)
     */
    fun showTestNotification(type: String = NotificationReceiver.TYPE_MORNING) {
        try {
            // Check exact alarm permission first
            if (!canScheduleExactAlarms()) {
                Log.w("NotificationHelper", "Cannot show test notification - exact alarm permission denied")
                return
            }

            // Ensure notification channel exists
            createNotificationChannel()

            val intent = createNotificationIntent(type)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9999, // Special request code for test
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Schedule for 3 seconds from now to test
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 3000,
                pendingIntent
            )

            if (BuildConfig.DEBUG) {
                Log.d("NotificationHelper", "Test notification scheduled for 3 seconds from now")
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Security exception showing test notification", e)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error showing test notification", e)
        }
    }

    /**
     * Restores scheduled alarms based on saved user preferences.
     * Called after device reboot or app update.
     */
    suspend fun restoreAlarmsFromPreferences() {
        val settings = preferencesRepository.settingsFlow.first()

        if (settings.morningNotificationEnabled) {
            scheduleMorningNotification()
        }

        if (settings.eveningNotificationEnabled) {
            scheduleEveningNotification()
        }
    }

    /**
     * Checks if the app can schedule exact alarms.
     * Required for Android 12+ (API 31+).
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // No restriction on older Android versions
        }
    }

    /**
     * Opens the system settings to allow exact alarm scheduling.
     * Required for Android 12+ if permission is not granted.
     */
    fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    /**
     * Calculates the next Thursday at the specified time.
     * If today is Thursday and the time hasn't passed, returns today.
     * Otherwise, returns next Thursday.
     */
    private fun getNextThursdayAt(hour: Int, minute: Int): Calendar {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // If it's already Thursday and the time has passed, move to next Thursday
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (currentDayOfWeek == MEETING_DAY_OF_WEEK && calendar.timeInMillis <= System.currentTimeMillis()) {
            // Time has passed today, schedule for next week
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        } else if (currentDayOfWeek != MEETING_DAY_OF_WEEK) {
            // Not Thursday, find next Thursday
            val daysUntilThursday = (MEETING_DAY_OF_WEEK - currentDayOfWeek + 7) % 7
            if (daysUntilThursday == 0) {
                // Today is Thursday but time hasn't passed yet
                // Keep current date
            } else {
                calendar.add(Calendar.DAY_OF_YEAR, daysUntilThursday)
            }
        }

        return calendar
    }

    /**
     * Creates the intent for triggering a notification.
     */
    private fun createNotificationIntent(type: String): Intent {
        return Intent(context, NotificationReceiver::class.java).apply {
            action = "com.attendancetracker.SHOW_NOTIFICATION"
            putExtra(NotificationReceiver.EXTRA_NOTIFICATION_TYPE, type)
        }
    }
}
