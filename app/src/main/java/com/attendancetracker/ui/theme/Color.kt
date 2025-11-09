package com.attendancetracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color definitions for the Attendance Tracker app.
 *
 * Follows Material 3 design guidelines with custom colors
 * for category distinction and status indication.
 */

// Primary Colors - Dwell CC Green Theme
val Primary = Color(0xFF48982C)          // Dwell Green
val PrimaryVariant = Color(0xFF2D5C1A)   // Dark Green
val Secondary = Color(0xFF66BB3D)        // Light Green
val SecondaryVariant = Color(0xFF48982C) // Dwell Green

// Background Colors
val BackgroundLight = Color(0xFFFAFAFA)  // Light gray
val BackgroundDark = Color(0xFF121212)   // Almost black
val SurfaceLight = Color(0xFFFFFFFF)     // White
val SurfaceDark = Color(0xFF1E1E1E)      // Dark gray

// Category Colors (for visual distinction in UI)
val CategoryOM = Color(0xFF48982C)       // Dwell Green - Original Member
val CategoryXT = Color(0xFF7B1FA2)       // Purple - Xenos Transfer
val CategoryRN = Color(0xFFF57C00)       // Orange - Returning New
val CategoryFT = Color(0xFF66BB3D)       // Light Green - First Timer
val CategoryV = Color(0xFF616161)        // Gray - Visitor

// Status Colors
val Present = Color(0xFF4CAF50)          // Green - Member is present
val Absent = Color(0xFFE0E0E0)           // Light gray - Member is absent
val ErrorRed = Color(0xFFD32F2F)         // Red - Error states
val WarningAmber = Color(0xFFFFA000)     // Amber - Warning states

// Text Colors
val TextPrimary = Color(0xFF212121)      // Dark gray - Primary text
val TextSecondary = Color(0xFF757575)    // Medium gray - Secondary text
val TextOnPrimary = Color(0xFFFFFFFF)    // White - Text on primary color
