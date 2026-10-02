package com.example.awaytime.ui.main

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awaytime.R
import com.example.awaytime.data.AwayTimeManager
import com.example.awaytime.data.WidgetPreferences
import com.example.awaytime.model.DailyAwayStats
import com.example.awaytime.model.WeeklyAwayStats
import com.example.awaytime.model.WidgetAccent
import com.example.awaytime.model.WidgetTheme
import com.example.awaytime.theme.*
import com.example.awaytime.widget.AwayWidgetSmallProvider
import com.example.awaytime.widget.AwayWidgetWeeklyProvider

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
    val prefs = remember { WidgetPreferences(context) }
    var currentTab by remember { mutableStateOf("Widget") }
    var selectedItem by remember { mutableStateOf<WidgetCategoryItem?>(null) }

    var todayStats by remember { mutableStateOf(AwayTimeManager.getTodayAwayStats(context)) }
    var weeklyStats by remember { mutableStateOf(AwayTimeManager.getWeeklyAwayStats(context)) }
    var hasPermission by remember { mutableStateOf(AwayTimeManager.hasUsageStatsPermission(context)) }

    var selectedTheme by remember { mutableStateOf(prefs.theme) }
    var selectedAccent by remember { mutableStateOf(prefs.accent) }
    var showTimeline by remember { mutableStateOf(prefs.showTimeline) }
    var showSparkle by remember { mutableStateOf(prefs.showSparkle) }
    var targetHours by remember { mutableStateOf(prefs.targetGoalHours) }

    val items = listOf(
        WidgetCategoryItem("daily", "Daily Away", "Track your time away from your phone", R.drawable.ic_timer, PastelBlue),
        WidgetCategoryItem("weekly", "Weekly Overview", "Weekly total, day average & 7-day dot chart", R.drawable.ic_chart, PastelPeach),
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

        // Detail / Config Bottom Sheet
        selectedItem?.let { item ->
            ModalBottomSheet(
                onDismissRequest = { selectedItem = null },
                containerColor = Color(0xFF13151D),
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                WidgetConfigSheetContent(
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
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    onPinWidget = { isWeekly ->
                        pinWidgetToHomeScreen(context, isWeekly)
                    }
                )
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

@Composable
fun WidgetConfigSheetContent(
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
    onRequestPermission: () -> Unit,
    onPinWidget: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp)
    ) {
        Text(
            text = item.title,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = item.description,
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Live interactive widget preview!
        Text(
            text = "LIVE HOME SCREEN PREVIEW",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (item.id == "weekly") {
            // Weekly 4x3 Widget Preview matching Image 2
            WeeklyWidgetPreview(
                weeklyStats = weeklyStats,
                accent = selectedAccent
            )
        } else {
            // Daily 2x2 Widget Preview matching Image 1 & 3
            SmallWidgetPreview(
                stats = todayStats,
                accent = selectedAccent,
                theme = selectedTheme,
                showTimeline = showTimeline,
                showSparkle = showSparkle
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Action: Pin Widget to Home Screen
        Button(
            onClick = { onPinWidget(item.id == "weekly") },
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_widgets_grid),
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add to Home Screen",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Customization Controls
        Text(
            text = "CUSTOMIZATION",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Accent Colors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Accent Color", color = TextPrimary, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetAccent.values().forEach { acc ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(acc.colorLong))
                            .border(
                                width = if (selectedAccent == acc) 2.dp else 0.dp,
                                color = if (selectedAccent == acc) TextPrimary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onAccentChange(acc) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Timeline Bar Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Show Timeline Bar", color = TextPrimary, fontSize = 15.sp)
                Text("Displays 24h away/usage intervals", color = TextSecondary, fontSize = 12.sp)
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
            Column {
                Text("Show Sparkle Badge", color = TextPrimary, fontSize = 15.sp)
                Text("8-point star away status indicator", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = showSparkle,
                onCheckedChange = onSparkleChange,
                colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = AccentBlue)
            )
        }

        if (item.id == "permissions" || !hasPermission) {
            Spacer(modifier = Modifier.height(18.dp))
            OutlinedButton(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(PastelPurple, AccentBlue))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Usage Access Permission", color = TextPrimary)
            }
        }
    }
}

@Composable
fun SmallWidgetPreview(
    stats: DailyAwayStats,
    accent: WidgetAccent,
    theme: WidgetTheme,
    showTimeline: Boolean,
    showSparkle: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(theme.bgStartColor))
            .border(1.dp, Color(0xFF2A2C35), RoundedCornerShape(26.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (showTimeline) {
                // Timeline bar preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2A2B33))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(0.18f))
                        Box(
                            modifier = Modifier
                                .weight(0.08f)
                                .fillMaxHeight()
                                .background(Color(accent.colorLong))
                        )
                        Spacer(modifier = Modifier.weight(0.04f))
                        Box(
                            modifier = Modifier
                                .weight(0.06f)
                                .fillMaxHeight()
                                .background(Color(accent.colorLong))
                        )
                        Spacer(modifier = Modifier.weight(0.03f))
                        Box(
                            modifier = Modifier
                                .weight(0.08f)
                                .fillMaxHeight()
                                .background(Color(accent.colorLong))
                        )
                        Spacer(modifier = Modifier.weight(0.04f))
                        Box(
                            modifier = Modifier
                                .weight(0.20f)
                                .fillMaxHeight()
                                .background(Color(accent.colorLong))
                        )
                        Spacer(modifier = Modifier.weight(0.04f))
                        Box(
                            modifier = Modifier
                                .weight(0.06f)
                                .fillMaxHeight()
                                .background(Color(accent.colorLong))
                        )
                        Spacer(modifier = Modifier.weight(0.25f))
                    }

                    // Vertical white cursor line
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 180.dp)
                            .width(2.dp)
                            .fillMaxHeight(0.8f)
                            .background(Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Away",
                        color = Color(0xFF9EACB9),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stats.formattedAwayTime(),
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontFamily = FontFamily.Serif
                    )
                }

                if (showSparkle) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sparkle),
                        contentDescription = null,
                        tint = Color(accent.colorLong),
                        modifier = Modifier
                            .size(28.dp)
                            .padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyWidgetPreview(
    weeklyStats: WeeklyAwayStats,
    accent: WidgetAccent
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF3880EC), Color(0xFF2160D4))
                )
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly away time",
                    color = Color(0xEEFFFFFF),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_sparkle),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = weeklyStats.formattedTotal(),
                color = Color.White,
                fontSize = 28.sp,
                fontFamily = FontFamily.Serif
            )

            Text(
                text = weeklyStats.dateRangeLabel,
                color = Color(0xC7FFFFFF),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Day average banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x26FFFFFF))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("◎", color = Color(0xEEFFFFFF), fontSize = 13.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Day average",
                    color = Color(0xEEFFFFFF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = weeklyStats.formattedAverage(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7-day dot chart preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyStats.dailyPoints.forEach { pt ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .width(1.dp)
                                .background(Color(0x40FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pt.dayNumber,
                            color = Color(0xC7FFFFFF),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

fun triggerWidgetUpdate(context: Context) {
    val intent = Intent("com.example.awaytime.ACTION_REFRESH_WIDGETS").apply {
        setPackage(context.packageName)
    }
    context.sendBroadcast(intent)
}

fun pinWidgetToHomeScreen(context: Context, isWeekly: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val providerClass = if (isWeekly) AwayWidgetWeeklyProvider::class.java else AwayWidgetSmallProvider::class.java
            val provider = ComponentName(context, providerClass)
            appWidgetManager.requestPinAppWidget(provider, null, null)
        }
    }
}
