package com.example.awaytime.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyGoal(
    val id: String,
    val title: String,
    val targetTimeMillis: Long,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isOverdue(): Boolean {
        return !isCompleted && System.currentTimeMillis() > targetTimeMillis
    }

    fun formattedTargetTime(): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(targetTimeMillis))
    }

    fun formattedReminderTime(): String {
        val reminderTime = targetTimeMillis - (10 * 60 * 1000L)
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(reminderTime))
    }

    fun remainingTimeText(): String {
        val diff = targetTimeMillis - System.currentTimeMillis()
        if (diff <= 0) {
            val overdueMins = (-diff) / 60000L
            return if (overdueMins < 60) "${overdueMins}m overdue" else "${overdueMins / 60}h ${overdueMins % 60}m overdue"
        }
        val mins = diff / 60000L
        val hours = mins / 60
        val remMins = mins % 60
        return when {
            hours > 0 && remMins > 0 -> "${hours}h ${remMins}m left"
            hours > 0 -> "${hours}h left"
            else -> "${remMins}m left"
        }
    }
}
