@echo off
REM Quick Backup Script for Attendance Tracker App
REM This script creates a timestamped backup commit

cd /d "%~dp0"

echo ============================================
echo Attendance Tracker - Quick Backup Tool
echo ============================================
echo.

REM Check if there are changes
git diff --quiet
if %errorlevel% equ 0 (
    echo No changes detected since last commit.
    echo Your code is already backed up!
    echo.
    pause
    exit /b 0
)

REM Show what has changed
echo Changes detected:
echo.
git status --short
echo.

REM Ask for backup confirmation
set /p confirm="Create backup of these changes? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo Backup cancelled.
    pause
    exit /b 0
)

REM Get backup description
set /p description="Brief description of changes (or press Enter for timestamp only): "

if "%description%"=="" (
    REM Create timestamp-based commit
    for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c-%%a-%%b)
    for /f "tokens=1-2 delims=/:" %%a in ('time /t') do (set mytime=%%a%%b)
    set message=Backup - %mydate% %mytime%
) else (
    set message=Backup: %description%
)

REM Create the backup
echo.
echo Creating backup...
git add .
git commit -m "%message%"

if %errorlevel% equ 0 (
    echo.
    echo ============================================
    echo ✓ Backup created successfully!
    echo ============================================
    echo.
    echo To restore this backup later, use:
    echo   git log --oneline
    echo   git reset --hard [commit-id]
    echo.
) else (
    echo.
    echo ✗ Backup failed. Check for errors above.
    echo.
)

pause
