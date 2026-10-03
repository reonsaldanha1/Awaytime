package com.example.awaytime.data

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.example.awaytime.model.DailyGoal
import com.example.awaytime.service.GoalReminderReceiver
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object DailyGoalsManager {
    private const val TAG = "DailyGoalsManager"
    private const val PREFS_NAME = "awaytime_daily_goals_prefs"
    private const val KEY_GOALS_JSON = "daily_goals_list_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun getGoals(context: Context): List<DailyGoal> {
        val jsonStr = getPrefs(context).getString(KEY_GOALS_JSON, null) ?: return emptyList()
        val list = mutableListOf<DailyGoal>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    DailyGoal(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        targetTimeMillis = obj.getLong("targetTimeMillis"),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing goals JSON", e)
        }
        return list.sortedWith(compareBy({ it.isCompleted }, { it.targetTimeMillis }))
    }

    @Synchronized
    fun saveGoals(context: Context, goals: List<DailyGoal>) {
        try {
            val jsonArray = JSONArray()
            for (goal in goals) {
                val obj = JSONObject().apply {
                    put("id", goal.id)
                    put("title", goal.title)
                    put("targetTimeMillis", goal.targetTimeMillis)
                    put("isCompleted", goal.isCompleted)
                    put("createdAt", goal.createdAt)
                }
                jsonArray.put(obj)
            }
            getPrefs(context).edit().putString(KEY_GOALS_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving goals JSON", e)
        }
    }

    fun addGoal(context: Context, title: String, targetTimeMillis: Long): DailyGoal {
        val newGoal = DailyGoal(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            targetTimeMillis = targetTimeMillis,
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        val currentList = getGoals(context).toMutableList()
        currentList.add(newGoal)
        saveGoals(context, currentList)

        // Schedule 10-minute reminder
        scheduleReminder(context, newGoal)
        return newGoal
    }

    fun toggleGoalCompleted(context: Context, goalId: String) {
        val currentList = getGoals(context).map { goal ->
            if (goal.id == goalId) {
                val updated = goal.copy(isCompleted = !goal.isCompleted)
                if (updated.isCompleted) {
                    cancelReminder(context, goal.id)
                } else if (!updated.isOverdue()) {
                    scheduleReminder(context, updated)
                }
                updated
            } else {
                goal
            }
        }
        saveGoals(context, currentList)
    }

    fun deleteGoal(context: Context, goalId: String) {
        cancelReminder(context, goalId)
        val currentList = getGoals(context).filter { it.id != goalId }
        saveGoals(context, currentList)
    }

    fun scheduleReminder(context: Context, goal: DailyGoal, allowImmediatePreReminder: Boolean = true) {
        if (goal.isCompleted) return

        val now = System.currentTimeMillis()
        if (goal.targetTimeMillis <= now) {
            // Already expired, no reminder needed
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Schedule 10-Minute Prior Reminder
        val reminder10MinTime = goal.targetTimeMillis - (10 * 60 * 1000L)
        if (reminder10MinTime > now) {
            scheduleSingleAlarm(
                context = context,
                alarmManager = alarmManager,
                goal = goal,
                triggerAt = reminder10MinTime,
                reminderType = GoalReminderReceiver.TYPE_10_MIN_PRIOR,
                requestCode = (goal.id + "_10min").hashCode()
            )
            Log.d(TAG, "Scheduled 10-min reminder for goal '${goal.title}' at $reminder10MinTime")
        } else if (allowImmediatePreReminder && goal.targetTimeMillis - now > 60_000L) {
            // Target is within 10 minutes but more than 1 min away: notify shortly
            scheduleSingleAlarm(
                context = context,
                alarmManager = alarmManager,
                goal = goal,
                triggerAt = now + 2000L,
                reminderType = GoalReminderReceiver.TYPE_10_MIN_PRIOR,
                requestCode = (goal.id + "_10min").hashCode()
            )
            Log.d(TAG, "Scheduled immediate 10-min reminder for goal '${goal.title}' at ${now + 2000L}")
        }

        // 2. Schedule On-Time Reminder (at target deadline)
        if (goal.targetTimeMillis > now) {
            scheduleSingleAlarm(
                context = context,
                alarmManager = alarmManager,
                goal = goal,
                triggerAt = goal.targetTimeMillis,
                reminderType = GoalReminderReceiver.TYPE_ON_TIME,
                requestCode = (goal.id + "_due").hashCode()
            )
            Log.d(TAG, "Scheduled on-time reminder for goal '${goal.title}' at ${goal.targetTimeMillis}")
        }
    }

    private fun scheduleSingleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        goal: DailyGoal,
        triggerAt: Long,
        reminderType: String,
        requestCode: Int
    ) {
        val intent = Intent(context, GoalReminderReceiver::class.java).apply {
            action = GoalReminderReceiver.ACTION_GOAL_REMINDER
            putExtra(GoalReminderReceiver.EXTRA_GOAL_ID, goal.id)
            putExtra(GoalReminderReceiver.EXTRA_GOAL_TITLE, goal.title)
            putExtra(GoalReminderReceiver.EXTRA_TARGET_TIME, goal.targetTimeMillis)
            putExtra(GoalReminderReceiver.EXTRA_REMINDER_TYPE, reminderType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm with AlarmManager for type $reminderType", e)
        }
    }

    fun cancelReminder(context: Context, goalId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, GoalReminderReceiver::class.java).apply {
                action = GoalReminderReceiver.ACTION_GOAL_REMINDER
            }

            // Cancel 10-minute prior alarm
            val prePendingIntent = PendingIntent.getBroadcast(
                context,
                (goalId + "_10min").hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (prePendingIntent != null) {
                alarmManager.cancel(prePendingIntent)
                prePendingIntent.cancel()
            }

            // Cancel on-time alarm
            val duePendingIntent = PendingIntent.getBroadcast(
                context,
                (goalId + "_due").hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (duePendingIntent != null) {
                alarmManager.cancel(duePendingIntent)
                duePendingIntent.cancel()
            }

            // Also cancel legacy single alarm
            val legacyPendingIntent = PendingIntent.getBroadcast(
                context,
                goalId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (legacyPendingIntent != null) {
                alarmManager.cancel(legacyPendingIntent)
                legacyPendingIntent.cancel()
            }

            // Clear any active notifications for this goal
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel((goalId + "_10min").hashCode())
            notificationManager?.cancel((goalId + "_due").hashCode())
            notificationManager?.cancel(goalId.hashCode())

            Log.d(TAG, "Cancelled all reminders and notifications for goal $goalId")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling reminder for goal $goalId", e)
        }
    }

    fun rescheduleAllPendingReminders(context: Context) {
        val goals = getGoals(context)
        val now = System.currentTimeMillis()
        for (goal in goals) {
            if (!goal.isCompleted && goal.targetTimeMillis > now) {
                scheduleReminder(context, goal, allowImmediatePreReminder = false)
            }
        }
    }
}
