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
        const val EXTRA_REMINDER_TYPE = "extra_reminder_type"

        const val TYPE_10_MIN_PRIOR = "reminder_10min"
        const val TYPE_ON_TIME = "reminder_on_time"

        const val CHANNEL_ID = "awaytime_goals_reminder_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_GOAL_REMINDER) return

        val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
        val goalTitle = intent.getStringExtra(EXTRA_GOAL_TITLE) ?: "Your Daily Goal"
        val targetTime = intent.getLongExtra(EXTRA_TARGET_TIME, 0L)
        val reminderType = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: TYPE_10_MIN_PRIOR

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
            "scheduled time"
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        createNotificationChannel(notificationManager)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_page", "goals")
        }

        val isDueReminder = (reminderType == TYPE_ON_TIME)
        val requestCode = if (isDueReminder) {
            (goalId + "_due").hashCode()
        } else {
            (goalId + "_10min").hashCode()
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = requestCode

        val (titleText, contentText, bigText) = if (isDueReminder) {
            val title = "Goal Due Now: $goalTitle"
            val content = "🎯 It's $targetFormatted! Time is up for your goal \"$goalTitle\"."
            val big = "🎯 It's $targetFormatted!\nTime is up for your goal \"$goalTitle\". Open Awaytime to mark it completed or review your streak!"
            Triple(title, content, big)
        } else {
            val diffMinutes = if (targetTime > System.currentTimeMillis()) {
                ((targetTime - System.currentTimeMillis()) / 60000L).coerceAtLeast(1L)
            } else {
                10L
            }
            val minText = if (diffMinutes in 1..10) "$diffMinutes minutes" else "10 minutes"
            val title = "Goal Reminder: 10 min left!"
            val content = "⏰ $minText left to complete \"$goalTitle\" (Target: $targetFormatted)!"
            val big = "⏰ $minText left to complete \"$goalTitle\" (Target: $targetFormatted)!\nStay focused and put your phone away to get it done on time."
            Triple(title, content, big)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_target)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Goal Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sends 10-minute and on-time deadline reminders for scheduled daily goals"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
