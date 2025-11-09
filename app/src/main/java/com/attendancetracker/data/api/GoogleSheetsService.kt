package com.attendancetracker.data.api

import android.content.Context
import com.attendancetracker.data.models.AttendanceRecord
import com.attendancetracker.data.models.Category
import com.attendancetracker.data.models.Member
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
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
     */
    private val SPREADSHEET_ID = "11M2RMedyD0pn0cve8MsgOVkXmNorOCSv80hRqMUaft0"

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
     */
    private val sheetsService: Sheets by lazy {
        Sheets.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Attendance Tracker")
            .build()
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
                val member = Member(
                    id = "${currentYearTab}_${rowIndex + 1}",
                    name = name,
                    category = category,
                    rowIndex = rowIndex + 1 // Google Sheets uses 1-based indexing
                )

                members.add(member)
            }

            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads attendance data for a specific date.
     *
     * Finds the column for the given date, reads attendance marks for that column,
     * and updates the attendance history for each member.
     *
     * @param dateString The date in sheet format (e.g., "11/06/25")
     * @param members The list of members to match attendance records against
     * @return Result containing AttendanceRecord or null if date not found, or failure with exception
     */
    suspend fun readAttendanceForDate(
        dateString: String,
        members: List<Member>
    ): Result<AttendanceRecord?> = withContext(Dispatchers.IO) {
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
                if (member != null) {
                    member.markAttendance(dateString, isPresent)
                    if (isPresent) {
                        record.markPresent(member.id, member.category)
                    }
                }
            }

            Result.success(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads all attendance data from the sheet.
     *
     * Reads the entire sheet, identifies all date columns, and populates
     * attendance history for all members.
     *
     * @param members The list of members to populate with attendance history
     * @return Result containing list of AttendanceRecord objects, or failure with exception
     */
    suspend fun readAllAttendance(members: List<Member>): Result<List<AttendanceRecord>> =
        withContext(Dispatchers.IO) {
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
                    return@withContext Result.success(emptyList())
                }

                val headerRow = values[0]
                val attendanceRecords = mutableListOf<AttendanceRecord>()

                // Identify date columns (cells containing "/" in the header)
                val dateColumns = headerRow.mapIndexedNotNull { index, cell ->
                    val cellValue = cell?.toString()?.trim() ?: ""
                    if (cellValue.contains("/")) {
                        index to cellValue
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
                            member.markAttendance(dateString, isPresent)
                            if (isPresent) {
                                record.markPresent(member.id, member.category)
                            }
                        }
                    }

                    attendanceRecords.add(record)
                }

                // Sort records by date
                Result.success(attendanceRecords.sortedBy { it.date })
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

            if (columnIndex == -1) {
                // Date column doesn't exist, append it to header
                columnIndex = headerRow.size
                headerRow.add(dateString)

                // Write updated header
                val headerValueRange = ValueRange().setValues(listOf(headerRow))
                sheetsService.spreadsheets().values()
                    .update(SPREADSHEET_ID, headerRange, headerValueRange)
                    .setValueInputOption("RAW")
                    .execute()
            }

            // Prepare batch update for attendance column
            val columnLetter = indexToColumnLetter(columnIndex)
            val updates = mutableListOf<ValueRange>()

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

            // Execute batch update
            val batchRequest = BatchUpdateValuesRequest()
                .setValueInputOption("RAW")
                .setData(updates)

            sheetsService.spreadsheets().values()
                .batchUpdate(SPREADSHEET_ID, batchRequest)
                .execute()

            Result.success(Unit)
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
        try {
            // First, get the sheet ID for the current year tab
            val spreadsheet = sheetsService.spreadsheets()
                .get(SPREADSHEET_ID)
                .execute()

            val sheetId = spreadsheet.sheets.find {
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
