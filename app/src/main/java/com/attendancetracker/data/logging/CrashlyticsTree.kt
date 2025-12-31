package com.attendancetracker.data.logging

import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

/**
 * Custom Timber tree that sends logs to Firebase Crashlytics.
 *
 * - WARN and ERROR level logs are sent to Crashlytics
 * - Exceptions are recorded as non-fatal crashes
 * - Lower priority logs are ignored to reduce noise
 */
class CrashlyticsTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Only send warnings and errors to Crashlytics
        if (priority < Log.WARN) return

        // Log the message to Crashlytics breadcrumbs
        Firebase.crashlytics.log("${tag ?: "App"}: $message")

        // Record exception if present
        t?.let { throwable ->
            Firebase.crashlytics.recordException(throwable)
        }
    }
}
