# Attendance Tracker App - Project Memory

## Project Overview
**App Name:** Attendance Tracker  
**Organization:** Dwell Community Church  
**Purpose:** Track attendance for Thursday night Exodus IV meetings  
**Platform:** Android (Kotlin + Jetpack Compose)  
**Developer:** Zanee  
**Project Path:** `C:\Users\zanee\AppDevAttendance`

## Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Build System:** Gradle (Kotlin DSL)
- **API Integration:** Google Sheets API v4
- **Authentication:** Google Sign-In with persistent sessions (24-hour)
- **Security:** EncryptedSharedPreferences (AES-256), Biometric authentication
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

## Google Sheets Integration
**Spreadsheet ID:** `11M2RMedyD0pn0cve8MsgOVkXmNorOCSv80hRqMUaft0`  
**Spreadsheet URL:** https://docs.google.com/spreadsheets/d/11M2RMedyD0pn0cve8MsgOVkXmNorOCSv80hRqMUaft0/edit?gid=702208671#gid=702208671  
**Current Year Tab:** "2025"

### Sheet Structure:
- **Column A:** Category labels (optional)
- **Column B:** Member names
- **Column C:** Status/Category (OM, XT, RN, FT, V)
- **Column D onwards:** Date columns (format: M/d/yy)
- **Row 1:** Header row
- **Row 2+:** Member data

### Categories:
- **OM** (Old Married) - Color: Blue
- **XT** (XT) - Color: Purple
- **RN** (RN) - Color: Red
- **FT** (FT) - Color: Orange
- **V** (V) - Color: Green

## Current Status (as of latest commit)

### Stable Version: ✅ WORKING
**Latest Commit:** 4d96287
**Commit Message:** "Fix: Use deprecated startActivityForResult to avoid 16-bit request code error"

### Features Implemented:
✅ Google Sheets authentication (OAuth 2.0)
✅ **Persistent authentication (24-hour sessions)** ⭐ NEW
✅ **Biometric authentication (fingerprint/face unlock)** ⭐ NEW
✅ **Encrypted credential storage (AES-256)** ⭐ NEW
✅ **Automatic session refresh every 30 minutes** ⭐ NEW
✅ Member list loading from Google Sheets
✅ Attendance marking (check/uncheck members)
✅ Save attendance to Google Sheets
✅ Attendance history view
✅ Category-based member grouping
✅ Member management (add, edit, delete)
✅ Sign-out functionality (Settings screen)
✅ Date picker for marking past attendance
✅ Color-coded categories
✅ Comprehensive error handling with detailed logging
✅ Success/error message handling
✅ App icon with Dwell CC logo
✅ Git version control setup

### Screens:
1. **SignInScreen** - Google authentication
2. **HomeScreen** - Main attendance marking (with date picker)
3. **HistoryScreen** - View past attendance records
4. **MembersScreen** - Manage member list
5. **SettingsScreen** - App configuration + sign-out

## Key Files & Locations

### Main Code:
- `MainActivity.kt` - App entry point, handles Google Sign-In (uses FragmentActivity for biometric support)
- `Navigation.kt` - Navigation graph

### Authentication & Security:
- `AuthManager.kt` - Session management, encrypted storage, 24-hour authentication
- `BiometricHelper.kt` - Biometric authentication (fingerprint/face)

### Data Layer:
- `GoogleSheetsService.kt` - All Google Sheets API calls
- `SheetsRepository.kt` - Repository pattern, data management
- `PreferencesRepository.kt` - App settings storage

### ViewModels:
- `AttendanceViewModel.kt` - Main app logic
- `SettingsViewModel.kt` - Settings management

### UI Screens:
- `app/src/main/java/com/attendancetracker/ui/screens/`

### Resources:
- **App Icon:** `app/src/main/res/mipmap-*/ic_launcher.png`
- **Colors:** `app/src/main/java/com/attendancetracker/ui/theme/Color.kt`
- **Theme:** Dwell green primary color (#48982C)

## Version Control

### Git Setup: ✅ Configured
**Repository:** Initialized in project root  
**Git Config:**
- User: Zanee
- Email: zanee@dwellcc.local

### Important Commits:
- `4ebe106` - Initial stable version (BASELINE)
- `e2b60ad` - Version control documentation
- `9697f0f` - Backup/restore scripts
- `545658f` - Backup quick start guide
- `9bc7c1d` - Dwell CC logo icon
- `3e8df1f` - Add persistent auth system and biometric support
- `a5c2e89` - Fix MainActivity to extend FragmentActivity for biometric
- `b7f4d3a` - Fix Navigation.kt import order
- `c8e5a1b` - Add USE_BIOMETRIC permission and enhanced error handling
- `4d96287` - Fix Google Sign-In using startActivityForResult (CURRENT)

### Backup Tools:
- `create-backup.bat` - Easy backup creation
- `restore-backup.bat` - Easy restore to previous version
- `VERSION_CONTROL_GUIDE.md` - Complete Git guide
- `BACKUP_QUICK_START.md` - Quick reference

## Build & Deployment

### Device Setup:
- Physical Android device (R5CX41RRY0M)
- USB debugging enabled
- Deployment via Android Studio

### Build Commands:
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install to device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Quick Deploy (approved):
```bash
"$USERPROFILE/AppData/Local/Android/Sdk/platform-tools/adb.exe" -s R5CX41RRY0M uninstall com.attendancetracker
"$USERPROFILE/AppData/Local/Android/Sdk/platform-tools/adb.exe" -s R5CX41RRY0M install "$USERPROFILE/AppDevAttendance/app/build/outputs/apk/debug/app-debug.apk"
```

## Known Issues & Solutions

### Issue 1: Sign-In "can only use lower 16 bits for request code" Error (FIXED)
**Cause:** FragmentActivity's legacy fragment management conflicts with modern ActivityResultContracts API
**Solution:** Use deprecated `startActivityForResult` with request code 9001 (commit 4d96287)
**Technical Details:** FragmentActivity reserves upper bits for fragment management, requiring 16-bit request codes. Modern API generates codes that can exceed this limit. The deprecated method is the correct solution when using FragmentActivity (required for biometric auth).

### Issue 2: Sign-In Not Appearing
**Cause:** Cached credentials expired
**Solution:** Use sign-out button in Settings screen

### Issue 3: No Data Loading
**Cause:** Authentication token invalid
**Solution:** Sign out and sign back in

### Issue 4: Build Errors
**Cause:** Gradle cache corruption
**Solution:** Build → Clean Project → Rebuild Project in Android Studio

### Issue 5: Biometric Prompt Not Showing
**Cause:** Biometric not enrolled on device or hardware unavailable
**Solution:** App gracefully falls back to normal access without biometric

## Google Cloud Configuration

### OAuth Setup:
- Project: "Attendance Tracker"
- API Enabled: Google Sheets API
- OAuth Consent Screen: External
- Scopes: `https://www.googleapis.com/auth/spreadsheets`
- Package Name: `com.attendancetracker`
- SHA-1 Fingerprint: Configured for debug keystore

## Recent Changes Log

### Session 1 (Initial Setup):
- Fixed MembersScreen.kt compilation errors (missing Box import)
- Added sign-out functionality
- Set up Git version control
- Created backup/restore scripts

### Session 2 (App Icon):
- Downloaded Dwell CC logo from dwellcc.org
- Created launcher icons for all densities
- Updated adaptive icons (Android 8.0+)
- Maintained Dwell green background

### Session 3 (Stability & Authentication):
- **Implemented persistent authentication** - 24-hour sessions with automatic refresh
- **Added biometric support** - Fingerprint/face unlock with graceful fallback
- **Implemented encrypted storage** - AES-256 encryption for credentials
- **Enhanced error handling** - Comprehensive logging and user-friendly error messages
- **Fixed MainActivity** - Changed to FragmentActivity for biometric compatibility
- **Fixed Navigation.kt** - Corrected import order
- **Added biometric permission** - USE_BIOMETRIC in AndroidManifest
- **Fixed sign-in error** - Resolved "16-bit request code" issue using startActivityForResult
- **Added dependencies** - androidx.biometric and androidx.security libraries

## Development Workflow

### Making Changes:
1. Make code changes in Android Studio
2. Test the app
3. If working: Double-click `create-backup.bat`
4. Enter description of changes
5. Commit is automatically created

### Reverting Changes:
1. Double-click `restore-backup.bat`
2. Select commit ID from list
3. Type `YES` to confirm
4. Rebuild in Android Studio

### Emergency Recovery:
```bash
cd "C:\Users\zanee\AppDevAttendance"
git reset --hard 4ebe106  # Return to initial stable version
```

## Dependencies (build.gradle.kts)

### Core:
- androidx.core:core-ktx:1.12.0
- androidx.lifecycle:lifecycle-runtime-ktx:2.7.0
- androidx.activity:activity-compose:1.8.2

### Compose:
- compose-bom:2023.10.01
- material3, material-icons-extended

### Security & Authentication:
- androidx.biometric:biometric:1.2.0-alpha05 ⭐ NEW
- androidx.security:security-crypto:1.1.0-alpha06 ⭐ NEW

### Google APIs:
- google-api-services-sheets:v4-rev20230815-2.0.0
- google-auth-library-oauth2-http:1.19.0
- play-services-auth:20.7.0
- google-api-client-android:2.2.0

### Coroutines:
- kotlinx-coroutines-android:1.7.3
- kotlinx-coroutines-play-services:1.7.3

## Testing Checklist

Before committing changes:
- [ ] App builds successfully
- [ ] App installs on device
- [ ] Sign-in works
- [ ] Members load from Google Sheets
- [ ] Can check/uncheck members
- [ ] Save attendance works
- [ ] No crashes in normal use
- [ ] No compilation errors

## Future Enhancements (Ideas)

Potential features to add:
- [ ] Offline mode with sync
- [ ] Export attendance reports
- [ ] Notifications for Thursday meetings
- [ ] Member statistics/trends
- [ ] Bulk attendance operations
- [ ] QR code check-in

## Important Notes

1. **NEVER commit broken code** - Only create backups when app is working
2. **Test before committing** - Always verify changes work
3. **Use clear commit messages** - Future you will thank you
4. **Spreadsheet ID is hardcoded** - Update in GoogleSheetsService.kt if sheet changes
5. **Sign-out exists** - Settings screen has sign-out button for auth issues
6. **Green color is brand** - Dwell CC green (#48982C) used throughout

## Contact & Resources

- **Dwell CC Website:** https://www.dwellcc.org/
- **Documentation:** See VERSION_CONTROL_GUIDE.md
- **Quick Start:** See BACKUP_QUICK_START.md
- **Setup Guide:** See SETUP_CHECKLIST.md

---

**Last Updated:** Session 3 ending with stability improvements (commit 4d96287)
**Status:** ✅ STABLE - All features working, persistent auth, biometric support, enhanced error handling

### Authentication System Details:

**Session Management:**
- 24-hour authentication sessions stored in encrypted SharedPreferences
- Automatic session refresh every 30 minutes in background
- Session validation checks both auth state and timestamp
- Graceful fallback if encryption unavailable

**Biometric Authentication:**
- Fingerprint and face recognition support
- Uses BIOMETRIC_STRONG or DEVICE_CREDENTIAL
- Only prompts if enabled in Settings and hardware available
- Gracefully falls back to regular app access on error/failure

**Sign-In Implementation:**
- Uses deprecated `startActivityForResult` due to FragmentActivity requirement
- Request code: 9001 (within 16-bit limit)
- Comprehensive error handling with specific error codes
- Detailed logging for debugging authentication issues
