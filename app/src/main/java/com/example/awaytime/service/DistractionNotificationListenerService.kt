package com.example.awaytime.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.awaytime.data.DistractionItem
import com.example.awaytime.data.DistractionManager
class DistractionNotificationListenerService : NotificationListenerService() {

    companion object {
        var instance: DistractionNotificationListenerService? = null
            private set

        fun dismissNotificationsForPackage(packageName: String) {
            val service = instance ?: return
            try {
                val activeSbns = service.activeNotifications ?: return
                for (sbn in activeSbns) {
                    if (sbn.packageName == packageName) {
                        service.cancelNotification(sbn.key)
                        Log.d("DistractionService", "Actively dismissed existing notification for $packageName")
                    }
                }
            } catch (e: Exception) {
                Log.e("DistractionService", "Error actively dismissing notifications for $packageName", e)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.d("DistractionService", "DistractionNotificationListenerService connected")
        // Dismiss any existing active notifications from currently blocked packages
        try {
            val activeSbns = activeNotifications ?: return
            for (sbn in activeSbns) {
                val pkg = sbn.packageName ?: continue
                if (DistractionManager.isPackageBlocked(this, pkg)) {
                    cancelNotification(sbn.key)
                    Log.d("DistractionService", "Dismissed already-active notification on connect: $pkg")
                }
            }
        } catch (e: Exception) {
            Log.e("DistractionService", "Error scanning active notifications on connect", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return // Ignore own notifications

        try {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            // Skip empty/ongoing system foreground notifications if not useful
            if (title.isBlank() && text.isBlank()) return

            val blockerEnabled = DistractionManager.isNotificationBlockerEnabled(this)
            val isPackageBlocked = DistractionManager.isPackageBlocked(this, pkg)
            val mode = DistractionManager.getNotificationBlockerMode(this)
            val isEssential = pkg == "com.android.dialer" || pkg == "com.google.android.dialer" || pkg == "com.android.phone" || pkg == "com.android.server.telecom"
            val isBlocked = blockerEnabled && (isPackageBlocked || (mode == "all" && !isEssential))

            // If the user blocked notifications for this app, cancel/dismiss it!
            if (isBlocked) {
                cancelNotification(sbn.key)
                // Also attempt cancelNotification with tag and id
                try {
                    cancelNotification(sbn.packageName, sbn.tag, sbn.id)
                } catch (e: Exception) {}
                Log.d("DistractionService", "Blocked incoming notification from $pkg: $title")
            }

            val pm = packageManager
            val appName = try {
                val ai = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                pkg
            }

            val item = DistractionItem(
                id = "${sbn.key}_${System.currentTimeMillis()}",
                packageName = pkg,
                appName = appName,
                title = title.ifBlank { appName },
                text = text,
                timestamp = System.currentTimeMillis(),
                isBlocked = isBlocked,
                sbnKey = sbn.key
            )

            DistractionManager.addNotification(this, item)

            // Notify UI
            val updateIntent = Intent("com.example.awaytime.ACTION_DISTRACTIONS_UPDATED").apply {
                setPackage(packageName)
            }
            sendBroadcast(updateIntent)

        } catch (e: Exception) {
            Log.e("DistractionService", "Error processing notification", e)
        }
    }
}
