# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Attendance Tracker is a native Android application for tracking meeting attendance with Google Sheets integration. Built with Kotlin, Jetpack Compose, and Material 3 design. The app uses Google Sheets as the single source of truth for all attendance data.

## Build Commands

### Building the App
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean
```

### Running Tests
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

### Installation
```bash
# Install debug APK to connected device
./gradlew installDebug

# Uninstall from device
"$USERPROFILE/AppData/Local/Android/Sdk/platform-tools/adb.exe" -s R5CX41RRY0M uninstall com.attendancetracker

# Install specific APK
"$USERPROFILE/AppData/Local/Android/Sdk/platform-tools/adb.exe" -s R5CX41RRY0M install "$USERPROFILE/AppDevAttendance/app/build/outputs/apk/debug/app-debug.apk"
```

### Gradle Tasks
```bash
# List all available tasks
./gradlew tasks

# Generate signing report (for SHA-1 fingerprint)
./gradlew signingReport
```

## Architecture

### MVVM Pattern with Clean Architecture

```
UI Layer (Composables)
    ↓
ViewModel (State Management)
    ↓
Repository (Data Abstraction)
    ↓
API Service / Local Storage
    ↓
Google Sheets API / Encrypted SharedPreferences
```

### Key Components

**Data Layer** (`app/src/main/java/com/attendancetracker/data/`)
- `models/`: Core data models (Member, AttendanceRecord, Category, AppSettings)
- `api/GoogleSheetsService.kt`: All Google Sheets API interactions
- `repository/SheetsRepository.kt`: Main data repository with StateFlow management
- `repository/PreferencesRepository.kt`: DataStore-based settings management
- `auth/AuthManager.kt`: Session management with encrypted storage
- `auth/BiometricHelper.kt`: Biometric authentication wrapper

**UI Layer** (`app/src/main/java/com/attendancetracker/ui/`)
- `screens/`: Full-screen composables (HomeScreen, HistoryScreen, MembersScreen, SettingsScreen, SignInScreen)
- `components/`: Reusable UI components (MemberListItem, CategoryHeader, CommonComponents)
- `theme/`: Material 3 theme definitions (Color, Theme, Type)
- `Navigation.kt`: Navigation graph setup

**ViewModel Layer** (`app/src/main/java/com/attendancetracker/viewmodel/`)
- `AttendanceViewModel.kt`: Primary ViewModel managing members, attendance records, and UI state
- `SettingsViewModel.kt`: Settings screen state management

### Data Flow

**Reading Data (Google Sheets → UI):**
1. `SheetsRepository.loadAllData()` calls `GoogleSheetsService.readMembers()` and `readAllAttendance()`
2. Google Sheets API returns raw sheet data
3. Service parses into Member and AttendanceRecord objects
4. Repository updates StateFlows
5. ViewModel collects flows and transforms into UI state
6. Composables collect state and recompose automatically

**Writing Data (UI → Google Sheets):**
1. User marks attendance and clicks Save
2. `ViewModel.saveAttendance()` calls `Repository.saveAttendance()`
3. `GoogleSheetsService.writeAttendance()` writes "x" marks to appropriate cells
4. Repository updates local cache
5. StateFlows emit new values
6. UI updates reactively

### State Management

Uses **Kotlin StateFlow** for reactive state updates:
- Repository exposes `StateFlow<List<Member>>`, `StateFlow<List<AttendanceRecord>>`, etc.
- ViewModel collects these flows and transforms them
- UI composables use `.collectAsState()` for automatic recomposition
- Single source of truth pattern - repository maintains all state

### Authentication Flow

1. **App Launch**: Check `AuthManager.isAuthenticated()` and valid Google account
2. **Valid Session**: Show biometric prompt (if enabled) → Initialize app
3. **Invalid Session**: Show SignInScreen
4. **Sign-In**: Use Google Sign-In with OAuth 2.0 requesting Sheets API scope
5. **Post-Auth**: Save to encrypted storage, initialize repository, start 30-minute session refresh

**Session Management:**
- 24-hour session duration stored in EncryptedSharedPreferences
- Fallback to standard SharedPreferences if encryption unavailable
- Automatic refresh every 30 minutes while app is active

**Biometric Authentication:**
- Optional layer on top of Google account authentication
- Uses AndroidX Biometric library
- Supports fingerprint, face recognition, and device credentials

## Google Sheets Integration

### Sheet Structure
- **Spreadsheet ID**: Currently `11M2RMedyD0pn0cve8MsgOVkXmNorOCSv80hRqMUaft0` (configured in `GoogleSheetsService.kt`)
- **Tab Name**: "2025" (year-based)
- **Layout**:
  - Column A: Category markers (OM, XT, FT/RN, V)
  - Column B: Member names
  - Column C: Category abbreviations (OM, XT, RN, FT, V)
  - Columns D+: Date headers (format "M/d/yy") with "x" for present, empty for absent

### API Operations
All implemented in `GoogleSheetsService.kt`:
- `readMembers()`: Reads columns B:C to get member list
- `readAllAttendance(members)`: Reads entire sheet to populate attendance history
- `writeAttendance(date, presentMemberIds)`: Writes/updates attendance for specific date
- `addMember(name, category)`: Inserts new row in appropriate category section
- `updateMember(member)`: Updates name/category for existing member
- `deleteMember(member)`: Deletes row from sheet

### Authentication
- Uses `GoogleAccountCredential` with Sheets API scope
- Credential linked to signed-in Google account email
- All API calls run on IO dispatcher (`withContext(Dispatchers.IO)`)

## Configuration

### Changing Spreadsheet
Edit `GoogleSheetsService.kt`:
```kotlin
private val SPREADSHEET_ID = "YOUR_SPREADSHEET_ID_HERE"
private const val SHEET_NAME = "2025"  // Year-based tab
```

### Google Cloud Setup Required
- OAuth 2.0 client ID configured for package `com.attendancetracker`
- SHA-1 fingerprint registered (get via `./gradlew signingReport`)
- Google Sheets API enabled
- Scope: `https://www.googleapis.com/auth/spreadsheets`

## Development Notes

### Manual Dependency Injection
No DI framework (Dagger/Hilt) used. Dependencies created manually in `MainActivity.kt`:
```kotlin
val authManager = AuthManager(context)
val repository = SheetsRepository(context, userEmail)
val viewModel = AttendanceViewModel(repository)
```

### Deprecated API Usage
Uses deprecated `startActivityForResult` for Google Sign-In to avoid 16-bit request code limitation with ActivityResultContracts. This is intentional and documented in code comments.

### Error Handling
- `Result<T>` return types for all repository/service operations
- Flow-based error state in repository (`StateFlow<String?>`)
- UI displays errors via `ErrorMessage` composable
- Errors auto-clear after user dismissal

### Navigation
Single-Activity architecture with Navigation Compose:
- Routes: `home` (start), `history`, `settings`, `members`
- Shared ViewModel across home/history/members screens
- Settings has separate ViewModel

### Key Files to Modify

**To change Google Sheets integration:**
- `data/api/GoogleSheetsService.kt`: API calls and sheet structure logic

**To modify attendance logic:**
- `data/models/AttendanceRecord.kt`: Attendance tracking model
- `viewmodel/AttendanceViewModel.kt`: UI state and business logic
- `ui/screens/HomeScreen.kt`: Main attendance marking interface

**To update authentication:**
- `data/auth/AuthManager.kt`: Session management
- `data/auth/BiometricHelper.kt`: Biometric authentication
- `MainActivity.kt`: Initial auth flow

**To change UI/theme:**
- `ui/theme/`: Color, Theme, Type definitions
- `ui/components/`: Reusable components
- `ui/screens/`: Screen implementations

### Build Configuration
- **Target SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Java Version**: 17
- **Compose BOM**: 2023.10.01
- **Kotlin Version**: Matches Android Gradle Plugin version

### Dependencies
Key libraries:
- Jetpack Compose (UI framework)
- Material 3 (design system)
- Google Sheets API v4 (data backend)
- Google Play Services Auth (OAuth)
- AndroidX Security (encrypted storage)
- AndroidX Biometric (biometric auth)
- Kotlin Coroutines (async operations)
- DataStore (preferences storage)

## Testing Notes

No comprehensive test suite currently implemented. When adding tests:
- Unit tests: Place in `app/src/test/`
- Instrumented tests: Place in `app/src/androidTest/`
- Mock `GoogleSheetsService` for repository tests
- Use Compose test utilities for UI tests (`ui-test-junit4`)
