package com.example.awaytime.data

import android.app.AlarmManager
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

    fun scheduleReminder(context: Context, goal: DailyGoal) {
        if (goal.isCompleted) return

        val reminderTime = goal.targetTimeMillis - (10 * 60 * 1000L) // 10 minutes before
        val now = System.currentTimeMillis()

        if (goal.targetTimeMillis <= now) {
            // Already expired, no reminder needed
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, GoalReminderReceiver::class.java).apply {
            action = GoalReminderReceiver.ACTION_GOAL_REMINDER
            putExtra(GoalReminderReceiver.EXTRA_GOAL_ID, goal.id)
            putExtra(GoalReminderReceiver.EXTRA_GOAL_TITLE, goal.title)
            putExtra(GoalReminderReceiver.EXTRA_TARGET_TIME, goal.targetTimeMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            goal.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = if (reminderTime <= now) {
            // Target is less than 10 minutes away! Notify shortly (within 2 seconds)
            now + 2000L
        } else {
            reminderTime
        }

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
            Log.d(TAG, "Scheduled reminder for goal '${goal.title}' at $triggerAt (target: ${goal.targetTimeMillis})")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm with AlarmManager", e)
        }
    }

    fun cancelReminder(context: Context, goalId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, GoalReminderReceiver::class.java).apply {
                action = GoalReminderReceiver.ACTION_GOAL_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                goalId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Cancelled reminder for goal $goalId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling reminder for goal $goalId", e)
        }
    }

    fun rescheduleAllPendingReminders(context: Context) {
        val goals = getGoals(context)
        val now = System.currentTimeMillis()
        for (goal in goals) {
            if (!goal.isCompleted && goal.targetTimeMillis > now) {
                scheduleReminder(context, goal)
            }
        }
    }
}
