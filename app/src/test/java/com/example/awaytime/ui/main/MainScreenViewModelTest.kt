package com.example.awaytime.ui.main

import com.example.awaytime.data.AwayTimeManager
import com.example.awaytime.model.DailyAwayStats
import com.example.awaytime.model.ScreenInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AwayTimeTest {
    @Test
    fun testDailyAwayStatsFormatting() {
        val stats = DailyAwayStats(
            dateLabel = "Today",
            totalAwayMillis = (18 * 3600 + 45 * 60) * 1000L,
            totalScreenMillis = (5 * 3600 + 15 * 60) * 1000L,
            currentStreakMillis = 30 * 60 * 1000L,
            intervals = listOf(
                ScreenInterval(0L, 1000L, isAway = true)
            )
        )
        assertEquals(18, stats.awayHours)
        assertEquals(45, stats.awayMinutes)
        assertEquals("18h 45m", stats.formattedAwayTime())
    }

    @Test
    fun testAwayDurationCalculation() {
        val dayMillis = 24 * 3600 * 1000L
        val screenMillis = 4 * 3600 * 1000L + 30 * 60 * 1000L
        val awayMillis = (dayMillis - screenMillis).coerceIn(0L, dayMillis)
        val awayHours = (awayMillis / (1000 * 60 * 60)).toInt()
        val awayMinutes = ((awayMillis / (1000 * 60)) % 60).toInt()
        assertEquals(19, awayHours)
        assertEquals(30, awayMinutes)
    }

    @Test
    fun testDurationFormatting() {
        val formatted = AwayTimeManager.formatDuration(3 * 3600000L + 25 * 60000L)
        assertEquals("3 h 25 m", formatted)
    }
}
