package com.example.awaytime.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awaytime.R
import com.example.awaytime.data.AppTimerManager
import com.example.awaytime.data.AwayTimeManager
import com.example.awaytime.model.AppUsageInfo
import com.example.awaytime.model.DailyWellbeingData

private val DarkCardBg = Color(0xFF171A21)
private val DarkCardBorder = Color(0xFF232733)
private val TextMutedGray = Color(0xFF9AA0B2)

@Composable
fun DailyWellbeingScreen(
    data: DailyWellbeingData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var currentData by remember(data) { mutableStateOf(data) }
    var timersMap by remember { mutableStateOf(AppTimerManager.getAllTimers(context)) }
    var timerDialogApp by remember { mutableStateOf<AppUsageInfo?>(null) }
    var showAllApps by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        currentData = AwayTimeManager.getDailyWellbeingData(context)
        while (true) {
            kotlinx.coroutines.delay(20000L)
            currentData = AwayTimeManager.getDailyWellbeingData(context)
        }
    }

    val displayData = currentData

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
            // Top Bar with Dailyaway title, back arrow, and right icons
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
                    text = "Dailyaway",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = {
                    currentData = AwayTimeManager.getDailyWellbeingData(context)
                    Toast.makeText(context, "Screen usage updated", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh),
                        contentDescription = "Refresh usage",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Scrollable Dailyaway content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // 1. Main Screen time today Card with Donut Chart and All Apps
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
                    color = DarkCardBg
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)
                    ) {
                        // Header row with screen time total on left and donut chart on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Awaytime",
                                    color = TextMutedGray,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (displayData.formattedAwayTime.isNotBlank()) displayData.formattedAwayTime else displayData.formattedTotalScreenTime,
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )

                                displayData.comparison?.let { comp ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val isImprovement = comp.isReduction
                                    val badgeBg = if (isImprovement) Color(0xFF133825) else Color(0xFF382319)
                                    val badgeColor = if (isImprovement) Color(0xFF69E094) else Color(0xFFFF8A65)
                                    val arrow = if (isImprovement) "↓" else "↑"

                                    Surface(
                                        color = badgeBg,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "$arrow ${comp.comparisonLabel}",
                                            color = badgeColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Donut Chart
                            WellbeingDonutChart(
                                data = displayData,
                                modifier = Modifier.size(105.dp)
                            )
                        }

                        // App Screen time header above the apps
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "App Screen time",
                            color = TextMutedGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Apps used today: 4 apps initially with 'More apps' button
                        val displayedApps = if (showAllApps) displayData.topApps else displayData.topApps.take(4)

                        displayedApps.forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(app.colorLong))
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = app.appName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = app.formattedDuration,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (displayData.topApps.size > 4) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showAllApps = !showAllApps },
                                color = Color(0xFF1E222D),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (showAllApps) "Fewer apps" else "More apps",
                                        color = Color(0xFF4DA2FF),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chevron_right),
                                        contentDescription = null,
                                        tint = Color(0xFF4DA2FF),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Most used app categories section
                Text(
                    text = "Most used app categories",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val gamesCategory = displayData.categories.firstOrNull { it.categoryName == "Games" }
                        ?: displayData.categories.getOrNull(0)
                    val socialCategory = displayData.categories.firstOrNull { it.categoryName == "Social" }
                        ?: displayData.categories.getOrNull(1)

                    // Games Card
                    gamesCategory?.let { cat ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(22.dp)),
                            color = DarkCardBg
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1B2A44)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = cat.iconRes),
                                        contentDescription = cat.categoryName,
                                        tint = Color(cat.colorLong),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = cat.categoryName,
                                    color = Color(0xFFC4C8D4),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cat.formattedDuration,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Social Card
                    socialCategory?.let { cat ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(22.dp)),
                            color = DarkCardBg
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF133240)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = cat.iconRes),
                                        contentDescription = cat.categoryName,
                                        tint = Color(cat.colorLong),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = cat.categoryName,
                                    color = Color(0xFFC4C8D4),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cat.formattedDuration,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Fully functional App timers section
                Text(
                    text = "App timers",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(22.dp)),
                    color = DarkCardBg
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "If you're using certain apps more than you'd like, set a timer to help manage your usage.",
                            color = TextMutedGray,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val appsWithTimers = displayData.topApps.filter { timersMap.containsKey(it.packageName) }
                        val appsWithoutTimers = displayData.topApps.filter { !timersMap.containsKey(it.packageName) }

                        // Section for active timers
                        if (appsWithTimers.isNotEmpty()) {
                            Text(
                                text = "Active Timers",
                                color = Color(0xFF4DA2FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            appsWithTimers.forEach { app ->
                                val timerMin = timersMap[app.packageName] ?: 60
                                val timerMillis = timerMin * 60_000L
                                val isReached = app.usageMillis >= timerMillis
                                val remainingMillis = (timerMillis - app.usageMillis).coerceAtLeast(0L)
                                val fraction = (app.usageMillis.toFloat() / timerMillis.toFloat()).coerceIn(0f, 1f)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF1F222C))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(app.colorLong))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = app.appName,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.weight(1f))

                                        if (isReached) {
                                            Surface(
                                                color = Color(0xFF381919),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "Limit reached",
                                                    color = Color(0xFFFF5252),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "${AwayTimeManager.formatDuration(remainingMillis)} left",
                                                color = Color(0xFF69E094),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(
                                            onClick = { timerDialogApp = app },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_timer),
                                                contentDescription = "Edit timer",
                                                tint = Color(0xFF4DA2FF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                AppTimerManager.removeTimer(context, app.packageName)
                                                timersMap = AppTimerManager.getAllTimers(context)
                                                Toast.makeText(context, "Timer removed for ${app.appName}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_delete),
                                                contentDescription = "Delete timer",
                                                tint = Color(0xFFFF6E6E),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Used: ${app.formattedDuration}",
                                            color = TextMutedGray,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Limit: ${AppTimerManager.formatTimerMinutes(timerMin)}",
                                            color = TextMutedGray,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF282C38))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (isReached) Color(0xFFFF5252) else Color(0xFF4DA2FF))
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Section for setting timers on apps
                        Text(
                            text = if (appsWithTimers.isNotEmpty()) "Other Apps" else "Set App Timers",
                            color = Color(0xFFC4C8D4),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var showAllRemaining by remember { mutableStateOf(false) }
                        val displayRemaining = if (showAllRemaining) appsWithoutTimers else appsWithoutTimers.take(4)

                        displayRemaining.forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF141720))
                                    .clickable { timerDialogApp = app }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(app.colorLong))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${app.formattedDuration} today",
                                        color = TextMutedGray,
                                        fontSize = 12.sp
                                    )
                                }

                                Surface(
                                    color = Color(0xFF232733),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { timerDialogApp = app }
                                ) {
                                    Text(
                                        text = "+ Set Timer",
                                        color = Color(0xFF4DA2FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        if (appsWithoutTimers.size > 4) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = { showAllRemaining = !showAllRemaining },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = if (showAllRemaining) "Show fewer apps" else "Show all apps (${appsWithoutTimers.size})",
                                    color = Color(0xFF4DA2FF),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Device wellbeing shortcut button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF282C38), RoundedCornerShape(12.dp))
                                .clickable { openSystemWellbeing(context) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_hourglass),
                                contentDescription = null,
                                tint = TextMutedGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Device Wellbeing & App Limits",
                                color = TextMutedGray,
                                fontSize = 12.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = TextMutedGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // App Timer Dialog
        timerDialogApp?.let { app ->
            val currentTimer = timersMap[app.packageName] ?: 0
            AppTimerDialog(
                app = app,
                currentMinutes = currentTimer,
                onSave = { minutes ->
                    AppTimerManager.setTimerMinutes(context, app.packageName, minutes)
                    timersMap = AppTimerManager.getAllTimers(context)
                    timerDialogApp = null
                    Toast.makeText(context, "Timer set for ${app.appName}: ${AppTimerManager.formatTimerMinutes(minutes)}/day", Toast.LENGTH_SHORT).show()
                },
                onDelete = {
                    AppTimerManager.removeTimer(context, app.packageName)
                    timersMap = AppTimerManager.getAllTimers(context)
                    timerDialogApp = null
                    Toast.makeText(context, "Timer removed for ${app.appName}", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { timerDialogApp = null }
            )
        }
    }
}

@Composable
fun AppTimerDialog(
    app: AppUsageInfo,
    currentMinutes: Int,
    onSave: (Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(if (currentMinutes > 0) currentMinutes else 60) }
    val row1 = listOf(15, 30, 45, 60)
    val row2 = listOf(90, 120, 180, 240)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(app.colorLong))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${app.appName} Timer",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Used today: ${app.formattedDuration}",
                    color = TextMutedGray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Daily limit: ${AppTimerManager.formatTimerMinutes(selectedMinutes)}",
                    color = Color(0xFF4DA2FF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Presets row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row1.forEach { min ->
                        val isSelected = selectedMinutes == min
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedMinutes = min },
                            color = if (isSelected) Color(0xFF388AF6) else Color(0xFF1F222C),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = AppTimerManager.formatTimerMinutes(min),
                                    color = if (isSelected) Color.White else TextMutedGray,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Presets row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row2.forEach { min ->
                        val isSelected = selectedMinutes == min
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedMinutes = min },
                            color = if (isSelected) Color(0xFF388AF6) else Color(0xFF1F222C),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = AppTimerManager.formatTimerMinutes(min),
                                    color = if (isSelected) Color.White else TextMutedGray,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom duration stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { if (selectedMinutes > 15) selectedMinutes -= 15 },
                        color = Color(0xFF232733),
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-15m", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    Text(
                        text = AppTimerManager.formatTimerMinutes(selectedMinutes),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(18.dp))

                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { selectedMinutes += 15 },
                        color = Color(0xFF232733),
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+15m", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedMinutes) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388AF6)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Set Timer", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentMinutes > 0) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = Color(0xFFFF5252))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextMutedGray)
                }
            }
        },
        containerColor = Color(0xFF171A21),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun WellbeingDonutChart(
    data: DailyWellbeingData,
    modifier: Modifier = Modifier
) {
    val total = (data.topApps.sumOf { it.usageMillis } + data.otherAppsMillis).coerceAtLeast(1L)

    Canvas(modifier = modifier) {
        val strokeWidth = 13.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        data class Slice(val sweep: Float, val color: Color)
        val slices = mutableListOf<Slice>()

        data.topApps.forEach { app ->
            val sweep = (app.usageMillis.toFloat() / total.toFloat()) * 360f
            if (sweep > 3f) {
                slices.add(Slice(sweep, Color(app.colorLong)))
            }
        }

        if (data.otherAppsMillis > 0L) {
            val otherSweep = (data.otherAppsMillis.toFloat() / total.toFloat()) * 360f
            if (otherSweep > 3f) {
                slices.add(Slice(otherSweep, Color(0xFF6E7482)))
            }
        }

        if (slices.isEmpty()) {
            drawArc(
                color = Color(0xFF388AF6),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            return@Canvas
        }

        var startAngle = -90f
        for (slice in slices) {
            val gap = if (slices.size > 1) 3f else 0f
            val sweepAngle = (slice.sweep - gap).coerceAtLeast(1f)
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += slice.sweep
        }
    }
}

private fun openSystemWellbeing(context: Context) {
    try {
        val intent = Intent("com.google.android.apps.wellbeing.action.APP_USAGE").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        openUsageSettings(context)
    }
}

private fun openUsageSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Settings cannot be opened directly on this device.", Toast.LENGTH_SHORT).show()
    }
}
