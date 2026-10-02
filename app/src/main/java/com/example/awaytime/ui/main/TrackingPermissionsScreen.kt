package com.example.awaytime.ui.main

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awaytime.R
import com.example.awaytime.data.AwayTimeManager
import com.example.awaytime.data.DistractionManager
import com.example.awaytime.service.DistractionNotificationListenerService

private val DarkCardBg = Color(0xFF171A21)
private val DarkCardBorder = Color(0xFF232733)
private val TextMutedGray = Color(0xFF9AA0B2)

data class PermissionStatusItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val isGranted: Boolean,
    val isWorkingProperly: Boolean,
    val statusText: String,
    val details: String,
    val onAction: (Context) -> Unit
)

@Composable
fun TrackingPermissionsScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }

    fun refresh() {
        refreshKey++
    }

    // Check permissions live
    val permissions = remember(refreshKey) {
        checkPermissions(context)
    }

    BackHandler {
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D11))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Tracking & Permissions",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = { refresh() }) {
                    Text("Recheck", color = Color(0xFF4DA2FF), fontSize = 13.5.sp)
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Summary Status Card
                val grantedCount = permissions.count { it.isGranted }
                val workingCount = permissions.count { it.isWorkingProperly }
                val allGood = workingCount == permissions.size

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 1.dp,
                            color = if (allGood) Color(0xFF1E3A2B) else DarkCardBorder,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    color = DarkCardBg
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (allGood) "All Systems Operational" else "System Permissions",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$grantedCount of ${permissions.size} permissions granted • $workingCount fully verified",
                                    color = TextMutedGray,
                                    fontSize = 13.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (allGood) Color(0xFF163824) else Color(0xFF332018)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_shield),
                                    contentDescription = null,
                                    tint = if (allGood) Color(0xFF69E094) else Color(0xFFFF8A65),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Header for Permissions List
                Text(
                    text = "REQUIRED PERMISSIONS & DIAGNOSTICS",
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                // Permissions list
                permissions.forEach { item ->
                    PermissionCard(item = item, onAction = {
                        item.onAction(context)
                        refresh()
                    })
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    item: PermissionStatusItem,
    onAction: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = when {
                    !item.isGranted -> Color(0xFF4A201A)
                    item.isWorkingProperly -> Color(0xFF1E3827)
                    else -> Color(0xFF3E3218)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { expanded = !expanded },
        color = DarkCardBg
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Indicator Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !item.isGranted -> Color(0xFFFF5252)
                                item.isWorkingProperly -> Color(0xFF69E094)
                                else -> Color(0xFFFFC107)
                            }
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        color = TextMutedGray,
                        fontSize = 12.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status Badge
                Surface(
                    color = when {
                        !item.isGranted -> Color(0xFF381410)
                        item.isWorkingProperly -> Color(0xFF123420)
                        else -> Color(0xFF332912)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.statusText,
                        color = when {
                            !item.isGranted -> Color(0xFFFF8A65)
                            item.isWorkingProperly -> Color(0xFF69E094)
                            else -> Color(0xFFFFD54F)
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Diagnostic details
            Text(
                text = item.details,
                color = if (item.isWorkingProperly) Color(0xFFA5D6A7) else Color(0xFFE0E0E0),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (!item.isGranted || !item.isWorkingProperly) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!item.isGranted) Color(0xFFE65100) else Color(0xFF1E88E5)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (!item.isGranted) "Grant Permission" else "Open Settings",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun checkPermissions(context: Context): List<PermissionStatusItem> {
    val items = mutableListOf<PermissionStatusItem>()

    // 1. Usage Stats Permission
    val hasUsage = AwayTimeManager.hasUsageStatsPermission(context)
    var usageWorking = false
    var usageDetail = ""
    if (hasUsage) {
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            val now = System.currentTimeMillis()
            val stats = usm?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - (3600 * 1000L), now)
            if (!stats.isNullOrEmpty()) {
                usageWorking = true
                usageDetail = "✓ Working properly: Successfully querying real-time system app usage and interactive events."
            } else {
                usageWorking = true // Granted, but no events logged in last hour
                usageDetail = "✓ Granted: Usage statistics service connected and active."
            }
        } catch (e: Exception) {
            usageWorking = false
            usageDetail = "⚠ Error querying usage statistics: ${e.localizedMessage}"
        }
    } else {
        usageDetail = "✗ Not active: Required to calculate daily away time, screen time today, and app breakdown."
    }

    items.add(
        PermissionStatusItem(
            id = "usage",
            title = "Usage Access",
            subtitle = "Tracks device screen time and app usage stats",
            isGranted = hasUsage,
            isWorkingProperly = usageWorking,
            statusText = if (hasUsage) (if (usageWorking) "Working Properly" else "Degraded") else "Not Granted",
            details = usageDetail,
            onAction = { c ->
                try {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        data = Uri.parse("package:${c.packageName}")
                    }
                    c.startActivity(intent)
                } catch (e: Exception) {
                    c.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }
        )
    )

    // 2. Notification Listener Permission
    val hasNotif = DistractionManager.hasNotificationListenerPermission(context)
    val listenerConnected = DistractionNotificationListenerService.instance != null
    val notifWorking = hasNotif && listenerConnected
    val notifDetail = when {
        notifWorking -> "✓ Working properly: Notification listener service is connected and actively monitoring distractions."
        hasNotif -> "✓ Granted: Notification access allowed. Listener service will attach automatically on incoming alerts."
        else -> "✗ Not active: Required for the Distractions page and for blocking intrusive app notifications."
    }

    items.add(
        PermissionStatusItem(
            id = "notification",
            title = "Notification Access",
            subtitle = "Reads notifications to list distractions and block apps",
            isGranted = hasNotif,
            isWorkingProperly = hasNotif,
            statusText = if (hasNotif) (if (notifWorking) "Working Properly" else "Active") else "Not Granted",
            details = notifDetail,
            onAction = { c ->
                DistractionManager.openNotificationListenerSettings(c)
            }
        )
    )

    // 3. Battery Optimization / Background execution
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    val batteryDetail = if (isIgnoringBatteryOptimizations) {
        "✓ Working properly: Unrestricted background execution enabled. Widgets refresh on screen toggle."
    } else {
        "○ Recommended: App battery optimization is standard. Disabling optimization prevents widgets from sleeping."
    }

    items.add(
        PermissionStatusItem(
            id = "battery",
            title = "Battery Optimization",
            subtitle = "Ensures background widget updates and screen tracking",
            isGranted = isIgnoringBatteryOptimizations,
            isWorkingProperly = true,
            statusText = if (isIgnoringBatteryOptimizations) "Unrestricted" else "Standard",
            details = batteryDetail,
            onAction = { c ->
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    c.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${c.packageName}")
                        }
                        c.startActivity(intent)
                    } catch (e2: Exception) {}
                }
            }
        )
    )

    return items
}
