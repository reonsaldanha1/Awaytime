package com.example.awaytime.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.awaytime.R
import com.example.awaytime.data.AwayTimeManager
import com.example.awaytime.data.WidgetPreferences
import com.example.awaytime.model.DailyAwayStats
import com.example.awaytime.model.WeeklyAwayStats
import com.example.awaytime.model.WidgetAccent
import com.example.awaytime.model.WidgetTheme
import com.example.awaytime.theme.*

data class WidgetCategoryItem(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: Int,
    val badgeColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { WidgetPreferences(context) }

    var currentTab by remember { mutableStateOf("Widget") }
    var selectedItem by remember { mutableStateOf<WidgetCategoryItem?>(null) }

    var hasPermission by remember { mutableStateOf(AwayTimeManager.hasUsageStatsPermission(context)) }
    var showPermissionDialog by remember { mutableStateOf(!hasPermission) }

    var todayStats by remember { mutableStateOf(AwayTimeManager.getTodayAwayStats(context)) }
    var weeklyStats by remember { mutableStateOf(AwayTimeManager.getWeeklyAwayStats(context)) }
    var dailyWellbeingData by remember { mutableStateOf(AwayTimeManager.getDailyWellbeingData(context)) }
    var weeklyAwayData by remember { mutableStateOf(AwayTimeManager.getWeeklyAwayData(context)) }

    var selectedTheme by remember { mutableStateOf(prefs.theme) }
    var selectedAccent by remember { mutableStateOf(prefs.accent) }
    var showTimeline by remember { mutableStateOf(prefs.showTimeline) }
    var showSparkle by remember { mutableStateOf(prefs.showSparkle) }
    var targetHours by remember { mutableStateOf(prefs.targetGoalHours) }

    LaunchedEffect(selectedItem) {
        if (selectedItem?.id == "daily") {
            dailyWellbeingData = AwayTimeManager.getDailyWellbeingData(context)
        } else if (selectedItem?.id == "weekly") {
            weeklyAwayData = AwayTimeManager.getWeeklyAwayData(context)
        }
    }

    // Re-check permission and refresh stats whenever app resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val perm = AwayTimeManager.hasUsageStatsPermission(context)
                hasPermission = perm
                if (perm) {
                    showPermissionDialog = false
                }
                todayStats = AwayTimeManager.getTodayAwayStats(context)
                weeklyStats = AwayTimeManager.getWeeklyAwayStats(context)
                dailyWellbeingData = AwayTimeManager.getDailyWellbeingData(context)
                weeklyAwayData = AwayTimeManager.getWeeklyAwayData(context)
                triggerWidgetUpdate(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val items = listOf(
        WidgetCategoryItem("daily", "Daily Away", "Track your time away from your phone", R.drawable.ic_timer, PastelBlue),
        WidgetCategoryItem("weekly", "Weekly Away", "Weekly screen time & apps used over the week", R.drawable.ic_chart, PastelPeach),
        WidgetCategoryItem("streaks", "Focus & Streaks", "Current offline streak & phone-free intervals", R.drawable.ic_hourglass, PastelPink),
        WidgetCategoryItem("goals", "Daily Goals", "Set target hours and digital detox milestones", R.drawable.ic_target, PastelMint),
        WidgetCategoryItem("themes", "Styles & Accents", "OLED Pitch Black, Radiant Blue, Nothing OS", R.drawable.ic_palette, PastelYellow),
        WidgetCategoryItem("timeline", "Timeline & Intervals", "Hourly screen-off distribution throughout the day", R.drawable.ic_chart, PastelTeal),
        WidgetCategoryItem("permissions", "Tracking & Permissions", "Usage Access permission for millisecond precision", R.drawable.ic_shield, PastelPurple)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // App Title matching Image 4
            Text(
                text = "Awaytime",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Subtitle
            Text(
                text = "Select widget to configure",
                color = TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // List of cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { item ->
                    WidgetConfigCard(
                        item = item,
                        onClick = { selectedItem = item }
                    )
                }
            }
        }

        // Floating Bottom Navigation Bar matching Image 4
        FloatingBottomBar(
            currentTab = currentTab,
            onTabSelected = { tab ->
                currentTab = tab
                if (tab == "Settings") {
                    selectedItem = items.first { it.id == "permissions" }
                } else if (tab == "Walls") {
                    selectedItem = items.first { it.id == "themes" }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        // First-launch Usage Access Permission Dialog
        if (showPermissionDialog && !hasPermission) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = {
                    Text(
                        text = "Usage Access Permission",
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Awaytime requires Usage Access permission to track your time away from your phone and accurately update your home screen widgets.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionDialog = false
                            openUsageAccessSettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Permission", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text("Not Now", color = TextMuted)
                    }
                },
                containerColor = Color(0xFF181A24),
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Daily Wellbeing Screen for the first option "Daily Away" (matches Image 2)
        if (selectedItem?.id == "daily") {
            DailyWellbeingScreen(
                data = dailyWellbeingData,
                onDismiss = { selectedItem = null }
            )
        } else if (selectedItem?.id == "weekly") {
            // Weekly Away Screen for "Weekly Away" (screen time of week & apps used over week)
            WeeklyAwayScreen(
                data = weeklyAwayData,
                onDismiss = { selectedItem = null }
            )
        } else {
            // Configuration Bottom Sheet for other items
            selectedItem?.let { item ->
                ModalBottomSheet(
                    onDismissRequest = { selectedItem = null },
                    containerColor = Color(0xFF14161F),
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    WidgetConfigSheetDirect(
                        item = item,
                        prefs = prefs,
                        todayStats = todayStats,
                        weeklyStats = weeklyStats,
                        hasPermission = hasPermission,
                        selectedTheme = selectedTheme,
                        selectedAccent = selectedAccent,
                        showTimeline = showTimeline,
                        showSparkle = showSparkle,
                        targetHours = targetHours,
                        onThemeChange = {
                            selectedTheme = it
                            prefs.theme = it
                            triggerWidgetUpdate(context)
                        },
                        onAccentChange = {
                            selectedAccent = it
                            prefs.accent = it
                            triggerWidgetUpdate(context)
                        },
                        onTimelineChange = {
                            showTimeline = it
                            prefs.showTimeline = it
                            triggerWidgetUpdate(context)
                        },
                        onSparkleChange = {
                            showSparkle = it
                            prefs.showSparkle = it
                            triggerWidgetUpdate(context)
                        },
                        onTargetHoursChange = {
                            targetHours = it
                            prefs.targetGoalHours = it
                        },
                        onRequestPermission = {
                            openUsageAccessSettings(context)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetConfigCard(
    item: WidgetCategoryItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, CardStroke, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = CardDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle pastel icon badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(item.badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = null,
                    tint = Color(0xFF1A1C24),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = "Configure",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FloatingBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, Color(0xFF262A36), RoundedCornerShape(32.dp)),
        color = BottomNavBg,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Settings Tab
            BottomNavItem(
                title = "Settings",
                iconRes = R.drawable.ic_settings,
                isSelected = currentTab == "Settings",
                onClick = { onTabSelected("Settings") }
            )

            // Widget Tab (Active pill style)
            BottomNavItem(
                title = "Widget",
                iconRes = R.drawable.ic_widgets_grid,
                isSelected = currentTab == "Widget",
                onClick = { onTabSelected("Widget") }
            )

            // Walls Tab
            BottomNavItem(
                title = "Walls",
                iconRes = R.drawable.ic_wallpaper,
                isSelected = currentTab == "Walls",
                onClick = { onTabSelected("Walls") }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    title: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) BottomNavActive else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = if (isSelected) TextPrimary else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

/**
 * Clean configuration options without widget preview or "Add to Home Screen" button
 */
@Composable
fun WidgetConfigSheetDirect(
    item: WidgetCategoryItem,
    prefs: WidgetPreferences,
    todayStats: DailyAwayStats,
    weeklyStats: WeeklyAwayStats,
    hasPermission: Boolean,
    selectedTheme: WidgetTheme,
    selectedAccent: WidgetAccent,
    showTimeline: Boolean,
    showSparkle: Boolean,
    targetHours: Int,
    onThemeChange: (WidgetTheme) -> Unit,
    onAccentChange: (WidgetAccent) -> Unit,
    onTimelineChange: (Boolean) -> Unit,
    onSparkleChange: (Boolean) -> Unit,
    onTargetHoursChange: (Int) -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp, top = 8.dp)
    ) {
        // Accent Colors Picker
        Text(
            text = "ACCENT COLOR",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sparkle & Timeline Color", color = TextPrimary, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WidgetAccent.values().forEach { acc ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(acc.colorLong))
                            .border(
                                width = if (selectedAccent == acc) 2.5.dp else 0.dp,
                                color = if (selectedAccent == acc) TextPrimary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onAccentChange(acc) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Widget Themes Selector
        Text(
            text = "WIDGET THEME",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WidgetTheme.values().forEach { th ->
                val isSelected = selectedTheme == th
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF26374A) else Color(0xFF1E212C))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AccentBlue else CardStroke,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onThemeChange(th) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = th.displayName,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Toggles
        Text(
            text = "DISPLAY PREFERENCES",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Timeline Bar Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show 24h Timeline Bar", color = TextPrimary, fontSize = 15.sp)
                Text("Fills away intervals up to current hour", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = showTimeline,
                onCheckedChange = onTimelineChange,
                colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = AccentBlue)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sparkle Icon Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show Sparkle Badge", color = TextPrimary, fontSize = 15.sp)
                Text("8-point status star on widget", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = showSparkle,
                onCheckedChange = onSparkleChange,
                colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = AccentBlue)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Daily Goal Hours
        Text(
            text = "DAILY DETOX GOAL",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(6, 8, 10, 12, 14).forEach { hours ->
                val isSelected = targetHours == hours
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentBlue else Color(0xFF1E212C))
                        .clickable { onTargetHoursChange(hours) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${hours}h",
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Permission Card / Status
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, CardStroke, RoundedCornerShape(16.dp)),
            color = Color(0xFF1A1C26)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Usage Access Status", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (hasPermission) "Active (Precise tracking enabled)" else "Missing (Tap to grant)",
                        color = if (hasPermission) Color(0xFF69E094) else Color(0xFFFFAB91),
                        fontSize = 12.sp
                    )
                }
                if (!hasPermission) {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Enable", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

fun openUsageAccessSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}

fun triggerWidgetUpdate(context: Context) {
    val intent = Intent("com.example.awaytime.ACTION_REFRESH_WIDGETS").apply {
        setPackage(context.packageName)
    }
    context.sendBroadcast(intent)
}
