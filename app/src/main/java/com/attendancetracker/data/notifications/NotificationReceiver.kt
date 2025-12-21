package com.attendancetracker.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.attendancetracker.MainActivity
import com.attendancetracker.R

/**
 * BroadcastReceiver that handles displaying notifications when alarms trigger.
 *
 * This receiver is invoked by AlarmManager at scheduled times to show
 * attendance reminder notifications.
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val TYPE_MORNING = "morning"
        const val TYPE_EVENING = "evening"
        private const val CHANNEL_ID = "attendance_reminders"
        const val MORNING_NOTIFICATION_ID = 1001
        const val EVENING_NOTIFICATION_ID = 1002
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationType = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE) ?: return

        // Create notification channel (safe to call repeatedly)
        createNotificationChannel(context)

        // Show appropriate notification
        when (notificationType) {
            TYPE_MORNING -> showMorningNotification(context)
            TYPE_EVENING -> showEveningNotification(context)
        }
    }

    /**
     * Creates the notification channel for attendance reminders.
     * Required for Android 8.0+ (API 26+).
     */
    private fun createNotificationChannel(context: Context) {
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
        }
    }

    /**
     * Shows the morning reminder notification.
     */
    private fun showMorningNotification(context: Context) {
        val notification = buildNotification(
            context = context,
            title = "Attendance Reminder",
            message = "Don't forget to count members for tonight's meeting!",
            notificationId = MORNING_NOTIFICATION_ID
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(MORNING_NOTIFICATION_ID, notification)
    }

    /**
     * Shows the evening reminder notification.
     */
    private fun showEveningNotification(context: Context) {
        val notification = buildNotification(
            context = context,
            title = "Time to Mark Attendance!",
            message = "The meeting is starting. Make sure to count members in the app.",
            notificationId = EVENING_NOTIFICATION_ID
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(EVENING_NOTIFICATION_ID, notification)
    }

    /**
     * Builds a notification with action buttons.
     */
    private fun buildNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
    ): android.app.Notification {
        // Intent to open the app when notification is tapped
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for "Remind Later" action (dismisses notification)
        val remindLaterIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "REMIND_LATER"
        }
        val remindLaterPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100,
            remindLaterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Using default launcher icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                0,
                "Open App",
                openAppPendingIntent
            )
            .addAction(
                0,
                "Remind Later",
                remindLaterPendingIntent
            )
            .build()
    }
}
