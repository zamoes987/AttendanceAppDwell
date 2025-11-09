# Attendance Tracker App

A native Android application for tracking meeting attendance with Google Sheets integration. Built with Kotlin, Jetpack Compose, and Material 3 design.

## Features

- **Google Sheets Integration**: Read and write attendance data directly to/from Google Sheets
- **Category-Based Organization**: Organize members by category (OM, XT, RN, FT, V)
- **Real-time Updates**: See attendance changes immediately
- **Attendance History**: View past meeting records with statistics
- **Material 3 Design**: Modern UI with dark mode support
- **Quick Actions**: Bulk select by category, select all, or clear all

## Prerequisites

Before you begin, ensure you have:

- **Android Studio** (latest version recommended)
- **Google Account** with access to your attendance Google Sheet
- **Google Cloud Account** for API credentials setup
- **Android Device or Emulator** (API Level 24 / Android 7.0 or higher)

## Google Cloud Setup

### 1. Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click "Select a project" → "New Project"
3. Enter project name: "Attendance Tracker"
4. Click "Create"

### 2. Enable Google Sheets API

1. In your Google Cloud project, go to "APIs & Services" → "Library"
2. Search for "Google Sheets API"
3. Click on it and click "Enable"

### 3. Configure OAuth Consent Screen

1. Go to "APIs & Services" → "OAuth consent screen"
2. Select "External" user type
3. Fill in required fields:
   - App name: "Attendance Tracker"
   - User support email: Your email
   - Developer contact: Your email
4. Click "Save and Continue"
5. On Scopes page, click "Add or Remove Scopes"
6. Add the scope: `https://www.googleapis.com/auth/spreadsheets`
7. Click "Save and Continue"
8. Add test users (your Google account email)
9. Click "Save and Continue"

### 4. Create Android OAuth Credentials

1. Go to "APIs & Services" → "Credentials"
2. Click "Create Credentials" → "OAuth client ID"
3. Application type: "Android"
4. Name: "Attendance Tracker Android"
5. Package name: `com.attendancetracker`
6. **SHA-1 certificate fingerprint**: (See "Get SHA-1 Fingerprint" section below)
7. Click "Create"

### 5. Get SHA-1 Fingerprint

**Method 1: Using Android Studio**
1. Open your project in Android Studio
2. Open the Gradle panel (right side)
3. Navigate to: `AttendanceTrackerApp` → `Tasks` → `android` → `signingReport`
4. Double-click `signingReport`
5. Look for "SHA1" in the output (under "debug" variant)
6. Copy this SHA-1 fingerprint

**Method 2: Using Command Line**
```bash
# Windows
cd C:\Users\[YourUsername]\.android
keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android

# Mac/Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Look for "SHA1:" in the output and copy the fingerprint.

### 6. Get Your Google Sheet ID

1. Open your Google Sheet in a web browser
2. Look at the URL: `https://docs.google.com/spreadsheets/d/YOUR_SHEET_ID_HERE/edit`
3. Copy the long string between `/d/` and `/edit` - this is your Sheet ID

## Project Setup

### 1. Clone or Extract the Project

```bash
cd C:\Users\zanee\AppDevAttendance
```

### 2. Open in Android Studio

1. Open Android Studio
2. Click "Open" and select the `AppDevAttendance` folder
3. Wait for Gradle sync to complete

### 3. Configure Google Sheet ID

1. Open `app/src/main/java/com/attendancetracker/data/api/GoogleSheetsService.kt`
2. Find the line:
   ```kotlin
   private val SPREADSHEET_ID = "YOUR_SPREADSHEET_ID_HERE"
   ```
3. Replace `"YOUR_SPREADSHEET_ID_HERE"` with your actual Google Sheet ID (keep the quotes)

Example:
```kotlin
private val SPREADSHEET_ID = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms"
```

### 4. Verify Google Sheet Structure

Your Google Sheet should have a tab named "2025" with this structure:

| Column A | Column B | Column C (Date 1) | Column D (Date 2) | ... |
|----------|----------|-------------------|-------------------|-----|
| Name     | Status   | 1/2/25            | 1/9/25            | ... |
| John Doe | OM       | x                 |                   | ... |
| Jane Smith | XT     | x                 | x                 | ... |

- **Column A**: Member names
- **Column B**: Category abbreviation (OM, XT, RN, FT, or V)
- **Columns C+**: Dates in format "M/d/yy" with "x" marking attendance

## Building and Running

### 1. Connect Your Android Device

**Option A: Physical Device**
1. Enable Developer Options on your Android device:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Go back to Settings → Developer Options
2. Enable "USB Debugging"
3. Connect device via USB
4. Allow USB debugging when prompted

**Option B: Emulator**
1. In Android Studio: Tools → Device Manager
2. Create a new virtual device (Pixel 5 recommended)
3. Select system image (API 34 recommended)
4. Finish setup and start emulator

### 2. Build and Run

1. In Android Studio, select your device from the device dropdown
2. Click the green "Run" button (or press Shift+F10)
3. Wait for the app to build and install
4. The app should launch automatically

### 3. Sign In

1. Click "Sign in with Google"
2. Select your Google account
3. Grant permissions:
   - Access to Google Sheets
   - Access to your account info
4. You should see the attendance screen with your member list

## Troubleshooting

### "Failed to load members" Error
- Verify your Sheet ID is correct in `GoogleSheetsService.kt`
- Ensure the sheet has a tab named "2025"
- Check that columns A and B contain member names and categories

### Sign-In Fails
- Verify SHA-1 fingerprint is correctly added to Google Cloud OAuth credentials
- Ensure you're using the same Google account that has access to the sheet
- Try revoking and re-granting permissions

### "API not enabled" Error
- Go to Google Cloud Console and ensure Google Sheets API is enabled
- Wait a few minutes after enabling (can take time to propagate)

### Build Errors
- Ensure you're using JDK 17
- File → Invalidate Caches → Invalidate and Restart
- Clean project: Build → Clean Project
- Rebuild: Build → Rebuild Project

### Permission Denied
- Ensure your Google account has Editor or Owner access to the sheet
- Check OAuth consent screen includes the correct scope
- Verify test users includes your Google account

## Project Structure

```
app/src/main/java/com/attendancetracker/
├── MainActivity.kt                 # App entry point
├── data/
│   ├── models/
│   │   ├── Category.kt            # Member category enum
│   │   ├── Member.kt              # Member data class
│   │   └── AttendanceRecord.kt    # Attendance record data class
│   ├── api/
│   │   └── GoogleSheetsService.kt # Google Sheets API integration
│   └── repository/
│       └── SheetsRepository.kt    # Repository layer
├── ui/
│   ├── screens/
│   │   ├── SignInScreen.kt        # Google sign-in screen
│   │   ├── HomeScreen.kt          # Main attendance marking screen
│   │   └── HistoryScreen.kt       # Attendance history screen
│   ├── components/
│   │   ├── MemberListItem.kt      # Member card component
│   │   ├── CategoryHeader.kt      # Category header component
│   │   └── CommonComponents.kt    # Reusable UI components
│   ├── theme/
│   │   ├── Color.kt               # Color definitions
│   │   ├── Theme.kt               # Material 3 theme
│   │   └── Type.kt                # Typography definitions
│   └── Navigation.kt              # Navigation setup
└── viewmodel/
    └── AttendanceViewModel.kt     # ViewModel for UI state
```

## Technologies Used

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Design**: Material 3
- **Backend**: Google Sheets API v4
- **Authentication**: Google Sign-In
- **Async**: Kotlin Coroutines & Flow
- **Navigation**: Jetpack Navigation Compose

## License

This project is created for educational and internal use.

## Support

For issues or questions:
1. Check the Troubleshooting section above
2. Review Google Cloud Console for API/auth issues
3. Verify Google Sheet structure and permissions
4. Check Android Studio Logcat for detailed error messages
