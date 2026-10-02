package com.example.awaytime.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.awaytime.data.AwayTimeManager

class ScreenEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                AwayTimeManager.recordScreenOff(context)
                refreshWidgets(context)
            }
            Intent.ACTION_SCREEN_ON -> {
                AwayTimeManager.recordScreenOn(context)
            }
            Intent.ACTION_USER_PRESENT -> {
                refreshWidgets(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                refreshWidgets(context)
            }
        }
    }

    private fun refreshWidgets(context: Context) {
        val refreshIntent = Intent("com.example.awaytime.ACTION_REFRESH_WIDGETS").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(refreshIntent)
    }
}
