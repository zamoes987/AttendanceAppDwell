# Version Control & Backup Guide

This guide explains how to use Git version control to maintain stable versions of your app and recover from issues.

## What is Git?

Git is a version control system that tracks changes to your code. It allows you to:
- Save snapshots of your code at any point in time
- Revert to previous working versions
- Track what changed and when
- Create backups of stable versions

## Your Current Setup

✅ Git repository initialized
✅ Initial stable version committed (commit: 4ebe106)
✅ All source code tracked

## Essential Git Commands

### 1. Check Status
See what files have changed:
```bash
cd "C:\Users\zanee\AppDevAttendance"
git status
```

### 2. Save Your Changes (Commit)

**When to commit:**
- After fixing a bug
- After adding a new feature
- When the app is in a stable, working state
- Before making major changes

**How to commit:**
```bash
# Stage all changes
git add .

# Create a commit with a descriptive message
git commit -m "Brief description of what changed"
```

**Example commit messages:**
- `git commit -m "Fix: Resolved sign-in authentication issue"`
- `git commit -m "Feature: Added member deletion confirmation dialog"`
- `git commit -m "Update: Improved error handling in GoogleSheetsService"`

### 3. View History
See all previous commits:
```bash
git log --oneline
```

This shows:
- Commit ID (e.g., 4ebe106)
- Commit message
- Date and author

### 4. Revert to a Previous Version

**To undo recent changes (not yet committed):**
```bash
# Discard all uncommitted changes and go back to last commit
git reset --hard HEAD
```

**To go back to a specific commit:**
```bash
# First, find the commit ID from git log
git log --oneline

# Then reset to that commit
git reset --hard <commit-id>

# Example:
# git reset --hard 4ebe106
```

**⚠️ WARNING:** `git reset --hard` permanently deletes uncommitted changes. Use with caution!

### 5. Create a Backup Branch

Before making risky changes, create a backup branch:
```bash
# Create a backup of your current state
git branch backup-stable-version

# List all branches
git branch
```

To switch back to the backup:
```bash
git checkout backup-stable-version
```

## Recommended Workflow

### Daily Workflow
1. **Morning:** Check status
   ```bash
   git status
   ```

2. **After making changes:** Commit frequently
   ```bash
   git add .
   git commit -m "Description of changes"
   ```

3. **End of day:** Review what you committed
   ```bash
   git log --oneline
   ```

### Before Major Changes
1. Create a backup branch:
   ```bash
   git branch backup-before-feature-X
   ```

2. Make your changes

3. If something breaks:
   ```bash
   git checkout backup-before-feature-X
   ```

## Recovery Scenarios

### Scenario 1: "I broke something, but haven't committed yet"
```bash
# Discard all changes since last commit
git reset --hard HEAD
```

### Scenario 2: "I committed broken code"
```bash
# View history to find the last good commit
git log --oneline

# Go back to that commit
git reset --hard <good-commit-id>
```

### Scenario 3: "I want to see what changed"
```bash
# See all uncommitted changes
git diff

# See changes in a specific file
git diff app/src/main/java/com/attendancetracker/MainActivity.kt
```

### Scenario 4: "I want to restore just one file"
```bash
# Restore a single file from last commit
git checkout HEAD -- path/to/file.kt

# Example:
# git checkout HEAD -- app/src/main/java/com/attendancetracker/MainActivity.kt
```

## Creating Tagged Releases

When you have a particularly stable version, tag it:
```bash
# Create a tag for the current version
git tag -a v1.0-stable -m "Stable version with Google Sheets integration"

# List all tags
git tag

# Go back to a tagged version
git checkout v1.0-stable
```

## Best Practices

### ✅ DO:
- Commit frequently (multiple times per day)
- Write clear commit messages
- Commit when the app is in a working state
- Create tags for major stable versions
- Check `git status` before committing

### ❌ DON'T:
- Commit broken code (unless documenting an issue)
- Write vague commit messages ("fixed stuff", "updates")
- Go weeks without committing
- Delete the .git folder (this is your entire history)

## Quick Reference Card

| Task | Command |
|------|---------|
| Check what changed | `git status` |
| Save changes | `git add . && git commit -m "message"` |
| View history | `git log --oneline` |
| Undo uncommitted changes | `git reset --hard HEAD` |
| Go to previous version | `git reset --hard <commit-id>` |
| Create backup branch | `git branch backup-name` |
| Switch to branch | `git checkout branch-name` |
| Tag a version | `git tag -a v1.0 -m "description"` |

## Setting Up Remote Backup (Optional)

To back up to GitHub/GitLab:

1. Create a repository on GitHub
2. Add the remote:
   ```bash
   git remote add origin <repository-url>
   ```
3. Push your code:
   ```bash
   git push -u origin master
   ```

## Your Stable Baseline

**Current stable commit:** 4ebe106
**Description:** Initial stable version with:
- Google Sheets integration working
- Sign-out functionality
- All screens functional
- No compilation errors

If you ever need to return to this exact state:
```bash
git reset --hard 4ebe106
```

## Getting Help

- View this guide: Open `VERSION_CONTROL_GUIDE.md`
- Git documentation: https://git-scm.com/doc
- In case of emergency: `git reset --hard <last-known-good-commit>`

---

**Remember:** Git is your safety net. Commit often, and you'll never lose your work!
