package com.example.awaytime.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.awaytime.MainActivity
import com.example.awaytime.R
import com.example.awaytime.data.DailyGoalsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoalReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_GOAL_REMINDER = "com.example.awaytime.ACTION_GOAL_REMINDER"
        const val EXTRA_GOAL_ID = "extra_goal_id"
        const val EXTRA_GOAL_TITLE = "extra_goal_title"
        const val EXTRA_TARGET_TIME = "extra_target_time"
        const val CHANNEL_ID = "awaytime_goals_reminder_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_GOAL_REMINDER) return

        val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
        val goalTitle = intent.getStringExtra(EXTRA_GOAL_TITLE) ?: "Your Daily Goal"
        val targetTime = intent.getLongExtra(EXTRA_TARGET_TIME, 0L)

        // Verify goal is still incomplete
        val goals = DailyGoalsManager.getGoals(context)
        val goal = goals.firstOrNull { it.id == goalId }
        if (goal != null && goal.isCompleted) {
            // Already completed, do not send reminder
            return
        }

        val targetFormatted = if (targetTime > 0L) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(targetTime))
        } else {
            "soon"
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        createNotificationChannel(notificationManager)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_page", "goals")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            goalId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val diffMinutes = if (targetTime > System.currentTimeMillis()) {
            ((targetTime - System.currentTimeMillis()) / 60000L).coerceAtLeast(1L)
        } else {
            0L
        }

        val messageText = if (diffMinutes in 1..10) {
            "⏰ $diffMinutes minutes left to complete \"$goalTitle\" (Target: $targetFormatted)!"
        } else {
            "⏰ 10 minutes left to complete \"$goalTitle\" (Target: $targetFormatted)!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_target)
            .setContentTitle("Goal Reminder: 10 min left!")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$messageText\nStay focused and put your phone away to get it done on time."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(goalId.hashCode(), notification)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Goal Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sends 10-minute reminders for scheduled daily goals"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
