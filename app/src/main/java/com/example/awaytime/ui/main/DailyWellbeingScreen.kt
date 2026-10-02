package com.example.awaytime.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

    BackHandler {
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D11))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top Bar with Digital Wellbeing title, back arrow, and right icons
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
                    text = "Digital Wellbeing",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { openSystemWellbeing(context) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bar_chart_alt),
                        contentDescription = "Chart",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { openUsageSettings(context) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vert),
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Scrollable Wellbeing content matching user's Image 2
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // 1. Top Card: Build healthy digital habits
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
                        Text(
                            text = "Build healthy digital habits",
                            color = Color(0xFF4DA2FF),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You'll get feedback and help to keep you on track.",
                            color = TextMutedGray,
                            fontSize = 13.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Main Screen time today Card with Donut Chart and Top Apps
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
                                    text = "Screen time today",
                                    color = TextMutedGray,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = data.formattedTotalScreenTime,
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )
                            }

                            // Donut Chart
                            WellbeingDonutChart(
                                data = data,
                                modifier = Modifier.size(105.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        // App rows
                        data.topApps.forEach { app ->
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
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Most used app categories section
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
                    val gamesCategory = data.categories.firstOrNull { it.categoryName == "Games" }
                        ?: data.categories.getOrNull(0)
                    val socialCategory = data.categories.firstOrNull { it.categoryName == "Social" }
                        ?: data.categories.getOrNull(1)

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

                // 4. App timers section
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

                        Spacer(modifier = Modifier.height(18.dp))

                        val primaryApp = data.topApps.firstOrNull()
                        if (primaryApp != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1F222C))
                                    .clickable { openSystemWellbeing(context) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(primaryApp.colorLong).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_hourglass),
                                        contentDescription = null,
                                        tint = Color(primaryApp.colorLong),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = primaryApp.appName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Surface(
                                    color = Color(0xFF282C38),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Manage Timer",
                                        color = Color(0xFF4DA2FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
            if (sweep > 4f) {
                slices.add(Slice(sweep, Color(app.colorLong)))
            }
        }

        if (data.otherAppsMillis > 0L) {
            val otherSweep = (data.otherAppsMillis.toFloat() / total.toFloat()) * 360f
            if (otherSweep > 4f) {
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
            val gap = if (slices.size > 1) 3.5f else 0f
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
    } catch (e: Exception) {}
}
