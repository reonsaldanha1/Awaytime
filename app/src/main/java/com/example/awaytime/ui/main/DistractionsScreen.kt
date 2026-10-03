package com.example.awaytime.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.awaytime.data.DistractionItem
import com.example.awaytime.data.DistractionManager
import com.example.awaytime.theme.PastelCoral

private val DarkCardBg = Color(0xFF171A21)
private val DarkCardBorder = Color(0xFF232733)
private val TextMutedGray = Color(0xFF9AA0B2)

@Composable
fun DistractionsScreen(
    onDismiss: () -> Unit,
    onNavigateToAppTimers: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(DistractionManager.hasNotificationListenerPermission(context)) }
    var distractions by remember { mutableStateOf(DistractionManager.getDistractions(context)) }
    var selectedTab by remember { mutableStateOf("notifications") } // "notifications" or "blocked"
    var isBlockerEnabled by remember { mutableStateOf(DistractionManager.isNotificationBlockerEnabled(context)) }
    var blockerMode by remember { mutableStateOf(DistractionManager.getNotificationBlockerMode(context)) }
    var showAddAppBlockDialog by remember { mutableStateOf(false) }

    fun refresh() {
        hasPermission = DistractionManager.hasNotificationListenerPermission(context)
        distractions = DistractionManager.getDistractions(context)
        isBlockerEnabled = DistractionManager.isNotificationBlockerEnabled(context)
        blockerMode = DistractionManager.getNotificationBlockerMode(context)
    }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                refresh()
            }
        }
        val filter = android.content.IntentFilter("com.example.awaytime.ACTION_DISTRACTIONS_UPDATED")
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {}
        }
    }

    BackHandler {
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D11))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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
                    text = "Distractions",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { openNotificationListenerSettings(context) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bell),
                        contentDescription = "Settings",
                        tint = if (hasPermission) Color(0xFF69E094) else Color(0xFFFF8A65),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Permission Card if not granted
                if (!hasPermission) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, Color(0xFF5A3022), RoundedCornerShape(20.dp)),
                            color = Color(0xFF281816)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Notification Reading Permission Needed",
                                    color = Color(0xFFFFAB91),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "To track distracting notifications and actively suppress unwanted alerts, grant Notification Access.",
                                    color = Color(0xFFD7CCC8),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { openNotificationListenerSettings(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Grant Permission", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Master Notification Blocker Card
                item {
                    val blockedPackages = DistractionManager.getBlockedPackages(context)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, if (isBlockerEnabled) Color(0xFF324738) else DarkCardBorder, RoundedCornerShape(24.dp)),
                        color = DarkCardBg
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(if (isBlockerEnabled) Color(0xFF1B3828) else Color(0xFF232733)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_bell_off),
                                            contentDescription = null,
                                            tint = if (isBlockerEnabled) Color(0xFF69E094) else TextMutedGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Notification Blocker",
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isBlockerEnabled) "Active • Suppressing alerts" else "Paused",
                                            color = if (isBlockerEnabled) Color(0xFF69E094) else TextMutedGray,
                                            fontSize = 12.5.sp
                                        )
                                    }
                                }

                                Switch(
                                    checked = isBlockerEnabled,
                                    onCheckedChange = {
                                        isBlockerEnabled = it
                                        DistractionManager.setNotificationBlockerEnabled(context, it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF69E094)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Silently dismiss incoming notifications from selected or distracting apps to keep your attention on what matters.",
                                color = TextMutedGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Mode Selector Chips: Block Selected vs Block All
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            blockerMode = "selected"
                                            DistractionManager.setNotificationBlockerMode(context, "selected")
                                        },
                                    color = if (blockerMode == "selected") Color(0xFF233229) else Color(0xFF141720),
                                    border = if (blockerMode == "selected") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF69E094)) else null,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Block Selected (${blockedPackages.size})",
                                            color = if (blockerMode == "selected") Color.White else TextMutedGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            blockerMode = "all"
                                            DistractionManager.setNotificationBlockerMode(context, "all")
                                        },
                                    color = if (blockerMode == "all") Color(0xFF3A201A) else Color(0xFF141720),
                                    border = if (blockerMode == "all") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF8A65)) else null,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Block All Distractions",
                                            color = if (blockerMode == "all") Color.White else TextMutedGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Add App to Blocklist Button
                            OutlinedButton(
                                onClick = { showAddAppBlockDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A364A))
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_bell_off),
                                    contentDescription = null,
                                    tint = Color(0xFF64B5F6),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("+ Add App to Blocklist", color = Color(0xFF64B5F6), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // App Timers & Limits Navigation Banner
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.dp, Color(0xFF382329), RoundedCornerShape(22.dp))
                            .clickable { onNavigateToAppTimers() },
                        color = Color(0xFF1E1418)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF351921)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_timer),
                                    contentDescription = null,
                                    tint = PastelCoral,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App Screen Time Timers",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Set daily limits. When reached, Awaytime blocks the app.",
                                    color = Color(0xFFD1B4BC),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = onNavigateToAppTimers,
                                colors = ButtonDefaults.buttonColors(containerColor = PastelCoral),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Configure →", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Filter Tabs: Recent Alerts / Blocked Apps
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterTab(
                            title = "Recent Alerts (${distractions.size})",
                            isSelected = selectedTab == "notifications",
                            onClick = { selectedTab = "notifications" },
                            modifier = Modifier.weight(1f)
                        )
                        FilterTab(
                            title = "Blocked Apps (${DistractionManager.getBlockedPackages(context).size})",
                            isSelected = selectedTab == "blocked",
                            onClick = { selectedTab = "blocked" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (selectedTab == "notifications") {
                    items(distractions) { item ->
                        val isBlocked = DistractionManager.isPackageBlocked(context, item.packageName)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isBlocked) Color(0xFF4A201A) else DarkCardBorder,
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            color = DarkCardBg
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isBlocked) Color(0xFFFF5252) else Color(item.colorLong))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.appName,
                                        color = Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (item.count > 1) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = Color(0xFF263042),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${item.count}",
                                                color = Color(0xFF64B5F6),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = item.formattedTime(),
                                        color = TextMutedGray,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                if (item.text.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.text,
                                        color = TextMutedGray,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        maxLines = 2
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isBlocked) {
                                        Surface(
                                            color = Color(0xFF381C16),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Blocked",
                                                color = Color(0xFFFF8A65),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        TextButton(
                                            onClick = {
                                                DistractionManager.setPackageBlocked(context, item.packageName, false)
                                                refresh()
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Unblock App", color = Color(0xFF4DA2FF), fontSize = 13.sp)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))

                                        Surface(
                                            color = Color(0xFF26181B),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.clickable {
                                                DistractionManager.setPackageBlocked(context, item.packageName, true)
                                                refresh()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_bell_off),
                                                    contentDescription = null,
                                                    tint = Color(0xFFFF8A65),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Block Notifications",
                                                    color = Color(0xFFFF8A65),
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Blocked Apps Tab
                    val blockedPackages = DistractionManager.getBlockedPackages(context)
                    val allApps = DistractionManager.getAllInstalledApps(context)
                    val blockedList = allApps.filter { blockedPackages.contains(it.first) }

                    if (blockedList.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
                                color = DarkCardBg
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_bell_off),
                                        contentDescription = null,
                                        tint = Color(0xFF5A6678),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No apps currently blocked",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add apps above to suppress notifications.",
                                        color = TextMutedGray,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(blockedList, key = { it.first }) { (pkg, name) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, Color(0xFF4A201A), RoundedCornerShape(18.dp)),
                                color = DarkCardBg
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text(text = pkg, color = TextMutedGray, fontSize = 11.5.sp)
                                    }

                                    Button(
                                        onClick = {
                                            DistractionManager.setPackageBlocked(context, pkg, false)
                                            refresh()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28364A)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Unblock", color = Color(0xFF90CAF9), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAppBlockDialog) {
        AddAppToBlocklistDialog(
            onDismiss = { showAddAppBlockDialog = false },
            onAddApp = { pkg ->
                DistractionManager.setPackageBlocked(context, pkg, true)
                refresh()
                showAddAppBlockDialog = false
            }
        )
    }
}

@Composable
fun AddAppToBlocklistDialog(
    onDismiss: () -> Unit,
    onAddApp: (packageName: String) -> Unit
) {
    val context = LocalContext.current
    val allApps = remember { DistractionManager.getAllInstalledApps(context) }
    var searchQuery by remember { mutableStateOf("") }
    val blocked = remember { DistractionManager.getBlockedPackages(context) }

    val filtered = allApps.filter { (pkg, name) ->
        !blocked.contains(pkg) && (searchQuery.isBlank() || name.contains(searchQuery, ignoreCase = true))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151821),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Block App Notifications",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...", color = Color(0xFF5A6275)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PastelCoral,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = Color(0xFF0F1218),
                        unfocusedContainerColor = Color(0xFF0F1218)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.first }) { (pkg, name) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onAddApp(pkg) },
                            color = Color(0xFF1B2230),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Block",
                                    color = Color(0xFFFF8A65),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextMutedGray)
            }
        }
    )
}

@Composable
fun FilterTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF4DA2FF) else DarkCardBorder,
                shape = RoundedCornerShape(14.dp)
            ),
        color = if (isSelected) Color(0xFF1E2838) else DarkCardBg
    ) {
        Box(
            modifier = Modifier.padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = if (isSelected) Color.White else TextMutedGray,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
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
