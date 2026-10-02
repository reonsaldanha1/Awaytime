package com.example.awaytime.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.widget.Toast
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
import com.example.awaytime.model.WeeklyAwayData

private val DarkCardBg = Color(0xFF171A21)
private val DarkCardBorder = Color(0xFF232733)
private val TextMutedGray = Color(0xFF9AA0B2)

@Composable
fun WeeklyAwayScreen(
    data: WeeklyAwayData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var currentData by remember(data) { mutableStateOf(data) }

    LaunchedEffect(Unit) {
        currentData = AwayTimeManager.getWeeklyAwayData(context)
        while (true) {
            kotlinx.coroutines.delay(20000L)
            currentData = AwayTimeManager.getWeeklyAwayData(context)
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
                    text = "Weeklyaway",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = {
                    currentData = AwayTimeManager.getWeeklyAwayData(context)
                    Toast.makeText(context, "Weekly usage updated", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh),
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { openSystemWellbeing(context) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bar_chart_alt),
                        contentDescription = "Chart",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { openUsageSettings(context) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vert),
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Banner Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
                    color = DarkCardBg
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)
                    ) {
                        val bannerMsg = if (displayData.comparison != null) {
                            val diffText = formatDetailedDiff(displayData.comparison.diffMillis, displayData.comparison.formattedDiff)
                            if (displayData.comparison.isReduction) {
                                "You have put your phone away $diffText more than last week"
                            } else {
                                "You have put your phone away $diffText less than last week"
                            }
                        } else {
                            "${displayData.dateRangeLabel} • 7-day usage & apps breakdown"
                        }

                        Text(
                            text = "Weekly Away Time",
                            color = Color(0xFF4DA2FF),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = bannerMsg,
                            color = TextMutedGray,
                            fontSize = 13.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Screen Usage of the Week Card + 7-Day Chart
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
                    color = DarkCardBg
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Away time this week",
                                    color = TextMutedGray,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (displayData.formattedTotalAwayTime.isNotBlank()) displayData.formattedTotalAwayTime else displayData.formattedTotalScreenTime,
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )

                                displayData.comparison?.let { comp ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val isReduction = comp.isReduction
                                    val badgeBg = if (isReduction) Color(0xFF133825) else Color(0xFF382319)
                                    val badgeColor = if (isReduction) Color(0xFF69E094) else Color(0xFFFF8A65)
                                    val arrow = if (isReduction) "↓" else "↑"

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

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Daily away avg",
                                    color = TextMutedGray,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = Color(0xFF1E2838),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (displayData.formattedAverageDailyAwayTime.isNotBlank()) displayData.formattedAverageDailyAwayTime else displayData.formattedAverageDailyScreenTime,
                                        color = Color(0xFF38BDF8),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 7-day bar chart
                        val maxDayHours = (displayData.dailyBreakdown.maxOfOrNull { it.screenHours } ?: 8f).coerceAtLeast(1f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            displayData.dailyBreakdown.forEach { day ->
                                val barHeightFraction = (day.screenHours / maxDayHours).coerceIn(0.08f, 1f)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (day.screenHours > 0.1f) "%.1fh".format(day.screenHours) else "0h",
                                        color = if (day.isToday) Color(0xFF4DA2FF) else TextMutedGray,
                                        fontSize = 10.sp,
                                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .fillMaxHeight(barHeightFraction * 0.72f)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                            .background(
                                                if (day.isToday) Color(0xFF388AF6)
                                                else Color(0xFF263248)
                                            )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = day.dayLabel,
                                        color = if (day.isToday) Color.White else TextMutedGray,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Apps Used Over the Week Card
                Text(
                    text = "Apps used over the week",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
                    color = DarkCardBg
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp)
                    ) {
                        val maxAppMillis = (displayData.topApps.maxOfOrNull { it.usageMillis } ?: 1L).coerceAtLeast(1L)

                        displayData.topApps.forEachIndexed { index, app ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
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
                                    Spacer(modifier = Modifier.width(12.dp))
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
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val progress = (app.usageMillis.toFloat() / maxAppMillis.toFloat()).coerceIn(0.02f, 1f)
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(app.colorLong),
                                    trackColor = Color(0xFF222632)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Weekly Categories
                Text(
                    text = "Most used categories this week",
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

                // 4. Weekly Away Detox Summary
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
                            text = "Time away from phone this week",
                            color = TextMutedGray,
                            fontSize = 13.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val weekTotalMillis = 7L * 24 * 3600 * 1000L
                        val awayWeekMillis = (weekTotalMillis - displayData.totalScreenMillis).coerceAtLeast(0L)
                        val awayHours = awayWeekMillis / 3600000L
                        val awayMins = (awayWeekMillis % 3600000L) / 60000L

                        Text(
                            text = "${awayHours}h ${awayMins}m phone-free",
                            color = Color(0xFF69E094),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val summaryNote = if (displayData.comparison?.isReduction == true) {
                            val diffText = formatDetailedDiff(displayData.comparison.diffMillis, displayData.comparison.formattedDiff)
                            "Weekly improvement: Put phone away $diffText more than last week!"
                        } else if (displayData.comparison != null) {
                            val diffText = formatDetailedDiff(displayData.comparison.diffMillis, displayData.comparison.formattedDiff)
                            "Notice: Put phone away $diffText less than last week."
                        } else {
                            "Great job investing in real-world moments and focus habits."
                        }

                        Text(
                            text = summaryNote,
                            color = TextMutedGray,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
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
    } catch (e: Exception) {}
}

private fun formatDetailedDiff(diffMillis: Long, fallbackFormatted: String): String {
    val absMillis = Math.abs(diffMillis)
    val totalMinutes = absMillis / (1000 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val hrStr = if (hours == 1L) "1 hour" else "$hours hours"
    val minStr = if (minutes == 1L) "1 minute" else "$minutes minutes"
    return when {
        hours > 0 && minutes > 0 -> "$hrStr $minStr"
        hours > 0 -> hrStr
        minutes > 0 -> minStr
        fallbackFormatted.isNotBlank() -> fallbackFormatted
            .replace(" h", " hours")
            .replace(" m", " minutes")
        else -> "0 minutes"
    }
}

