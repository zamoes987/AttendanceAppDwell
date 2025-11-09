# 🤖 CLAUDE CODE CONVERSATION SCRIPT
## Complete Attendance Tracker App Build Guide

This document contains the **exact prompts** to use with Claude Code to build your entire Attendance Tracker app from scratch.

**IMPORTANT:** Copy each prompt in order, paste into Claude Code, and wait for it to complete before moving to the next prompt.

---

## 📋 PREREQUISITES

Before starting Claude Code:
1. ✅ Create project directory: `mkdir AttendanceTrackerApp && cd AttendanceTrackerApp`
2. ✅ Have Android Studio installed (for building/running the app later)
3. ✅ Have your Google Sheet open to get the Sheet ID

---

## 🚀 SESSION 1: PROJECT INITIALIZATION

### Prompt 1.1: Create Project Structure

```
I need to create a complete Android app project from scratch for an attendance tracking system that integrates with Google Sheets.

PROJECT SPECIFICATIONS:
- Project name: AttendanceTrackerApp
- Package name: com.attendancetracker
- Language: Kotlin
- UI Framework: Jetpack Compose (Material 3)
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34
- Build system: Gradle with Kotlin DSL

REQUIRED DEPENDENCIES:
- Jetpack Compose (latest BOM)
- Google Sheets API v4
- Google Auth Library OAuth2
- Google Play Services Auth
- AndroidX Lifecycle ViewModel Compose
- Kotlin Coroutines (Android + Play Services)
- AndroidX Navigation Compose
- DataStore Preferences
- Gson

Create the complete project structure including:
1. Root-level build.gradle.kts
2. settings.gradle.kts
3. gradle.properties
4. app/build.gradle.kts
5. AndroidManifest.xml with necessary permissions (INTERNET, ACCESS_NETWORK_STATE, GET_ACCOUNTS)
6. Basic folder structure in app/src/main/java/com/attendancetracker/
7. res/ folder structure

Start by creating the Gradle configuration files first.
```

**Wait for completion, then verify the files were created.**

---

### Prompt 1.2: Verify and Adjust

```
List all files you created. Make sure the package structure is:
com/attendancetracker/
├── data/
│   ├── models/
│   ├── api/
│   └── repository/
├── ui/
│   ├── screens/
│   ├── components/
│   └── theme/
└── viewmodel/

If this structure doesn't exist, create it now.
```

---

## 📊 SESSION 2: DATA MODELS

### Prompt 2.1: Create Category Enum

```
Create file: app/src/main/java/com/attendancetracker/data/models/Category.kt

This should be an enum class representing member categories:

CATEGORIES:
- OM = Original Member
- XT = Xenos Transfer  
- RN = Returning New
- FT = First Timer
- V = Visitor

Include:
1. displayName and abbreviation properties for each category
2. Companion object with fromAbbreviation() function to convert "OM" → Category.ORIGINAL_MEMBER
3. getRegularCategories() function that returns list excluding Visitors and First Timers

Use proper Kotlin enum syntax with full kdoc comments.
```

---

### Prompt 2.2: Create Member Data Class

```
Create file: app/src/main/java/com/attendancetracker/data/models/Member.kt

Data class representing a member in the attendance system:

PROPERTIES:
- id: String (unique identifier, format: "2025_rowNumber")
- name: String (full name from sheet)
- category: Category (member's status)
- rowIndex: Int (actual row number in Google Sheet, 1-indexed)
- attendanceHistory: MutableMap<String, Boolean> (key = date string like "11/06/25", value = present/absent)

FUNCTIONS:
- getTotalAttendance(): Int (count of all "true" values)
- wasPresent(dateString: String): Boolean
- markAttendance(dateString: String, present: Boolean): Unit
- getAttendancePercentage(): Double

EXTENSION FUNCTIONS:
- List<Member>.groupByCategory(): Map<Category, List<Member>>
- List<Member>.sortByNameAndCategory(): List<Member>

Include full kdoc comments for all properties and functions.
```

---

### Prompt 2.3: Create AttendanceRecord Data Class

```
Create file: app/src/main/java/com/attendancetracker/data/models/AttendanceRecord.kt

Data class for a single meeting's attendance:

PROPERTIES:
- date: LocalDate
- dateString: String (format matching sheet: "11/06/25")
- columnIndex: Int (column position in sheet)
- presentMembers: MutableSet<String> (set of member IDs)
- categoryTotals: MutableMap<Category, Int> (count per category)

FUNCTIONS:
- getRegularTotal(): Int (excludes FT and V)
- getTotalAttendance(): Int (all categories)
- markPresent(memberId: String, category: Category)
- markAbsent(memberId: String, category: Category)
- isPresent(memberId: String): Boolean

COMPANION OBJECT:
- formatDateForSheet(date: LocalDate): String (converts to "M/d/yy" format)
- getTodayDateString(): String
- parseDateFromSheet(dateString: String): LocalDate?

Also create a simple AttendanceSummary data class with:
- weekOf: LocalDate
- totalPresent: Int
- categoryBreakdown: Map<Category, Int>
- comparisonToPrevious: Int? (nullable)

Use java.time.LocalDate for dates. Include full kdoc.
```

---

## 🔌 SESSION 3: GOOGLE SHEETS API SERVICE

### Prompt 3.1: Create Google Sheets Service

```
Create file: app/src/main/java/com/attendancetracker/data/api/GoogleSheetsService.kt

This class handles ALL Google Sheets API interactions.

IMPORTS NEEDED:
- com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
- com.google.api.client.http.javanet.NetHttpTransport
- com.google.api.client.json.gson.GsonFactory
- com.google.api.services.sheets.v4.Sheets
- com.google.api.services.sheets.v4.SheetsScopes
- com.google.api.services.sheets.v4.model.*

CLASS STRUCTURE:
- Constructor: GoogleSheetsService(context: Context, accountName: String)
- Private property: SPREADSHEET_ID = "YOUR_SPREADSHEET_ID_HERE" (placeholder)
- Private property: currentYearTab = "2025"
- Lazy initialized credential using GoogleAccountCredential with SPREADSHEETS scope
- Lazy initialized sheetsService

SUSPEND FUNCTIONS (all return Result<T> and use withContext(Dispatchers.IO)):

1. readMembers(): Result<List<Member>>
   - Reads range "2025!A:B" (Name and Status columns)
   - Skips header row (row 0)
   - Skips total rows (rows that start with category abbreviations)
   - Creates Member objects with proper rowIndex
   - Returns Result.success(members) or Result.failure(exception)

2. readAttendanceForDate(dateString: String, members: List<Member>): Result<AttendanceRecord?>
   - Reads header row to find column index for dateString
   - Returns null if date column doesn't exist
   - Reads that column's data
   - Matches with members by rowIndex
   - Populates AttendanceRecord
   - Updates member.attendanceHistory

3. readAllAttendance(members: List<Member>): Result<List<AttendanceRecord>>
   - Reads entire sheet range "2025!A1:ZZ"
   - Identifies date columns (header cells containing "/")
   - For each date column, reads all attendance marks
   - Populates each member's attendanceHistory map
   - Returns sorted list of AttendanceRecord objects

4. writeAttendance(dateString: String, presentMemberIds: Set<String>, allMembers: List<Member>): Result<Unit>
   - Checks if date column exists in header row
   - If not, appends new date column to header
   - Uses BatchUpdateValuesRequest to write all member attendance
   - For each member: writes "x" if present, "" if absent
   - Uses columnLetter format (A, B, C, ... Z, AA, AB, etc.)

5. testConnection(): Result<Boolean>
   - Simple test read of "2025!A1:B1"
   - Returns success(true) or failure(exception)

HELPER FUNCTION:
- indexToColumnLetter(index: Int): String
  Converts 0→A, 1→B, 25→Z, 26→AA, 27→AB, etc.

Add comprehensive kdoc comments explaining Google Sheets API usage, OAuth requirements, and error handling.
```

---

## 🗄️ SESSION 4: REPOSITORY LAYER

### Prompt 4.1: Create Sheets Repository

```
Create file: app/src/main/java/com/attendancetracker/data/repository/SheetsRepository.kt

This repository provides a clean API between the UI and Google Sheets service.

CLASS STRUCTURE:
- Constructor: SheetsRepository(context: Context, accountName: String)
- Private property: sheetsService = GoogleSheetsService(context, accountName)

STATE FLOWS (using MutableStateFlow):
- _members: MutableStateFlow<List<Member>>
- members: Flow<List<Member>> (exposed as read-only)
- _attendanceRecords: MutableStateFlow<List<AttendanceRecord>>
- attendanceRecords: Flow<List<AttendanceRecord>>
- _isLoading: MutableStateFlow<Boolean>
- isLoading: Flow<Boolean>
- _error: MutableStateFlow<String?>
- error: Flow<String?>

SUSPEND FUNCTIONS:

1. loadAllData(): Result<Unit>
   - Sets isLoading = true
   - Calls sheetsService.readMembers()
   - Updates _members state
   - Calls sheetsService.readAllAttendance(members)
   - Updates _attendanceRecords state
   - Handles errors and sets _error state
   - Sets isLoading = false in finally block

2. refreshMembers(): Result<Unit>
   - Reloads just the member list
   - Updates _members state

3. saveAttendance(presentMemberIds: Set<String>): Result<Unit>
   - Gets current Thursday date
   - Formats as dateString
   - Calls sheetsService.writeAttendance()
   - Updates local cache (both members and attendanceRecords)
   - Handles errors

4. testConnection(): Result<Boolean>
   - Delegates to sheetsService.testConnection()

REGULAR FUNCTIONS:

5. getCurrentThursday(): LocalDate
   - Returns today if it's Thursday
   - Otherwise returns next Thursday
   - Uses java.time.temporal.TemporalAdjusters

6. getTodayAttendance(): AttendanceRecord?
   - Gets current Thursday
   - Finds matching record in _attendanceRecords

7. getMembersByCategory(): Map<Category, List<Member>>
   - Groups current members by category
   - Returns sorted map

8. getAttendanceSummary(date: LocalDate): AttendanceSummary?
   - Finds record for date
   - Returns summary with totals

9. getAllSummaries(): List<AttendanceSummary>
   - Converts all records to summaries

10. searchMembers(query: String): List<Member>
    - Filters members by name (case-insensitive)

11. getMemberById(id: String): Member?
    - Returns single member or null

12. getCategoryTrends(weeksBack: Int = 4): Map<Category, Double>
    - Calculates average attendance % per category
    - For recent X weeks

13. clearError()
    - Sets _error to null

Include comprehensive kdoc for all functions explaining caching strategy and state management.
```

---

## 🎨 SESSION 5: UI THEME

### Prompt 5.1: Create Colors

```
Create file: app/src/main/java/com/attendancetracker/ui/theme/Color.kt

Define Material 3 color scheme:

PRIMARY COLORS:
- Primary = Color(0xFF1976D2) // Blue
- PrimaryVariant = Color(0xFF004BA0)
- Secondary = Color(0xFF388E3C) // Green
- SecondaryVariant = Color(0xFF00600F)

BACKGROUNDS:
- BackgroundLight = Color(0xFFFAFAFA)
- BackgroundDark = Color(0xFF121212)
- SurfaceLight = Color(0xFFFFFFFF)
- SurfaceDark = Color(0xFF1E1E1E)

CATEGORY COLORS (for visual distinction):
- CategoryOM = Color(0xFF1976D2) // Blue
- CategoryXT = Color(0xFF7B1FA2) // Purple
- CategoryRN = Color(0xFFF57C00) // Orange
- CategoryFT = Color(0xFF388E3C) // Green
- CategoryV = Color(0xFF616161) // Gray

STATUS COLORS:
- Present = Color(0xFF4CAF50) // Green
- Absent = Color(0xFFE0E0E0) // Light gray
- ErrorRed = Color(0xFFD32F2F)
- WarningAmber = Color(0xFFFFA000)

TEXT COLORS:
- TextPrimary = Color(0xFF212121)
- TextSecondary = Color(0xFF757575)
- TextOnPrimary = Color(0xFFFFFFFF)

Use androidx.compose.ui.graphics.Color for all definitions.
```

---

### Prompt 5.2: Create Theme

```
Create file: app/src/main/java/com/attendancetracker/ui/theme/Theme.kt

Material 3 theme configuration:

1. Define DarkColorScheme using darkColorScheme()
2. Define LightColorScheme using lightColorScheme()
3. Create AttendanceTrackerTheme composable function:
   - Parameter: darkTheme: Boolean = isSystemInDarkTheme()
   - Parameter: content: @Composable () -> Unit
   - Selects color scheme based on darkTheme
   - Sets status bar color to match theme
   - Wraps content in MaterialTheme

Include SideEffect to update system bars using WindowCompat.

Import androidx.compose.material3.* for Material 3 components.
```

---

### Prompt 5.3: Create Typography

```
Create file: app/src/main/java/com/attendancetracker/ui/theme/Type.kt

Define Material 3 Typography using androidx.compose.material3.Typography:

- displayLarge: 57sp, Bold
- displayMedium: 45sp, Bold
- headlineLarge: 32sp, Bold
- headlineMedium: 28sp, SemiBold
- titleLarge: 22sp, SemiBold
- titleMedium: 16sp, Medium
- bodyLarge: 16sp, Normal
- bodyMedium: 14sp, Normal
- labelLarge: 14sp, Medium

Use FontFamily.Default for all styles.
Set appropriate lineHeight for each style.
```

---

## 🧠 SESSION 6: VIEWMODEL

### Prompt 6.1: Create AttendanceViewModel

```
Create file: app/src/main/java/com/attendancetracker/viewmodel/AttendanceViewModel.kt

ViewModel connecting UI to repository:

CLASS: AttendanceViewModel(private val repository: SheetsRepository) : ViewModel()

STATE FLOWS (exposed to UI):
- val members: StateFlow<List<Member>> from repository
- val attendanceRecords: StateFlow<List<AttendanceRecord>> from repository
- val isLoading: StateFlow<Boolean> from repository
- val error: StateFlow<String?> from repository

MUTABLE STATE (internal):
- private val _selectedMembers = MutableStateFlow<Set<String>>(emptySet())
- val selectedMembers: StateFlow<Set<String>>
- private val _currentDate = MutableStateFlow(repository.getCurrentThursday())
- val currentDate: StateFlow<LocalDate>
- private val _showSaveSuccess = MutableStateFlow(false)
- val showSaveSuccess: StateFlow<Boolean>

UI STATE DATA CLASS:
data class UiState(
    val membersByCategory: Map<Category, List<Member>>,
    val selectedCount: Int,
    val totalMembers: Int,
    val todayDateString: String,
    val canSave: Boolean
)

- val uiState: StateFlow<UiState> derived using combine()

FUNCTIONS:

1. init block:
   - Launches loadData()
   - Loads today's attendance to pre-populate selectedMembers

2. loadData()
   - viewModelScope.launch
   - Calls repository.loadAllData()
   - On success, loads today's attendance
   - Updates _selectedMembers with already-present members

3. toggleMemberSelection(memberId: String)
   - Adds/removes from _selectedMembers set

4. selectAll()
   - Adds all member IDs to _selectedMembers

5. clearAll()
   - Clears _selectedMembers

6. selectCategory(category: Category)
   - Adds all members of that category to _selectedMembers

7. saveAttendance()
   - viewModelScope.launch
   - Calls repository.saveAttendance(_selectedMembers.value)
   - On success, sets _showSaveSuccess = true
   - Handles errors

8. dismissSuccessMessage()
   - Sets _showSaveSuccess = false

9. clearError()
   - Calls repository.clearError()

10. refreshData()
    - Reloads all data from sheet

Include comprehensive kdoc explaining state management and UI interaction patterns.
```

---

## 📱 SESSION 7: UI COMPONENTS

### Prompt 7.1: Create Member List Item Component

```
Create file: app/src/main/java/com/attendancetracker/ui/components/MemberListItem.kt

Composable component for displaying a single member with attendance toggle:

@Composable
fun MemberListItem(
    member: Member,
    isSelected: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
)

LAYOUT:
- Card with elevation
- Row layout with padding
- Column containing:
  - Text: member name (bodyLarge style)
  - Text: category displayName (bodySmall, secondary color)
- Spacer (weight = 1f)
- Switch or Checkbox for attendance
  - Checked = isSelected
  - onCheckedChange = { onToggle(member.id) }
  - Use Present color when checked

STYLING:
- Card has rounded corners
- Different start border color based on member.category (use category colors from Color.kt)
- Proper spacing (16.dp padding)
- Enable ripple effect on click

Include @Preview with sample data.
```

---

### Prompt 7.2: Create Category Header Component

```
Create file: app/src/main/java/com/attendancetracker/ui/components/CategoryHeader.kt

Composable for category section headers:

@Composable
fun CategoryHeader(
    category: Category,
    memberCount: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
)

LAYOUT:
- Surface with category color as background
- Row layout:
  - Column:
    - Text: category displayName (titleMedium, bold)
    - Text: "$selectedCount / $memberCount present" (bodySmall)
  - Spacer(weight = 1f)
  - TextButton: "Select All"
    - onClick = onSelectAll

STYLING:
- Full width
- 12.dp vertical padding, 16.dp horizontal
- Category-specific background color (tinted, not full opacity)
- Proper text color contrast

Include @Preview.
```

---

### Prompt 7.3: Create Loading and Error Components

```
Create file: app/src/main/java/com/attendancetracker/ui/components/CommonComponents.kt

Three simple composables:

1. @Composable fun LoadingIndicator(modifier: Modifier = Modifier)
   - Box with Center alignment
   - CircularProgressIndicator
   - Optional Text: "Loading..."

2. @Composable fun ErrorMessage(
       message: String,
       onDismiss: () -> Unit,
       modifier: Modifier = Modifier
   )
   - Card with error color background
   - Row: Error icon + Text + Close IconButton
   - onDismiss closes the error

3. @Composable fun SuccessMessage(
       message: String,
       onDismiss: () -> Unit,
       modifier: Modifier = Modifier
   )
   - Similar to ErrorMessage but with success/green color
   - Shows checkmark icon

All with proper Material 3 styling and previews.
```

---

## 🖥️ SESSION 8: MAIN SCREENS

### Prompt 8.1: Create Home Screen

```
Create file: app/src/main/java/com/attendancetracker/ui/screens/HomeScreen.kt

Main screen for marking attendance:

@Composable
fun HomeScreen(
    viewModel: AttendanceViewModel,
    onNavigateToHistory: () -> Unit
)

COLLECT STATE:
- val uiState by viewModel.uiState.collectAsState()
- val isLoading by viewModel.isLoading.collectAsState()
- val error by viewModel.error.collectAsState()
- val showSuccess by viewModel.showSaveSuccess.collectAsState()

LAYOUT (Scaffold):

TOP APP BAR:
- Title: "Attendance - ${uiState.todayDateString}"
- Actions: IconButton for History navigation
- Actions: IconButton for Refresh

FLOATING ACTION BUTTON:
- ExtendedFloatingActionButton
- Text: "Save Attendance (${uiState.selectedCount})"
- Icon: Save icon
- onClick: viewModel.saveAttendance()
- Enabled only if uiState.canSave

CONTENT:
- Show LoadingIndicator when isLoading
- Show ErrorMessage if error != null
- Show SuccessMessage if showSuccess (auto-dismiss after 2 seconds using LaunchedEffect)

Main content (LazyColumn):
- For each category in uiState.membersByCategory:
  - CategoryHeader with onSelectAll = { viewModel.selectCategory(category) }
  - For each member in category:
    - MemberListItem(
        member = member,
        isSelected = member.id in selectedMembers,
        onToggle = { viewModel.toggleMemberSelection(it) }
      )

BOTTOM SECTION (sticky):
- Row with:
  - Button "Select All" -> viewModel.selectAll()
  - Button "Clear All" -> viewModel.clearAll()
  - Spacer
  - Text showing count: "${selectedCount} / ${totalMembers}"

Include proper padding, spacing, and Material 3 styling.
```

---

### Prompt 8.2: Create History Screen

```
Create file: app/src/main/java/com/attendancetracker/ui/screens/HistoryScreen.kt

Screen showing past attendance records:

@Composable
fun HistoryScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit
)

COLLECT STATE:
- val attendanceRecords by viewModel.attendanceRecords.collectAsState()
- val summaries = remember(attendanceRecords) { viewModel.repository.getAllSummaries() }

LAYOUT (Scaffold):

TOP APP BAR:
- Title: "Attendance History"
- NavigationIcon: Back arrow -> onNavigateBack()

CONTENT (LazyColumn):
- For each summary in summaries (sorted by date descending):
  - AttendanceSummaryCard:
    - Card with elevation
    - Column:
      - Text: Date (formatted: "Thursday, Month Day, Year")
      - Text: Total attendance with icon
      - Row of category chips showing breakdown:
        - "OM: X" in blue chip
        - "XT: X" in purple chip
        - "RN: X" in orange chip
        - etc.
      - Optional: Trend indicator (▲ or ▼) if comparisonToPrevious exists

EMPTY STATE:
- If no summaries, show centered message: "No attendance records yet"

Include proper Material 3 styling, spacing, and a preview.
```

---

## 🚀 SESSION 9: MAIN ACTIVITY & NAVIGATION

### Prompt 9.1: Create MainActivity with Google Sign-In

```
Create file: app/src/main/java/com/attendancetracker/MainActivity.kt

This is the entry point requiring Google authentication:

CLASS: MainActivity : ComponentActivity()

COMPANION OBJECT:
- const val RC_SIGN_IN = 9001

PROPERTIES:
- private lateinit var googleSignInClient: GoogleSignInClient
- private var accountName: String? = null
- private var repository: SheetsRepository? = null
- private var viewModel: AttendanceViewModel? = null

onCreate():
1. super.onCreate(savedInstanceState)
2. Configure Google Sign-In:
   - GoogleSignInOptions with EMAIL and SPREADSHEETS scope
   - Build GoogleSignInClient
3. Check if already signed in (GoogleSignIn.getLastSignedInAccount)
4. If signed in:
   - Initialize repository and viewModel
   - setContent with UI
5. If not signed in:
   - setContent with SignInScreen
   - Launch sign-in intent

onActivityResult():
- Handle RC_SIGN_IN result
- Get GoogleSignInAccount
- Extract accountName
- Initialize repository and viewModel
- Update UI

setContent block:
- AttendanceTrackerTheme {
    Surface {
      if (accountName != null && viewModel != null) {
        AttendanceNavigation(viewModel!!)
      } else {
        SignInScreen(onSignIn = { startSignIn() })
      }
    }
  }

HELPER FUNCTIONS:
- private fun startSignIn()
  - Creates sign-in intent
  - Launches with startActivityForResult

- private fun handleSignInResult(task: Task<GoogleSignInAccount>)
  - Extracts account
  - Sets accountName
  - Initializes repository and viewModel
  - Updates UI
```

---

### Prompt 9.2: Create Sign-In Screen

```
Create file: app/src/main/java/com/attendancetracker/ui/screens/SignInScreen.kt

Simple screen prompting for Google Sign-In:

@Composable
fun SignInScreen(
    onSignIn: () -> Unit
)

LAYOUT:
- Box(modifier = Modifier.fillMaxSize(), contentAlignment = Center)
- Column(horizontalAlignment = CenterHorizontally, spacing = 24.dp):
  - Icon: App icon or Google Sheets icon
  - Text: "Attendance Tracker" (headlineLarge)
  - Text: "Sign in with Google to continue" (bodyLarge, secondary color)
  - Spacer(16.dp)
  - Button(onClick = onSignIn, wide button):
    - Row: Google icon + Text "Sign in with Google"

STYLING:
- Center everything
- Use Primary color for button
- Proper padding (32.dp)
- Material 3 elevated button style

Include @Preview.
```

---

### Prompt 9.3: Create Navigation

```
Create file: app/src/main/java/com/attendancetracker/ui/Navigation.kt

Navigation setup using Compose Navigation:

@Composable
fun AttendanceNavigation(
    viewModel: AttendanceViewModel
)

ROUTES:
- const val ROUTE_HOME = "home"
- const val ROUTE_HISTORY = "history"

SETUP:
- val navController = rememberNavController()
- NavHost(navController, startDestination = ROUTE_HOME):
  
  composable(ROUTE_HOME) {
    HomeScreen(
      viewModel = viewModel,
      onNavigateToHistory = { navController.navigate(ROUTE_HISTORY) }
    )
  }
  
  composable(ROUTE_HISTORY) {
    HistoryScreen(
      viewModel = viewModel,
      onNavigateBack = { navController.popBackStack() }
    )
  }

Keep it simple - just two screens with basic navigation.
```

---

## 📄 SESSION 10: ANDROID MANIFEST

### Prompt 10.1: Update AndroidManifest.xml

```
Update the AndroidManifest.xml file:

Add these permissions before <application> tag:
- INTERNET
- ACCESS_NETWORK_STATE
- GET_ACCOUNTS

In <application> tag:
- android:usesCleartextTraffic="true" (for development)
- android:theme="@style/Theme.AttendanceTrackerApp"

In <activity> tag for MainActivity:
- android:exported="true"
- Intent filter for MAIN/LAUNCHER

Add inside <activity>:
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />
</intent-filter>

Ensure package name is "com.attendancetracker"
```

---

## 📝 SESSION 11: STRINGS AND RESOURCES

### Prompt 11.1: Create strings.xml

```
Create or update: app/src/main/res/values/strings.xml

Add all user-facing strings:
- app_name: "Attendance Tracker"
- sign_in_title: "Attendance Tracker"
- sign_in_prompt: "Sign in with Google to continue"
- sign_in_button: "Sign in with Google"
- home_title: "Attendance"
- save_attendance: "Save Attendance"
- select_all: "Select All"
- clear_all: "Clear All"
- history_title: "Attendance History"
- loading: "Loading..."
- error_load_failed: "Failed to load data"
- error_save_failed: "Failed to save attendance"
- success_saved: "Attendance saved successfully"
- no_history: "No attendance records yet"
- refresh: "Refresh"
- present: "Present"
- absent: "Absent"

Use proper XML formatting.
```

---

## 🎯 SESSION 12: FINAL SETUP & CONFIGURATION

### Prompt 12.1: Create README for Setup

```
Create: README.md in project root

Include:
1. Project overview
2. Prerequisites (Android Studio, Google Cloud account)
3. Google Cloud Console setup steps (OAuth, Sheet ID)
4. How to run the app
5. How to configure SPREADSHEET_ID
6. How to get SHA-1 fingerprint
7. Troubleshooting section
8. Project structure explanation

Make it comprehensive but easy to follow.
```

---

### Prompt 12.2: Create Configuration Checklist

```
Create: SETUP_CHECKLIST.md

Step-by-step checklist:
□ Install Android Studio
□ Open project in Android Studio
□ Sync Gradle files
□ Create Google Cloud project
□ Enable Google Sheets API
□ Configure OAuth consent screen
□ Create Android OAuth credentials (with SHA-1)
□ Get Google Sheet ID
□ Update GoogleSheetsService.kt with Sheet ID
□ Connect Android phone via USB
□ Enable USB debugging
□ Build and run app
□ Sign in with Google
□ Grant permissions
□ Test attendance marking

Include links to detailed guides for each step.
```

---

### Prompt 12.3: Verify Project Completeness

```
Please verify and list:
1. All files that were created
2. Complete directory structure
3. Any missing files or configurations
4. Summary of what needs to be done manually:
   - Google Cloud setup
   - Sheet ID configuration
   - SHA-1 fingerprint
   - Testing

Also check for any compilation errors or missing imports.
```

---

## ✅ FINAL STEPS (MANUAL)

After Claude Code completes all prompts:

1. **Open in Android Studio:**
   ```
   File → Open → Select AttendanceTrackerApp folder
   ```

2. **Sync Gradle:**
   - Android Studio will prompt to sync
   - Click "Sync Now"
   - Wait for dependencies to download

3. **Get SHA-1 Fingerprint:**
   ```bash
   # In Android Studio Terminal:
   gradlew signingReport
   
   # Or use keytool:
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```

4. **Follow Google Cloud Setup:**
   - Use the GOOGLE_SETUP_GUIDE.md created earlier
   - Create OAuth credentials with your SHA-1
   - Get your Sheet ID

5. **Update Code:**
   - Open `GoogleSheetsService.kt`
   - Replace `YOUR_SPREADSHEET_ID_HERE` with actual Sheet ID

6. **Build & Run:**
   - Connect Android phone via USB
   - Click "Run" (green play button) in Android Studio
   - Select your device
   - App will install and launch

---

## 🆘 TROUBLESHOOTING PROMPTS

If you encounter issues, use these follow-up prompts:

### Build Error:
```
I'm getting this build error: [paste error]

Please fix the issue and explain what was wrong.
```

### Missing Dependency:
```
The project can't find [class/package name].

Check the build.gradle.kts dependencies and add any missing imports.
```

### Runtime Crash:
```
The app crashes with this error: [paste logcat]

Please identify the issue and provide a fix.
```

---

## 📊 PROGRESS TRACKING

As you go through the prompts, check off each session:

- [ ] Session 1: Project Initialization
- [ ] Session 2: Data Models  
- [ ] Session 3: Google Sheets API Service
- [ ] Session 4: Repository Layer
- [ ] Session 5: UI Theme
- [ ] Session 6: ViewModel
- [ ] Session 7: UI Components
- [ ] Session 8: Main Screens
- [ ] Session 9: MainActivity & Navigation
- [ ] Session 10: Android Manifest
- [ ] Session 11: Strings & Resources
- [ ] Session 12: Final Setup

---

## 🎉 SUCCESS CRITERIA

Your app is ready when:
✅ All files compile without errors
✅ Gradle sync succeeds
✅ App installs on your phone
✅ Google Sign-In works
✅ Member list loads from your sheet
✅ You can mark attendance and save to sheet
✅ History screen shows past records

---

**TIPS FOR USING CLAUDE CODE:**

1. **Copy one prompt at a time** - Don't overwhelm Claude Code
2. **Wait for completion** - Let it finish before next prompt
3. **Review output** - Check files were created correctly
4. **Ask for fixes** - If something's wrong, ask Claude Code to fix it
5. **Be specific** - If you want changes, describe exactly what you need

**Good luck! Your app will be ready soon! 🚀**
