package com.attendancetracker.data.logging

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utility class for exporting app logs.
 *
 * Provides functionality to share the log file via Android's share sheet,
 * allowing users to send logs to Claude for debugging.
 */
object LogExporter {

    /**
     * Shares the app log file using Android's share sheet.
     *
     * Users can choose to:
     * - Copy the log content
     * - Send via email
     * - Share to a notes app
     * - Any other share target
     *
     * @param context The context to use for sharing
     */
    fun shareLogFile(context: Context) {
        try {
            val logFile = File(context.filesDir, FileLoggingTree.LOG_FILE_NAME)

            if (!logFile.exists() || logFile.length() == 0L) {
                Toast.makeText(context, "No logs to export", Toast.LENGTH_SHORT).show()
                return
            }

            // Use FileProvider to share the file securely
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Attendance Tracker Debug Logs")
                putExtra(Intent.EXTRA_TEXT, "Debug logs from Attendance Tracker app")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Export Logs")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export logs: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Gets the log file content as a string.
     *
     * Useful for displaying in-app or copying to clipboard.
     *
     * @param context The context to use
     * @return The log file content, or an error message if reading fails
     */
    fun getLogContent(context: Context): String {
        return try {
            val logFile = File(context.filesDir, FileLoggingTree.LOG_FILE_NAME)
            if (logFile.exists()) {
                logFile.readText()
            } else {
                "No logs available"
            }
        } catch (e: Exception) {
            "Failed to read logs: ${e.message}"
        }
    }

    /**
     * Clears all logs.
     *
     * @param context The context to use
     */
    fun clearLogs(context: Context) {
        try {
            val logFile = File(context.filesDir, FileLoggingTree.LOG_FILE_NAME)
            if (logFile.exists()) {
                logFile.writeText("--- Logs cleared ---\n")
            }
            Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to clear logs: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Gets the size of the log file in a human-readable format.
     *
     * @param context The context to use
     * @return The file size as a string (e.g., "45 KB")
     */
    fun getLogFileSize(context: Context): String {
        return try {
            val logFile = File(context.filesDir, FileLoggingTree.LOG_FILE_NAME)
            if (logFile.exists()) {
                val bytes = logFile.length()
                when {
                    bytes < 1024 -> "$bytes B"
                    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                    else -> "${bytes / (1024 * 1024)} MB"
                }
            } else {
                "0 B"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
