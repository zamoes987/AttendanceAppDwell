package com.attendancetracker

import android.app.Application
import com.attendancetracker.data.logging.CrashlyticsTree
import com.attendancetracker.data.logging.FileLoggingTree
import timber.log.Timber

/**
 * Application class for Attendance Tracker.
 *
 * Initializes logging infrastructure (Timber) on app startup.
 * - Debug builds: DebugTree (logcat) + FileLoggingTree (local file)
 * - Release builds: CrashlyticsTree (Firebase) + FileLoggingTree (local file)
 */
class AttendanceTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            // Debug: Log to logcat with line numbers
            Timber.plant(Timber.DebugTree())
            // Also log to file for debugging
            Timber.plant(FileLoggingTree(this))
        } else {
            // Release: Send errors to Crashlytics
            Timber.plant(CrashlyticsTree())
            // Also log to file for user export
            Timber.plant(FileLoggingTree(this))
        }

        Timber.d("AttendanceTrackerApp initialized")
    }
}
