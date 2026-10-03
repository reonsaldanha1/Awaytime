package com.example.awaytime.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import com.example.awaytime.service.DistractionNotificationListenerService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DistractionItem(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val isBlocked: Boolean = false,
    val colorLong: Long = 0xFF4F8DF7L,
    val count: Int = 1,
    val sbnKey: String = ""
) {
    fun formattedTime(): String {
        val diff = System.currentTimeMillis() - timestamp
        val mins = diff / (60 * 1000L)
        return when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            mins < 1440 -> "${mins / 60}h ago"
            else -> SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

object DistractionManager {
    private const val PREFS_NAME = "awaytime_distraction_prefs"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val KEY_NOTIFICATION_BLOCKER_ENABLED = "notification_blocker_enabled"
    private const val KEY_BLOCKER_MODE = "notification_blocker_mode" // "selected" or "all"

    // In-memory cache of recent notifications
    private val recentNotifications = mutableListOf<DistractionItem>()

    fun isNotificationBlockerEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATION_BLOCKER_ENABLED, true)
    }

    fun setNotificationBlockerEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATION_BLOCKER_ENABLED, enabled).apply()
    }

    fun getNotificationBlockerMode(context: Context): String {
        return getPrefs(context).getString(KEY_BLOCKER_MODE, "selected") ?: "selected"
    }

    fun setNotificationBlockerMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_BLOCKER_MODE, mode).apply()
    }

    fun hasNotificationListenerPermission(context: Context): Boolean {
        return try {
            val cn = ComponentName(context, DistractionNotificationListenerService::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            flat != null && flat.contains(cn.flattenToString())
        } catch (e: Exception) {
            false
        }
    }

    fun openNotificationListenerSettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBlockedPackages(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_BLOCKED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun isPackageBlocked(context: Context, packageName: String): Boolean {
        return getBlockedPackages(context).contains(packageName)
    }

    fun setPackageBlocked(context: Context, packageName: String, blocked: Boolean) {
        val current = getBlockedPackages(context).toMutableSet()
        if (blocked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        getPrefs(context).edit().putStringSet(KEY_BLOCKED_PACKAGES, current).apply()

        if (blocked) {
            DistractionNotificationListenerService.dismissNotificationsForPackage(packageName)
        }
    }

    fun getAllInstalledApps(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val list = mutableListOf<Pair<String, String>>()
        val seen = mutableSetOf<String>()
        for (info in resolveInfos) {
            val pkg = info.activityInfo?.packageName ?: continue
            if (pkg == context.packageName || pkg == "com.android.systemui") continue
            if (!seen.add(pkg)) continue
            val label = try {
                info.loadLabel(pm).toString()
            } catch (e: Exception) {
                pkg
            }
            list.add(pkg to label)
        }
        return list.sortedBy { it.second.lowercase() }
    }

    fun addNotification(context: Context, item: DistractionItem) {
        synchronized(recentNotifications) {
            // Check if there is an existing notification from the same package with similar content (e.g. charging or same title)
            val isCharging = item.title.contains("charging", ignoreCase = true) ||
                             item.text.contains("charging", ignoreCase = true) ||
                             item.title.contains("battery", ignoreCase = true) ||
                             item.text.contains("battery", ignoreCase = true)

            val existingIndex = recentNotifications.indexOfFirst { existing ->
                if (existing.packageName != item.packageName) return@indexOfFirst false
                if (isCharging) {
                    val exCharging = existing.title.contains("charging", ignoreCase = true) ||
                                     existing.text.contains("charging", ignoreCase = true) ||
                                     existing.title.contains("battery", ignoreCase = true) ||
                                     existing.text.contains("battery", ignoreCase = true)
                    if (exCharging) return@indexOfFirst true
                }
                // Also pile up if same title or same sbnKey
                (item.sbnKey.isNotBlank() && existing.sbnKey == item.sbnKey) ||
                (existing.title.equals(item.title, ignoreCase = true))
            }

            if (existingIndex != -1) {
                val existing = recentNotifications[existingIndex]
                val updatedCount = existing.count + 1
                // Replace with newest content/timestamp and bumped count
                val updated = item.copy(
                    id = existing.id,
                    count = updatedCount,
                    // If latest title/text is not empty use it, otherwise keep previous
                    title = if (item.title.isNotBlank()) item.title else existing.title,
                    text = if (item.text.isNotBlank()) item.text else existing.text,
                    timestamp = item.timestamp,
                    isBlocked = item.isBlocked
                )
                recentNotifications.removeAt(existingIndex)
                recentNotifications.add(0, updated)
            } else {
                recentNotifications.removeAll { it.id == item.id }
                recentNotifications.add(0, item)
            }

            if (recentNotifications.size > 100) {
                recentNotifications.removeAt(recentNotifications.lastIndex)
            }
        }
    }

    fun getDistractions(context: Context): List<DistractionItem> {
        val blocked = getBlockedPackages(context)

        synchronized(recentNotifications) {
            if (recentNotifications.isNotEmpty()) {
                return recentNotifications.map { it.copy(isBlocked = blocked.contains(it.packageName)) }
            }
        }

        // Return sample distractions if list is empty
        val now = System.currentTimeMillis()
        val samples = listOf(
            DistractionItem(
                id = "s1",
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "alice_w liked your story",
                text = "Tap to see Alice's updates and 2 other reactions.",
                timestamp = now - (12 * 60 * 1000L),
                isBlocked = blocked.contains("com.instagram.android"),
                colorLong = 0xFFE1306CL
            ),
            DistractionItem(
                id = "s2",
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                title = "New Video: Deep Focus Ambient Sounds",
                text = "10 hours of concentration music for study and focus.",
                timestamp = now - (28 * 60 * 1000L),
                isBlocked = blocked.contains("com.google.android.youtube"),
                colorLong = 0xFFFF0000L
            ),
            DistractionItem(
                id = "s3",
                packageName = "org.telegram.messenger",
                appName = "Telegram",
                title = "Design Community",
                text = "David: Has anyone tested the latest layout updates?",
                timestamp = now - (45 * 60 * 1000L),
                isBlocked = blocked.contains("org.telegram.messenger"),
                colorLong = 0xFF229ED9L
            ),
            DistractionItem(
                id = "s4",
                packageName = "com.supercell.clashofclans",
                appName = "Clash of Clans",
                title = "Your Village Needs You!",
                text = "Shield is running out in 15 minutes. Defend your base!",
                timestamp = now - (90 * 60 * 1000L),
                isBlocked = blocked.contains("com.supercell.clashofclans"),
                colorLong = 0xFFFFC107L
            ),
            DistractionItem(
                id = "s5",
                packageName = "com.twitter.android",
                appName = "X (Twitter)",
                title = "Trending in Technology",
                text = "See what developers and designers are discussing right now.",
                timestamp = now - (150 * 60 * 1000L),
                isBlocked = blocked.contains("com.twitter.android"),
                colorLong = 0xFF1DA1F2L
            )
        )

        return samples
    }
}
