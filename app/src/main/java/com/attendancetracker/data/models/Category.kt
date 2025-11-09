package com.attendancetracker.data.models

/**
 * Enum representing different member categories in the attendance system.
 *
 * Each category has a display name for UI presentation and an abbreviation
 * that matches the format used in the Google Sheet.
 *
 * Categories:
 * - ORIGINAL_MEMBER (OM): Original members of the group
 * - XENOS_TRANSFER (XT): Members transferred from Xenos
 * - RETURNING_NEW (RN): Returning members who are new to the group
 * - FIRST_TIMER (FT): First-time attendees
 * - VISITOR (V): Visitors
 */
enum class Category(
    val displayName: String,
    val abbreviation: String
) {
    ORIGINAL_MEMBER("Original Member", "OM"),
    XENOS_TRANSFER("Xenos Transfer", "XT"),
    RETURNING_NEW("Returning New", "RN"),
    FIRST_TIMER("First Timer", "FT"),
    VISITOR("Visitor", "V");

    companion object {
        /**
         * Converts an abbreviation string to its corresponding Category enum value.
         *
         * @param abbr The abbreviation (e.g., "OM", "XT", "RN", "FT", "V")
         * @return The matching Category enum value, or null if no match is found
         *
         * Example:
         * ```
         * val category = Category.fromAbbreviation("OM") // Returns Category.ORIGINAL_MEMBER
         * ```
         */
        fun fromAbbreviation(abbr: String): Category? {
            return entries.find { it.abbreviation.equals(abbr, ignoreCase = true) }
        }

        /**
         * Returns a list of "regular" categories, excluding First Timers and Visitors.
         *
         * Regular categories are used for calculating core attendance metrics,
         * while FT and V are tracked separately as they represent temporary attendees.
         *
         * @return List of regular member categories (OM, XT, RN)
         */
        fun getRegularCategories(): List<Category> {
            return listOf(ORIGINAL_MEMBER, XENOS_TRANSFER, RETURNING_NEW)
        }
    }
}
