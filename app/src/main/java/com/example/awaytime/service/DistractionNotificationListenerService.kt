package com.example.awaytime.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.awaytime.data.DistractionItem
import com.example.awaytime.data.DistractionManager

class DistractionNotificationListenerService : NotificationListenerService() {

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

            val pm = packageManager
            val appName = try {
                val ai = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                pkg
            }

            val isBlocked = DistractionManager.isPackageBlocked(this, pkg)

            // If the user blocked notifications for this app, cancel/dismiss it!
            if (isBlocked) {
                cancelNotification(sbn.key)
                Log.d("DistractionService", "Blocked incoming notification from $pkg: $title")
            }

            val item = DistractionItem(
                id = "${sbn.key}_${System.currentTimeMillis()}",
                packageName = pkg,
                appName = appName,
                title = title.ifBlank { appName },
                text = text,
                timestamp = System.currentTimeMillis(),
                isBlocked = isBlocked
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
