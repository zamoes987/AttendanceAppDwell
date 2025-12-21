# Attendance Tracker

A native Android application for tracking meeting attendance with Google Sheets integration. Built with Kotlin, Jetpack Compose, and Material 3 design.

## Features

### Core Functionality
- **Attendance Marking** - Mark members present with checkboxes, save to Google Sheets
- **Date Selection** - Material 3 date picker, defaults to current meeting day
- **Bulk Operations** - Select All / Clear All / category-based selection
- **Member Management** - Add, edit, and delete members with category assignment

### Analytics & History
- **Statistics Dashboard** - Overall metrics, category comparisons, attendance trends
- **Attendance History** - View past meeting records with date filtering
- **Streak Tracking** - Current and longest attendance streaks per member
- **Trend Analysis** - Visualize improving/declining attendance patterns

### Authentication & Security
- **Google Sign-In** - OAuth 2.0 authentication with Google Sheets API
- **Biometric Authentication** - Optional fingerprint/face unlock
- **Session Management** - Secure 24-hour sessions with automatic refresh
- **Encrypted Storage** - Credentials stored in EncryptedSharedPreferences

### Notifications
- **Scheduled Reminders** - Thursday morning (8 AM) and evening (7 PM) notifications
- **Boot Persistence** - Alarms survive device restarts

### Design
- **Material 3 UI** - Modern Material You design language
- **Dark Mode** - Full light/dark theme support
- **Responsive Layout** - Works on phones and tablets

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Architecture | MVVM with Clean Architecture |
| Design System | Material 3 |
| Backend | Google Sheets API v4 |
| Auth | Google Sign-In + OAuth 2.0 |
| Async | Kotlin Coroutines & StateFlow |
| Navigation | Jetpack Navigation Compose |
| Storage | DataStore + EncryptedSharedPreferences |
| Biometrics | AndroidX Biometric |

## Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** or higher
- **Android Device/Emulator** API 24+ (Android 7.0+)
- **Google Cloud Account** for API credentials
- **Google Sheet** with proper structure (see below)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/zamoes987/AttendanceAppDwell.git
cd AttendanceAppDwell
```

### 2. Google Cloud Setup

1. Create a project at [Google Cloud Console](https://console.cloud.google.com/)
2. Enable **Google Sheets API**
3. Configure **OAuth consent screen** (External, add Sheets scope)
4. Create **Android OAuth 2.0 credentials**:
   - Package name: `com.attendancetracker`
   - SHA-1 fingerprint: Get via `./gradlew signingReport`
5. Add test users (your Google account)

### 3. Configure the App

Edit `app/src/main/java/com/attendancetracker/data/api/GoogleSheetsService.kt`:

```kotlin
private val SPREADSHEET_ID = "YOUR_SPREADSHEET_ID_HERE"  // Replace with your Sheet ID
```

Find your Sheet ID in the URL: `https://docs.google.com/spreadsheets/d/SHEET_ID_HERE/edit`

### 4. Build and Run

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Or use Android Studio's Run button.

## Google Sheet Structure

Create a tab named "2025" (or current year) with this structure:

| Column A | Column B | Column C | Column D+ |
|----------|----------|----------|-----------|
| *(category marker)* | Member Name | Category | Date Headers (M/d/yy) |
| OM | John Smith | OM | x |
| OM | Jane Doe | OM | x |
| XT | Alex Johnson | XT | |
| V | New Person | V | x |

- **Column A**: Category markers (OM, XT, FT/RN, V) - used for visual grouping
- **Column B**: Member names
- **Column C**: Category abbreviations (OM, XT, RN, FT, V)
- **Column D+**: Date columns with "x" for present, empty for absent

### Category Codes
- **OM** - Original Member
- **XT** - Transfer
- **RN** - Returning New
- **FT** - First Timer
- **V** - Visitor

## Project Structure

```
app/src/main/java/com/attendancetracker/
├── MainActivity.kt                    # Entry point, auth flow
├── data/
│   ├── api/
│   │   └── GoogleSheetsService.kt     # Sheets API operations
│   ├── auth/
│   │   ├── AuthManager.kt             # Session management
│   │   └── BiometricHelper.kt         # Biometric auth
│   ├── models/
│   │   ├── Member.kt                  # Member data class
│   │   ├── AttendanceRecord.kt        # Meeting attendance
│   │   ├── Category.kt                # Category enum
│   │   └── Statistics.kt              # Analytics models
│   ├── notifications/
│   │   ├── NotificationHelper.kt      # Alarm scheduling
│   │   ├── NotificationReceiver.kt    # Alarm handler
│   │   └── BootReceiver.kt            # Boot persistence
│   └── repository/
│       ├── SheetsRepository.kt        # Data abstraction
│       └── PreferencesRepository.kt   # Settings storage
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt              # Main attendance UI
│   │   ├── HistoryScreen.kt           # Past meetings
│   │   ├── StatisticsScreen.kt        # Analytics dashboard
│   │   ├── MembersScreen.kt           # Member management
│   │   ├── SettingsScreen.kt          # App settings
│   │   └── SignInScreen.kt            # Google sign-in
│   ├── components/                    # Reusable UI components
│   ├── theme/                         # Material 3 theme
│   └── Navigation.kt                  # Nav graph
└── viewmodel/
    ├── AttendanceViewModel.kt         # Main state management
    └── SettingsViewModel.kt           # Settings state
```

## Customization Guide

This app is designed to be easily adapted for your organization.

### Branding (look for `TODO: CUSTOMIZATION` comments)

| File | What to Change |
|------|----------------|
| `ui/screens/HomeScreen.kt` | Organization name in TopAppBar |
| `ui/screens/SignInScreen.kt` | Sign-in screen branding |
| `ui/screens/SettingsScreen.kt` | About section organization name |
| `ui/theme/Color.kt` | Primary/secondary brand colors |

### Configuration

| File | What to Change |
|------|----------------|
| `data/api/GoogleSheetsService.kt` | `SPREADSHEET_ID` and `currentYearTab` |
| `ui/screens/HomeScreen.kt` | External submission URL (if needed) |

### Meeting Schedule

Notification times are configured in `data/notifications/NotificationHelper.kt`:
- `scheduleMorningNotification()` - Currently Thursday 8 AM
- `scheduleEveningNotification()` - Currently Thursday 7 PM

## Troubleshooting

### "Failed to load members"
- Verify Sheet ID is correct
- Ensure sheet has a tab matching `currentYearTab` (default: "2025")
- Check member data exists in columns B and C

### Sign-In Issues
- Verify SHA-1 fingerprint in Google Cloud matches your keystore
- Ensure Google account is added as a test user in OAuth consent screen
- Try clearing app data and signing in again

### API Errors
- Confirm Google Sheets API is enabled in Cloud Console
- Check your Google account has edit access to the sheet
- Look for detailed errors in Android Studio Logcat

### Build Errors
- Ensure JDK 17 is configured
- Run `./gradlew clean` then rebuild
- File > Invalidate Caches in Android Studio

## Known Limitations

- Requires internet connection for most operations (no offline mode)
- Single spreadsheet support (multi-spreadsheet would require code changes)
- Year-based sheet tabs require annual tab creation
- Test users limit in External OAuth mode (publish app for production)

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Uses [Google Sheets API](https://developers.google.com/sheets/api)
- Design follows [Material 3 Guidelines](https://m3.material.io/)
- AI-assisted development with Claude Code
