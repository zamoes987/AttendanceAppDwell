package com.attendancetracker.data.logging

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom Timber tree that writes logs to a local file.
 *
 * Features:
 * - Logs are timestamped with level and tag
 * - File is kept under 1MB (older logs trimmed automatically)
 * - File can be exported for debugging with Claude
 *
 * Log file location: {app_internal_storage}/app_logs.txt
 */
class FileLoggingTree(context: Context) : Timber.Tree() {

    private val logFile: File = File(context.filesDir, LOG_FILE_NAME)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    companion object {
        const val LOG_FILE_NAME = "app_logs.txt"
        private const val MAX_FILE_SIZE = 1_000_000L // 1MB
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val timestamp = dateFormat.format(Date())
            val level = priorityToString(priority)
            val logTag = tag ?: "App"

            // Format: "2025-01-15 14:30:45.123 D/MainActivity: Sign-in successful"
            val logLine = "$timestamp $level/$logTag: $message\n"

            // Append to file
            logFile.appendText(logLine)

            // If there's a throwable, append the stack trace
            t?.let { throwable ->
                logFile.appendText("${throwable.stackTraceToString()}\n")
            }

            // Trim file if too large
            if (logFile.length() > MAX_FILE_SIZE) {
                trimLogFile()
            }
        } catch (e: Exception) {
            // Don't crash if logging fails - just print to logcat
            Log.e("FileLoggingTree", "Failed to write log to file", e)
        }
    }

    /**
     * Converts Android log priority to a single character.
     */
    private fun priorityToString(priority: Int): String = when (priority) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> "?"
    }

    /**
     * Trims the log file by keeping only the last half.
     * This prevents the file from growing indefinitely.
     */
    private fun trimLogFile() {
        try {
            val lines = logFile.readLines()
            val halfIndex = lines.size / 2
            val trimmedLines = lines.subList(halfIndex, lines.size)

            // Add marker indicating logs were trimmed
            val header = "--- Log file trimmed at ${dateFormat.format(Date())} ---\n"
            logFile.writeText(header)
            logFile.appendText(trimmedLines.joinToString("\n"))
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Failed to trim log file", e)
        }
    }

    /**
     * Gets the log file for export.
     */
    fun getLogFile(): File = logFile

    /**
     * Clears the log file.
     */
    fun clearLogs() {
        try {
            logFile.writeText("--- Log cleared at ${dateFormat.format(Date())} ---\n")
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Failed to clear log file", e)
        }
    }
}
