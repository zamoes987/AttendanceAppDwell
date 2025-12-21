package com.attendancetracker.data.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.attendancetracker.BuildConfig
import com.attendancetracker.data.models.AttendanceRecord
import com.attendancetracker.data.models.Category
import com.attendancetracker.data.models.Member
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.InsertDimensionRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Service class that handles all Google Sheets API interactions.
 *
 * This class manages authentication, reading member data, reading/writing attendance records,
 * and all other operations with the Google Sheets backend.
 *
 * SETUP REQUIREMENTS:
 * 1. Google Cloud Project with Sheets API enabled
 * 2. OAuth 2.0 credentials configured for Android
 * 3. User must sign in with Google account that has access to the spreadsheet
 * 4. SPREADSHEET_ID must be configured to your actual Google Sheet ID
 *
 * @property context Application context for credential management
 * @property accountName Google account email address for authentication
 */
class GoogleSheetsService(
    private val context: Context,
    private val accountName: String
) {
    /**
     * The ID of the Google Spreadsheet to read/write from.
     *
     * IMPORTANT: Replace this with your actual spreadsheet ID!
     * You can find this in the URL of your Google Sheet:
     * https://docs.google.com/spreadsheets/d/YOUR_SPREADSHEET_ID_HERE/edit
     *
     * TODO: For production use, consider loading this from:
     * - BuildConfig fields (set in build.gradle)
     * - A configuration file
     * - User settings (already supported via PreferencesRepository)
     */
    private val SPREADSHEET_ID = "YOUR_SPREADSHEET_ID_HERE"

    /**
     * The name of the tab/sheet to use for the current year.
     */
    private val currentYearTab = "2025"

    /**
     * Google account credential initialized with Sheets API scope.
     * Lazy-loaded when first accessed.
     */
    private val credential: GoogleAccountCredential by lazy {
        GoogleAccountCredential.usingOAuth2(
            context,
            listOf(SheetsScopes.SPREADSHEETS)
        ).apply {
            selectedAccountName = accountName
        }
    }

    /**
     * Google Sheets API service client.
     * Lazy-loaded when first accessed.
     *
     * ISSUE #3 FIX: Configured with 30-second timeouts to prevent indefinite hangs
     */
    private val sheetsService: Sheets by lazy {
        val transport = NetHttpTransport()

        val requestInitializer = HttpRequestInitializer { request ->
            credential.initialize(request)
            request.connectTimeout = 30_000 // 30 seconds
            request.readTimeout = 30_000    // 30 seconds
        }

        Sheets.Builder(
            transport,
            GsonFactory.getDefaultInstance(),
            requestInitializer
        )
            .setApplicationName("Attendance Tracker")
            .build()
    }

    /**
     * Checks if network connectivity is available.
     *
     * ISSUE #2 FIX: Network availability check to prevent cryptic offline errors
     *
     * @return true if internet connection is available, false otherwise
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    /**
     * Reads the member list from the Google Sheet.
     *
     * Reads columns B (Name) and C (Status/Category) from the current year tab.
     * Skips the header row and any total rows (rows starting with category abbreviations).
     *
     * @return Result containing list of Member objects, or failure with exception
     */
    suspend fun readMembers(): Result<List<Member>> = withContext(Dispatchers.IO) {
        // ISSUE #2 FIX: Check network connectivity before API call
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        try {
            val range = "$currentYearTab!B:C"
            val response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute()

            val values = response.getValues() ?: return@withContext Result.failure(
                Exception("No data found in sheet")
            )

            val members = mutableListOf<Member>()

            // Skip header row (index 0), start from row 1
            for (rowIndex in 1 until values.size) {
                val row = values[rowIndex]

                // Skip empty rows
                if (row.isEmpty()) continue

                val name = row.getOrNull(0)?.toString()?.trim() ?: continue
                val categoryAbbr = row.getOrNull(1)?.toString()?.trim() ?: continue

                // Skip total rows (rows that start with category abbreviations)
                val isTotalRow = Category.entries.any {
                    it.abbreviation.equals(categoryAbbr, ignoreCase = true) &&
                    name.equals(categoryAbbr, ignoreCase = true)
                }
                if (isTotalRow) continue

                // Parse category
                val category = Category.fromAbbreviation(categoryAbbr) ?: continue

                // Create member with 1-indexed row number (row 0 is header, so rowIndex + 1)
                // Note: Using mutableMapOf() for attendanceHistory to allow mutations during data loading
                val member = Member(
                    id = "${currentYearTab}_${rowIndex + 1}",
                    name = name,
                    category = category,
                    rowIndex = rowIndex + 1, // Google Sheets uses 1-based indexing
                    attendanceHistory = mutableMapOf() // Mutable for initial loading phase
                )

                members.add(member)
            }

            Result.success(members)
        } catch (e: UserRecoverableAuthIOException) {
            // ISSUE #1 FIX: Handle OAuth token expiration
            Result.failure(Exception("Authentication expired. Please sign in again."))
        } catch (e: GoogleJsonResponseException) {
            // TASK #1 FIX: Add sheet deletion detection
            if (e.statusCode == 404) {
                Result.failure(Exception("Sheet '$currentYearTab' not found. Please check spreadsheet configuration."))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads attendance data for a specific date.
     *
     * Finds the column for the given date, reads attendance marks for that column,
     * and creates an AttendanceRecord with the present members.
     *
     * NOTE: This method does NOT modify the member objects. Use readAllAttendance()
     * to get members with populated attendance history.
     *
     * @param dateString The date in sheet format (e.g., "11/06/25")
     * @param members The list of members to match attendance records against
     * @return Result containing AttendanceRecord or null if date not found, or failure with exception
     */
    suspend fun readAttendanceForDate(
        dateString: String,
        members: List<Member>
    ): Result<AttendanceRecord?> = withContext(Dispatchers.IO) {
        // ISSUE #2 FIX: Check network connectivity before API call
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        try {
            // First, read the header row to find the date column
            val headerRange = "$currentYearTab!1:1"
            val headerResponse = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, headerRange)
                .execute()

            val headerRow = headerResponse.getValues()?.firstOrNull()
                ?: return@withContext Result.success(null)

            // Find the column index for this date
            val columnIndex = headerRow.indexOfFirst { cell ->
                cell.toString().trim() == dateString
            }

            if (columnIndex == -1) {
                // Date column doesn't exist yet
                return@withContext Result.success(null)
            }

            // Read the entire column for this date
            val columnLetter = indexToColumnLetter(columnIndex)
            val dataRange = "$currentYearTab!$columnLetter:$columnLetter"
            val dataResponse = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, dataRange)
                .execute()

            val columnData = dataResponse.getValues() ?: return@withContext Result.success(null)

            // Parse the date
            val date = AttendanceRecord.parseDateFromSheet(dateString) ?: LocalDate.now()

            // Create attendance record
            val record = AttendanceRecord(
                date = date,
                dateString = dateString,
                columnIndex = columnIndex
            )

            // Match attendance marks with members (skip header row)
            for (rowIndex in 1 until columnData.size) {
                val cell = columnData[rowIndex].firstOrNull()?.toString()?.trim() ?: ""
                val isPresent = cell.isNotEmpty() && cell.lowercase() == "x"

                // Find member by row index (adding 1 because row 0 is header)
                val member = members.find { it.rowIndex == rowIndex + 1 }
                if (member != null && isPresent) {
                    record.markPresent(member.id, member.category)
                }
            }

            Result.success(record)
        } catch (e: UserRecoverableAuthIOException) {
            // ISSUE #1 FIX: Handle OAuth token expiration
            Result.failure(Exception("Authentication expired. Please sign in again."))
        } catch (e: GoogleJsonResponseException) {
            // TASK #1 FIX: Add sheet deletion detection
            if (e.statusCode == 404) {
                Result.failure(Exception("Sheet '$currentYearTab' not found. Please check spreadsheet configuration."))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads all attendance data from the sheet.
     *
     * Reads the entire sheet, identifies all date columns, and populates
     * attendance history for all members using immutable pattern.
     *
     * @param members The list of members to populate with attendance history
     * @return Result containing Pair of (updated members with attendance history, AttendanceRecord list), or failure with exception
     */
    suspend fun readAllAttendance(members: List<Member>): Result<Pair<List<Member>, List<AttendanceRecord>>> =
        withContext(Dispatchers.IO) {
            // ISSUE #2 FIX: Check network connectivity before API call
            if (!isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No internet connection"))
            }

            try {
                // Read the entire data range
                val range = "$currentYearTab!A1:ZZ"
                val response = sheetsService.spreadsheets().values()
                    .get(SPREADSHEET_ID, range)
                    .execute()

                val values = response.getValues() ?: return@withContext Result.failure(
                    Exception("No data found in sheet")
                )

                if (values.isEmpty()) {
                    return@withContext Result.success(Pair(members, emptyList()))
                }

                val headerRow = values[0]
                val attendanceRecords = mutableListOf<AttendanceRecord>()

                // Build attendance map for all members (immutable pattern)
                val attendanceMap = mutableMapOf<String, MutableMap<String, Boolean>>()
                members.forEach { member ->
                    attendanceMap[member.id] = mutableMapOf()
                }

                // Identify date columns (cells containing "/" in the header)
                // Also deduplicate by date and filter out future dates
                val today = java.time.LocalDate.now()
                val seenDates = mutableSetOf<java.time.LocalDate>()
                val dateColumns = headerRow.mapIndexedNotNull { index, cell ->
                    val cellValue = cell?.toString()?.trim() ?: ""
                    if (cellValue.contains("/")) {
                        val parsedDate = AttendanceRecord.parseDateFromSheet(cellValue)
                        // Only include if: (1) not a duplicate, (2) not in the future
                        if (parsedDate != null && !parsedDate.isAfter(today) && seenDates.add(parsedDate)) {
                            index to cellValue
                        } else {
                            if (BuildConfig.DEBUG && parsedDate != null) {
                                if (parsedDate.isAfter(today)) {
                                    Log.d("GoogleSheetsService", "Skipping future date column: $cellValue ($parsedDate)")
                                } else {
                                    Log.d("GoogleSheetsService", "Skipping duplicate date column: $cellValue (already have $parsedDate)")
                                }
                            }
                            null
                        }
                    } else {
                        null
                    }
                }

                // For each date column, read attendance
                for ((columnIndex, dateString) in dateColumns) {
                    val date = AttendanceRecord.parseDateFromSheet(dateString) ?: continue

                    val record = AttendanceRecord(
                        date = date,
                        dateString = dateString,
                        columnIndex = columnIndex
                    )

                    // Read attendance marks for this column (skip header row)
                    for (rowIndex in 1 until values.size) {
                        val row = values[rowIndex]
                        val cell = row.getOrNull(columnIndex)?.toString()?.trim() ?: ""
                        val isPresent = cell.isNotEmpty() && cell.lowercase() == "x"

                        // Find member by row index (adding 1 because row 0 is header)
                        val member = members.find { it.rowIndex == rowIndex + 1 }
                        if (member != null) {
                            // DEBUG: Log cell value and presence detection for first few members
                            if (BuildConfig.DEBUG && member.name.contains("Stormie", ignoreCase = true)) {
                                Log.d("GoogleSheetsService", "Date: $dateString, Member: ${member.name}, Cell: '$cell', IsPresent: $isPresent")
                            }

                            // CRITICAL FIX: Build attendance map instead of mutating members
                            attendanceMap[member.id]?.put(dateString, isPresent)
                            if (isPresent) {
                                record.markPresent(member.id, member.category)
                            }
                        }
                    }

                    attendanceRecords.add(record)
                }

                // CRITICAL FIX: Create new members with attendance history (immutable pattern)
                val updatedMembers = members.map { member ->
                    val attendanceHistory = attendanceMap[member.id] ?: emptyMap()
                    member.copy(attendanceHistory = attendanceHistory)
                }

                // Debug logging for development
                if (BuildConfig.DEBUG) {
                    Log.d("GoogleSheetsService", "Total members updated: ${updatedMembers.size}")
                    Log.d("GoogleSheetsService", "Total attendance records created: ${attendanceRecords.size}")
                }

                // Sort records by date and return both updated members and records
                Result.success(Pair(updatedMembers, attendanceRecords.sortedBy { it.date }))
            } catch (e: UserRecoverableAuthIOException) {
                // ISSUE #1 FIX: Handle OAuth token expiration
                Result.failure(Exception("Authentication expired. Please sign in again."))
            } catch (e: GoogleJsonResponseException) {
                // TASK #1 FIX: Add sheet deletion detection
                if (e.statusCode == 404) {
                    Result.failure(Exception("Sheet '$currentYearTab' not found. Please check spreadsheet configuration."))
                } else {
                    Result.failure(e)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Writes attendance data for a specific date to the sheet.
     *
     * If the date column doesn't exist, it will be created.
     * Writes "x" for present members and empty string for absent members.
     *
     * @param dateString The date in sheet format (e.g., "11/06/25")
     * @param presentMemberIds Set of member IDs who were present
     * @param allMembers Complete list of all members
     * @return Result indicating success or failure
     */
    suspend fun writeAttendance(
        dateString: String,
        presentMemberIds: Set<String>,
        allMembers: List<Member>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // ISSUE #2 FIX: Check network connectivity before API call
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        try {
            // Read header row to check if date column exists
            val headerRange = "$currentYearTab!1:1"
            val headerResponse = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, headerRange)
                .execute()

            val headerRow = headerResponse.getValues()?.firstOrNull()?.toMutableList()
                ?: mutableListOf()

            // Find or create date column
            // Parse the target date for comparison
            val targetDate = AttendanceRecord.parseDateFromSheet(dateString)

            var columnIndex = headerRow.indexOfFirst { cell ->
                val cellString = cell.toString().trim()
                // Try both exact match and date parsing for flexibility
                cellString == dateString ||
                (targetDate != null && AttendanceRecord.parseDateFromSheet(cellString) == targetDate)
            }

            // ISSUE #4 FIX: Combine header and data writes into SINGLE batch operation for atomicity
            val updates = mutableListOf<ValueRange>()

            if (columnIndex == -1) {
                // Date column doesn't exist, append it to header
                columnIndex = headerRow.size
                headerRow.add(dateString)

                // Add header update to batch
                updates.add(
                    ValueRange()
                        .setRange(headerRange)
                        .setValues(listOf(headerRow))
                )
            }

            // TASK #3 FIX: Add empty member list validation
            if (allMembers.isEmpty()) {
                return@withContext Result.failure(Exception("No members found. Cannot save attendance."))
            }

            // Prepare attendance column data
            val columnLetter = indexToColumnLetter(columnIndex)

            // Create attendance values for all members, sorted by row index
            val sortedMembers = allMembers.sortedBy { it.rowIndex }
            val attendanceValues = sortedMembers.map { member ->
                listOf(if (member.id in presentMemberIds) "x" else "")
            }

            // Update starts from row 2 (row 1 is header)
            val firstRow = sortedMembers.firstOrNull()?.rowIndex ?: 2
            val lastRow = sortedMembers.lastOrNull()?.rowIndex ?: (sortedMembers.size + 1)
            val dataRange = "$currentYearTab!$columnLetter$firstRow:$columnLetter$lastRow"
            updates.add(
                ValueRange()
                    .setRange(dataRange)
                    .setValues(attendanceValues)
            )

            // Execute SINGLE atomic batch update for both header and data
            val batchRequest = BatchUpdateValuesRequest()
                .setValueInputOption("RAW")
                .setData(updates)

            sheetsService.spreadsheets().values()
                .batchUpdate(SPREADSHEET_ID, batchRequest)
                .execute()

            // TASK #4 FIX: Add concurrent write detection
            try {
                val verifyResponse = sheetsService.spreadsheets().values()
                    .get(SPREADSHEET_ID, headerRange)
                    .execute()
                val verifyHeader = verifyResponse.getValues()?.firstOrNull() ?: emptyList()
                val dateCount = verifyHeader.count { cell ->
                    val cellStr = cell.toString().trim()
                    cellStr == dateString
                }
                if (dateCount > 1) {
                    android.util.Log.w("GoogleSheetsService", "Warning: Duplicate date columns detected for $dateString")
                }
            } catch (e: Exception) {
                // Verification failed but write succeeded, just log
                android.util.Log.e("GoogleSheetsService", "Failed to verify write", e)
            }

            Result.success(Unit)
        } catch (e: UserRecoverableAuthIOException) {
            // ISSUE #1 FIX: Handle OAuth token expiration
            Result.failure(Exception("Authentication expired. Please sign in again."))
        } catch (e: GoogleJsonResponseException) {
            // TASK #2 FIX: Add permission error detection
            if (e.statusCode == 403) {
                Result.failure(Exception("Permission denied. You need edit access to this sheet."))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tests the connection to Google Sheets.
     *
     * Attempts a simple read operation to verify credentials and permissions.
     *
     * @return Result with true if connection successful, or failure with exception
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val range = "$currentYearTab!B1:C1"
            sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds a new member to the Google Sheet.
     *
     * Finds the correct category section and inserts the member at the end of that section.
     *
     * @param name The member's name
     * @param category The member's category
     * @return Result indicating success or failure
     */
    suspend fun addMember(name: String, category: Category): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        try {
            // Read columns B and C to find where to insert
            val readRange = "$currentYearTab!B:C"
            val response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, readRange)
                .execute()

            val values = response.getValues() ?: emptyList()

            // Find the last row that has the same category in column C (index 1)
            var insertRow = -1
            for (rowIndex in values.indices.reversed()) {
                val row = values[rowIndex]
                if (row.size >= 2) {
                    val rowCategory = row.getOrNull(1)?.toString()?.trim()
                    if (rowCategory.equals(category.abbreviation, ignoreCase = true)) {
                        // Found the last member of this category
                        // Insert after this row (rowIndex is 0-based, sheet rows are 1-based)
                        insertRow = rowIndex + 2  // +1 for 1-based indexing, +1 to insert after
                        break
                    }
                }
            }

            // If no member of this category exists, find the category section marker
            if (insertRow == -1) {
                // Read column A to find category markers
                val colARange = "$currentYearTab!A:A"
                val colAResponse = sheetsService.spreadsheets().values()
                    .get(SPREADSHEET_ID, colARange)
                    .execute()

                val colAValues = colAResponse.getValues() ?: emptyList()

                // Find the category marker row based on category
                val markerText = when (category) {
                    Category.ORIGINAL_MEMBER -> "OM"
                    Category.XENOS_TRANSFER -> "XT"
                    Category.RETURNING_NEW, Category.FIRST_TIMER -> "FT/RN"
                    Category.VISITOR -> "V"
                }

                for (rowIndex in colAValues.indices) {
                    val cellValue = colAValues[rowIndex].firstOrNull()?.toString()?.trim()
                    if (cellValue == markerText) {
                        // Found the category marker, insert right after it
                        insertRow = rowIndex + 2  // +1 for 1-based, +1 to insert after marker
                        break
                    }
                }

                // If still not found, insert at end
                if (insertRow == -1) {
                    insertRow = values.size + 1
                }
            }

            // Insert a new row using the Sheets API
            val spreadsheet = sheetsService.spreadsheets()
                .get(SPREADSHEET_ID)
                .execute()

            val sheetId = spreadsheet.sheets.find {
                it.properties.title == currentYearTab
            }?.properties?.sheetId ?: return@withContext Result.failure(
                Exception("Sheet '$currentYearTab' not found")
            )

            // Insert a blank row at the target position
            val insertRequest = Request().setInsertDimension(
                InsertDimensionRequest()
                    .setRange(
                        DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("ROWS")
                            .setStartIndex(insertRow - 1)  // 0-based for API
                            .setEndIndex(insertRow)         // Insert 1 row
                    )
            )

            val batchUpdateRequest = BatchUpdateSpreadsheetRequest()
                .setRequests(listOf(insertRequest))

            sheetsService.spreadsheets()
                .batchUpdate(SPREADSHEET_ID, batchUpdateRequest)
                .execute()

            // Now write the member data to the newly inserted row
            val writeRange = "$currentYearTab!B$insertRow:C$insertRow"
            val newValues = listOf(
                listOf(name, category.abbreviation)
            )

            val body = ValueRange().setValues(newValues)

            sheetsService.spreadsheets().values()
                .update(SPREADSHEET_ID, writeRange, body)
                .setValueInputOption("RAW")
                .execute()

            Result.success(Unit)
        } catch (e: UserRecoverableAuthIOException) {
            Result.failure(Exception("Authentication expired. Please sign in again."))
        } catch (e: GoogleJsonResponseException) {
            // TASK #2 FIX: Add permission error detection
            if (e.statusCode == 403) {
                Result.failure(Exception("Permission denied. You need edit access to this sheet."))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing member in the Google Sheet.
     *
     * Updates the member's row with new name and category in columns B and C.
     *
     * @param member The member to update (must have valid rowIndex)
     * @param newName The new name
     * @param newCategory The new category
     * @return Result indicating success or failure
     */
    suspend fun updateMember(member: Member, newName: String, newCategory: Category): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        try {
            // Update the specific row (rowIndex is already 1-based from readMembers)
            val rowNumber = member.rowIndex
            val range = "$currentYearTab!B$rowNumber:C$rowNumber"
            val values = listOf(
                listOf(newName, newCategory.abbreviation)
            )

            val body = ValueRange().setValues(values)

            sheetsService.spreadsheets().values()
                .update(SPREADSHEET_ID, range, body)
                .setValueInputOption("RAW")
                .execute()

            Result.success(Unit)
        } catch (e: UserRecoverableAuthIOException) {
            Result.failure(Exception("Authentication expired. Please sign in again."))
        } catch (e: GoogleJsonResponseException) {
            // TASK #2 FIX: Add permission error detection
            if (e.statusCode == 403) {
                Result.failure(Exception("Permission denied. You need edit access to this sheet."))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a member from the Google Sheet.
     *
     * Deletes the entire row for this member from the sheet.
     * All subsequent rows will shift up.
     *
     * @param member The member to delete (must have valid rowIndex)
     * @return Result indicating success or failure
     */
    suspend fun deleteMember(member: Member): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        try {
            // First, get the sheet ID for the current year tab
            val spreadsheet = sheetsService.spreadsheets()
                .get(SPREADSHEET_ID)
                .execute()

            val sheetId = spreadsheet.sheets?.find {
                it.properties.title == currentYearTab
            }?.properties?.sheetId ?: return@withContext Result.failure(
                Exception("Sheet '$currentYearTab' not found")
            )

            // Delete the entire row (rowIndex is 1-based, API needs 0-based)
            val startIndex = member.rowIndex - 1  // Convert to 0-based
            val endIndex = member.rowIndex  // End is exclusive, so this deletes just this row

            val deleteRequest = Request().setDeleteDimension(
                DeleteDimensionRequest()
                    .setRange(
                        DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("ROWS")
                            .setStartIndex(startIndex)
                            .setEndIndex(endIndex)
                    )
            )

            val batchUpdateRequest = BatchUpdateSpreadsheetRequest()
                .setRequests(listOf(deleteRequest))

            sheetsService.spreadsheets()
                .batchUpdate(SPREADSHEET_ID, batchUpdateRequest)
                .execute()

            Result.success(Unit)
        } catch (e: UserRecoverableAuthIOException) {
            Result.failure(Exception("Authentication expired. Please sign in again."))
        } catch (e: GoogleJsonResponseException) {
            // TASK #2 FIX: Add permission error detection
            if (e.statusCode == 403) {
                Result.failure(Exception("Permission denied. You need edit access to this sheet."))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Converts a zero-based column index to Excel-style column letter(s).
     *
     * Examples:
     * - 0 → "A"
     * - 1 → "B"
     * - 25 → "Z"
     * - 26 → "AA"
     * - 27 → "AB"
     *
     * @param index Zero-based column index
     * @return Excel-style column letter(s)
     */
    private fun indexToColumnLetter(index: Int): String {
        var num = index
        var columnLetter = ""

        while (num >= 0) {
            columnLetter = ('A' + (num % 26)) + columnLetter
            num = (num / 26) - 1
        }

        return columnLetter
    }
}
