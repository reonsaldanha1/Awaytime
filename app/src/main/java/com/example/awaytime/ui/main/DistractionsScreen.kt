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

private val DarkCardBg = Color(0xFF171A21)
private val DarkCardBorder = Color(0xFF232733)
private val TextMutedGray = Color(0xFF9AA0B2)

@Composable
fun DistractionsScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(DistractionManager.hasNotificationListenerPermission(context)) }
    var distractions by remember { mutableStateOf(DistractionManager.getDistractions(context)) }
    var selectedTab by remember { mutableStateOf("notifications") } // "notifications" or "blocked"

    fun refresh() {
        hasPermission = DistractionManager.hasNotificationListenerPermission(context)
        distractions = DistractionManager.getDistractions(context)
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
                                    text = "To track distracting notifications and block unwanted alerts from specific apps, grant Notification Access.",
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

                // Summary Stats Card
                item {
                    val blockedCount = DistractionManager.getBlockedPackages(context).size
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
                        color = DarkCardBg
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Distractions",
                                    color = TextMutedGray,
                                    fontSize = 13.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${distractions.size}",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Blocked Apps",
                                    color = TextMutedGray,
                                    fontSize = 13.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = if (blockedCount > 0) Color(0xFF381C16) else Color(0xFF1E2433),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "$blockedCount blocked",
                                        color = if (blockedCount > 0) Color(0xFFFF8A65) else Color(0xFF38BDF8),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tabs: All Notifications / Blocked Apps
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
                                // App Name + Timestamp + Block Status Badge
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
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = item.formattedTime(),
                                        color = TextMutedGray,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Notification Content
                                Text(
                                    text = item.title,
                                    color = Color(0xFFE5E7EB),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (item.text.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.text,
                                        color = TextMutedGray,
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Block / Unblock Action Button for this specific app
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isBlocked) {
                                        Surface(
                                            color = Color(0xFF381410),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_bell_off),
                                                    contentDescription = null,
                                                    tint = Color(0xFFFF5252),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "Muted",
                                                    color = Color(0xFFFF5252),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

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
                    // Blocked Apps list
                    val blockedPackages = DistractionManager.getBlockedPackages(context)
                    val allKnownPackages = distractions.map { it.packageName to it.appName }.distinctBy { it.first }

                    if (allKnownPackages.isEmpty()) {
                        item {
                            Text("No apps detected yet", color = TextMutedGray, fontSize = 14.sp)
                        }
                    } else {
                        items(allKnownPackages) { (pkg, name) ->
                            val isBlocked = blockedPackages.contains(pkg)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp)),
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

                                    Switch(
                                        checked = isBlocked,
                                        onCheckedChange = { blocked ->
                                            DistractionManager.setPackageBlocked(context, pkg, blocked)
                                            refresh()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFFFF5252)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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
