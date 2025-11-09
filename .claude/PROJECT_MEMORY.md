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
- **Authentication:** Google Sign-In
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
**Latest Commit:** 9bc7c1d  
**Commit Message:** "Update app launcher icon with Dwell CC logo from website"

### Features Implemented:
✅ Google Sheets authentication (OAuth 2.0)  
✅ Member list loading from Google Sheets  
✅ Attendance marking (check/uncheck members)  
✅ Save attendance to Google Sheets  
✅ Attendance history view  
✅ Category-based member grouping  
✅ Member management (add, edit, delete)  
✅ Sign-out functionality (Settings screen)  
✅ Date picker for marking past attendance  
✅ Color-coded categories  
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
- `MainActivity.kt` - App entry point, handles Google Sign-In
- `Navigation.kt` - Navigation graph

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
- `9bc7c1d` - Dwell CC logo icon (CURRENT)

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

### Issue 1: Sign-In Not Appearing
**Cause:** Cached credentials expired  
**Solution:** Use sign-out button in Settings screen

### Issue 2: No Data Loading
**Cause:** Authentication token invalid  
**Solution:** Sign out and sign back in

### Issue 3: Build Errors
**Cause:** Gradle cache corruption  
**Solution:** Build → Clean Project → Rebuild Project in Android Studio

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

**Last Updated:** Session ending with app icon update (commit 9bc7c1d)  
**Status:** ✅ STABLE - All features working, icon updated with Dwell CC logo
