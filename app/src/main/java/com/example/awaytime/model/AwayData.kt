package com.example.awaytime.model

/**
 * Represents an interval where the screen was off (phone away) or on.
 */
data class ScreenInterval(
    val startMillis: Long,
    val endMillis: Long,
    val isAway: Boolean
)

/**
 * Daily statistics for time away from phone.
 */
data class DailyAwayStats(
    val dateLabel: String,
    val totalAwayMillis: Long,
    val totalScreenMillis: Long,
    val currentStreakMillis: Long,
    val intervals: List<ScreenInterval> = emptyList()
) {
    val awayHours: Int get() = (totalAwayMillis / (1000 * 60 * 60)).toInt()
    val awayMinutes: Int get() = ((totalAwayMillis / (1000 * 60)) % 60).toInt()

    fun formattedAwayTime(): String = "${awayHours}h ${awayMinutes}m"
}

/**
 * Day data point for the weekly chart (e.g. Day 6..12 or Mon..Sun).
 */
data class DayPoint(
    val dayNumber: String,
    val awayHours: Float
)

/**
 * Weekly statistics for time away from phone.
 */
data class WeeklyAwayStats(
    val dateRangeLabel: String,
    val totalAwayHours: Int,
    val totalAwayMinutes: Int,
    val averageHours: Int,
    val averageMinutes: Int,
    val dailyPoints: List<DayPoint>
) {
    fun formattedTotal(): String = "${totalAwayHours} hr ${totalAwayMinutes} min"
    fun formattedAverage(): String = "${averageHours} hr ${averageMinutes} min"
}

/**
 * Supported themes for widgets.
 */
enum class WidgetTheme(val displayName: String, val bgStartColor: Long, val bgEndColor: Long, val isDark: Boolean) {
    PITCH_DARK("Pitch Dark", 0xFF17181CL, 0xFF17181CL, true),
    RADIANT_BLUE("Radiant Blue", 0xFF3880ECL, 0xFF2160D4L, true),
    NOTHING_MONO("Nothing Mono", 0xFF101010L, 0xFF101010L, true),
    SOFT_MINT("Muted Slate", 0xFF22262EL, 0xFF1A1C22L, true)
}

/**
 * Accent colors for sparkle and indicators.
 */
enum class WidgetAccent(val displayName: String, val colorLong: Long) {
    CYBER_BLUE("Cyber Blue", 0xFF4F8DF7L),
    MINT_GREEN("Mint Green", 0xFF69E094L),
    CORAL_PEACH("Coral Peach", 0xFFFFAB91L),
    BUBBLE_PINK("Bubble Pink", 0xFFFF80ABL),
    SUN_YELLOW("Sun Yellow", 0xFFFFD54FL)
}

/**
 * Data representation for an individual app's daily usage.
 */
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageMillis: Long,
    val formattedDuration: String,
    val colorLong: Long,
    val categoryName: String = "App"
)

/**
 * Data representation for an app category's daily usage (e.g. Games, Social).
 */
data class AppCategoryUsage(
    val categoryName: String,
    val usageMillis: Long,
    val formattedDuration: String,
    val iconRes: Int,
    val colorLong: Long
)

/**
 * Complete daily digital wellbeing stats matching Image 2.
 */
data class DailyWellbeingData(
    val totalScreenMillis: Long,
    val formattedTotalScreenTime: String,
    val topApps: List<AppUsageInfo>,
    val categories: List<AppCategoryUsage>,
    val otherAppsMillis: Long
)

