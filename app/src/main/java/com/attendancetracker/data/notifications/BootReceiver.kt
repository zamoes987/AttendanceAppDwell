package com.attendancetracker.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.attendancetracker.BuildConfig
import com.attendancetracker.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that restores scheduled notifications after device reboot.
 *
 * AlarmManager alarms are cleared when the device reboots, so we need to
 * re-schedule them based on user preferences.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        if (BuildConfig.DEBUG) {
            Log.d("BootReceiver", "Device booted, restoring notification alarms")
        }

        // Restore alarms based on saved preferences
        // This runs on a background thread to avoid blocking the boot process
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferencesRepository = PreferencesRepository(context)
                val notificationHelper = NotificationHelper(context, preferencesRepository)
                notificationHelper.restoreAlarmsFromPreferences()

                if (BuildConfig.DEBUG) {
                    Log.d("BootReceiver", "Notification alarms restored successfully")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error restoring notification alarms", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
