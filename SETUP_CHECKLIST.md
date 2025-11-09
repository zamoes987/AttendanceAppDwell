# Attendance Tracker App - Setup Checklist

Use this checklist to ensure you complete all setup steps correctly.

## ✅ Phase 1: Prerequisites

- [x ] Android Studio installed (latest version)
- [ x] Google account created
- [ x] Access to the attendance Google Sheet (Editor or Owner permission)
- [ x] Android device or emulator available (API 24+)

---

## ✅ Phase 2: Google Cloud Setup

### Google Cloud Project

- [ x] Navigate to [Google Cloud Console](https://console.cloud.google.com/)
- [ x] Create new project named "Attendance Tracker"
- [ x] Project created successfully

### Enable APIs

- [ x] Go to "APIs & Services" → "Library"
- [x ] Search for "Google Sheets API"
- [ x] Click "Enable"
- [ x] API enabled (green checkmark visible)

### OAuth Consent Screen

- [ x] Go to "APIs & Services" → "OAuth consent screen"
- [ x] Select "External" user type
- [ x] Fill in app name: "Attendance Tracker"
- [ x] Add user support email
- [ x] Add developer contact email
- [ x] Click "Save and Continue"
- [ x] Add scope: `https://www.googleapis.com/auth/spreadsheets`
- [ x] Add test users (your Google account email)
- [ x] OAuth consent screen configuration complete

### Get SHA-1 Fingerprint

**Choose one method:**

#### Method A: Android Studio (Recommended)
- [x ] Open Android Studio
- [x ] Open Gradle panel (right side)
- [x ] Navigate: `AttendanceTrackerApp` → `Tasks` → `android` → `signingReport`
- [x ] Double-click `signingReport`
- [ x] Copy SHA-1 from "debug" variant output
- [ ] SHA-1 copied: `3C:93:F9:21:13:B9:01:E6:0D:69:80:B5:B5:62:3C:AF:31:34:8E:E7`

#### Method B: Command Line
```bash
# Windows
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android

# Mac/Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```
- [ ] Command executed
- [ ] SHA-1 copied: `________________`

### Create OAuth Credentials

- [x ] Go to "APIs & Services" → "Credentials"
- [ x] Click "Create Credentials" → "OAuth client ID"
- [ x] Select application type: "Android"
- [ xx] Enter name: "Attendance Tracker Android"
- [ x] Enter package name: `com.attendancetracker`
- [x ] Paste SHA-1 certificate fingerprint
- [ z] Click "Create"
- [z ] OAuth credentials created

---

## ✅ Phase 3: Google Sheet Preparation

### Get Sheet ID

- [x ] Open your Google Sheet in browser
- [ x] Copy URL
- [ x] Extract Sheet ID from URL (between `/d/` and `/edit`)
- [x ] Sheet ID copied: `11M2RMedyD0pn0cve8MsgOVkXmNorOCSv80hRqMUaft0`

### Verify Sheet Structure

- [x ] Sheet has tab named "2025"
- [ x] Column A contains category labels or is empty
- [ x] Column B contains member names
- [ x] Column C contains status (OM, XT, RN, FT, or V)
- [x ] Header row exists (Row 1)
- [ x] Date columns start in Column D and use format "M/d/yy" (e.g., "1/16/25" or "01/16/25")

Example structure verified:
- [x ] First row is header (empty, "Name", "Status", dates...)
- [x ] Member data starts from Row 2
- [ x] At least one member exists
- [ x] Categories are correct abbreviations (OM, XT, RN, FT, V)

---

## ✅ Phase 4: Android Studio Setup

### Open Project

- [x ] Launch Android Studio
- [x ] Click "Open"
- [x ] Navigate to `C:\Users\zanee\AppDevAttendance`
- [ x] Select folder and click "OK"
- [x ] Wait for Gradle sync to complete
- [x ] No Gradle errors

### Configure Sheet ID

- [ x] Open `app/src/main/java/com/attendancetracker/data/api/GoogleSheetsService.kt`
- [ x] Find line: `private val SPREADSHEET_ID = "YOUR_SPREADSHEET_ID_HERE"`
- [x ] Replace with your actual Sheet ID
- [ x] Save file (Ctrl+S / Cmd+S)
- [x ] Sheet ID configured

### Verify Dependencies

- [x ] Open `app/build.gradle.kts`
- [x ] All dependencies resolved (no red underlines)
- [ x] Gradle sync successful

---

## ✅ Phase 5: Device Setup

Choose **Option A** (Physical Device) OR **Option B** (Emulator):

### Option A: Physical Android Device

- [ x] Go to device Settings → About Phone
- [ x] Tap "Build Number" 7 times
- [ x] Developer mode enabled
- [ x] Go to Settings → Developer Options
- [ x] Enable "USB Debugging"
- [ x] Connect device via USB
- [ x] Accept "Allow USB debugging" prompt
- [ x] Device appears in Android Studio device dropdown

### Option B: Android Emulator

- [ ] In Android Studio: Tools → Device Manager
- [ ] Click "Create Device"
- [ ] Select hardware (Pixel 5 recommended)
- [ ] Select system image (API 34 recommended)
- [ ] Download system image if needed
- [ ] Finish device creation
- [ ] Start emulator
- [ ] Emulator boots successfully

---

## ✅ Phase 6: Build and Run

### Build the App

- [ ] Select device from device dropdown
- [ ] Click green "Run" button (▶) or press Shift+F10
- [ ] Gradle build starts
- [ ] Build completes successfully (no errors)
- [ ] App installs on device
- [ ] App launches automatically

### First Launch

- [ ] Sign-in screen appears
- [ ] Click "Sign in with Google"
- [ ] Google account picker appears
- [ ] Select your Google account
- [ ] Grant requested permissions:
  - [ ] See and manage your spreadsheets
  - [ ] View your email address
- [ ] App navigates to Home screen

### Verify Functionality

- [ ] Member list loads successfully
- [ ] Members are grouped by category
- [ ] Category colors visible on cards
- [ ] Can check/uncheck members
- [ ] "Select All" button works
- [ ] "Clear All" button works
- [ ] Select by category works
- [ ] Count updates correctly (X / Y format)
- [ ] Click "Save Attendance" button
- [ ] Success message appears
- [ ] Click History icon (top-right)
- [ ] History screen loads
- [ ] Today's attendance record appears
- [ ] Category breakdown chips visible
- [ ] Back button returns to Home

---

## ✅ Phase 7: Test in Google Sheets

### Verify Attendance Was Written

- [ ] Open Google Sheet in browser
- [ ] Find today's date column (new column should be added)
- [ ] Check that "x" marks appear for selected members
- [ ] Empty cells for unselected members
- [ ] All rows match correctly

### Test Editing in Sheet

- [ ] Manually add "x" in sheet for a member
- [ ] Go back to app
- [ ] Click refresh button
- [ ] Verify member is now checked in app

---

## ✅ Phase 8: Final Verification

### App Features

- [ ] Dark mode works (change system setting)
- [ ] Navigation works smoothly
- [ ] No crashes during normal use
- [ ] Loading indicators appear during operations
- [ ] Error messages are clear and helpful
- [ ] Success messages auto-dismiss after 2 seconds

### Performance

- [ ] App loads within reasonable time (< 5 seconds)
- [ ] Smooth scrolling through member list
- [ ] No lag when selecting members
- [ ] Save operation completes quickly

---

## 🎉 Completion

### All Done!

- [ ] All checklist items completed
- [ ] App fully functional
- [ ] No blocking issues
- [ ] Ready for regular use

---

## 🆘 Troubleshooting

If you encountered issues, check:

- [ ] Reviewed README.md troubleshooting section
- [ ] Checked Android Studio Logcat for errors
- [ ] Verified Google Cloud OAuth settings
- [ ] Confirmed Sheet ID is correct
- [ ] Tested internet connection
- [ ] Restarted app and tried again

Common issues solved:
- [ ] Issue 1: ________________
- [ ] Issue 2: ________________
- [ ] Issue 3: ________________

---

## 📝 Notes

**My Setup Details:**
- Sheet ID: `________________`
- SHA-1 Fingerprint: `________________`
- Device Used: `________________`
- Android Studio Version: `________________`
- Setup Date: `________________`

**Additional Notes:**
```
[Write any additional notes or customizations here]
```

---

**Setup completed by:** ________________
**Date:** ________________
**Time taken:** ________________
