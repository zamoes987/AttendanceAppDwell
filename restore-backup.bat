@echo off
REM Quick Restore Script for Attendance Tracker App
REM This script helps you restore to a previous version

cd /d "%~dp0"

echo ============================================
echo Attendance Tracker - Restore Tool
echo ============================================
echo.
echo Recent backups (most recent first):
echo.

REM Show last 10 commits
git log --oneline -10

echo.
echo ============================================
echo.
echo To restore to a previous version:
echo   1. Find the commit ID from the list above (e.g., 4ebe106)
echo   2. Enter it below
echo.
echo WARNING: This will discard any uncommitted changes!
echo.

set /p commit_id="Enter commit ID to restore (or 'cancel' to exit): "

if /i "%commit_id%"=="cancel" (
    echo Restore cancelled.
    pause
    exit /b 0
)

if "%commit_id%"=="" (
    echo No commit ID entered. Restore cancelled.
    pause
    exit /b 0
)

echo.
echo ============================================
echo You are about to restore to commit: %commit_id%
echo.
echo This will:
echo   - Discard all uncommitted changes
echo   - Reset your code to the selected version
echo.
set /p confirm="Are you SURE you want to continue? (YES/no): "

if /i not "%confirm%"=="YES" (
    echo Restore cancelled for safety.
    echo (You must type 'YES' in all caps to confirm)
    pause
    exit /b 0
)

echo.
echo Restoring to commit %commit_id%...
git reset --hard %commit_id%

if %errorlevel% equ 0 (
    echo.
    echo ============================================
    echo ✓ Successfully restored to commit %commit_id%
    echo ============================================
    echo.
    echo Your code is now at the selected version.
    echo Rebuild the app in Android Studio to see the changes.
    echo.
) else (
    echo.
    echo ✗ Restore failed. Check the commit ID and try again.
    echo.
)

pause
