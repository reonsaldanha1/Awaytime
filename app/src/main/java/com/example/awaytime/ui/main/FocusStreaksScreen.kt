package com.example.awaytime.ui.main

import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.awaytime.R
import com.example.awaytime.data.FocusAppInfo
import com.example.awaytime.data.FocusSessionManager
import com.example.awaytime.model.DailyAwayStats
import kotlinx.coroutines.delay
import java.util.Date

private val DarkCardBg = Color(0xFF171A21)
private val DarkCardBorder = Color(0xFF232733)
private val TextMutedGray = Color(0xFF9AA0B2)

@Composable
fun FocusStreaksScreen(
    todayStats: DailyAwayStats,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isFocusActive by remember { mutableStateOf(FocusSessionManager.isFocusActive(context)) }
    var remainingMillis by remember { mutableStateOf(FocusSessionManager.getRemainingMillis(context)) }
    var selectedDurationMinutes by remember { mutableStateOf(FocusSessionManager.getFocusDurationMinutes(context).coerceAtLeast(15)) }
    var allowedPackages by remember { mutableStateOf(FocusSessionManager.getAllowedPackages(context)) }
    var showAppPickerDialog by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(FocusSessionManager.canDrawOverlays(context)) }

    // Live ticker for active focus session
    LaunchedEffect(isFocusActive) {
        while (isFocusActive) {
            val rem = FocusSessionManager.getRemainingMillis(context)
            val active = FocusSessionManager.isFocusActive(context)
            isFocusActive = active
            remainingMillis = rem
            if (!active || rem <= 0L) {
                isFocusActive = false
                break
            }
            delay(1000L)
        }
    }

    // Auto-refresh overlay permission on resume
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = FocusSessionManager.canDrawOverlays(context)
                isFocusActive = FocusSessionManager.isFocusActive(context)
                remainingMillis = FocusSessionManager.getRemainingMillis(context)
                allowedPackages = FocusSessionManager.getAllowedPackages(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                    text = "Focus & Streaks",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.weight(1f))

                if (isFocusActive) {
                    Surface(
                        color = Color(0xFF133825),
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF69E094))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active",
                                color = Color(0xFF69E094),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // 1. Focus Timer Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
                    color = DarkCardBg
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        if (isFocusActive) {
                            // Active Session View
                            ActiveFocusView(
                                remainingMillis = remainingMillis,
                                durationMinutes = selectedDurationMinutes,
                                allowedCount = allowedPackages.size,
                                onStop = {
                                    FocusSessionManager.stopFocusSession(context)
                                    isFocusActive = false
                                    Toast.makeText(context, "Focus session stopped", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            // Inactive Configuration View
                            InactiveFocusView(
                                selectedDurationMinutes = selectedDurationMinutes,
                                onDurationSelect = { selectedDurationMinutes = it },
                                allowedCount = allowedPackages.size,
                                onOpenAppPicker = { showAppPickerDialog = true },
                                hasOverlayPermission = hasOverlayPermission,
                                onRequestOverlayPermission = {
                                    FocusSessionManager.openOverlaySettings(context)
                                },
                                onStart = {
                                    FocusSessionManager.startFocusSession(
                                        context = context,
                                        durationMinutes = selectedDurationMinutes,
                                        allowedPackages = allowedPackages
                                    )
                                    isFocusActive = true
                                    remainingMillis = selectedDurationMinutes * 60 * 1000L
                                    Toast.makeText(context, "Focus mode started! Restricting other apps.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                // 2. Streaks & Milestones Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
                    color = DarkCardBg
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text(
                            text = "Offline Streak & Milestones",
                            color = Color(0xFFF472B6),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Stay off your screen to extend your unbroken offline streak.",
                            color = TextMutedGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Current Streak
                            val streakMins = (todayStats.currentStreakMillis / 60000L).coerceAtLeast(0L)
                            val streakHours = streakMins / 60L
                            val streakRemMins = streakMins % 60L
                            val streakText = if (streakHours > 0) "${streakHours}h ${streakRemMins}m" else "${streakRemMins}m"

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1D212C)),
                                color = Color(0xFF1D212C)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Current Streak", color = TextMutedGray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = streakText,
                                        color = Color(0xFFFFB74D),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Phone-free now", color = TextMutedGray, fontSize = 11.sp)
                                }
                            }

                            // Completed Focus Sessions
                            val completed = FocusSessionManager.getCompletedSessions(context)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1D212C)),
                                color = Color(0xFF1D212C)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Sessions Done", color = TextMutedGray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$completed",
                                        color = Color(0xFF69E094),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Focus milestones", color = TextMutedGray, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Total Focus Minutes
                        val totalMins = FocusSessionManager.getTotalFocusMinutes(context)
                        val totalHours = totalMins / 60L
                        val remMins = totalMins % 60L
                        val totalFocusStr = if (totalHours > 0) "${totalHours}h ${remMins}m" else "${remMins}m"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF141720))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_target),
                                contentDescription = null,
                                tint = Color(0xFF4DA2FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Deep Focus Time", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("Invested in distraction-free work", color = TextMutedGray, fontSize = 11.5.sp)
                            }
                            Text(totalFocusStr, color = Color(0xFF4DA2FF), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. Hourly Focus & Screen-Off Intervals
                if (todayStats.intervals.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
                        color = DarkCardBg
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text(
                                text = "Today's Phone-Free Intervals",
                                color = Color(0xFF2DD4BF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Recorded intervals when your screen was switched off.",
                                color = TextMutedGray,
                                fontSize = 12.5.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val recentIntervals = todayStats.intervals.takeLast(6).reversed()
                            recentIntervals.forEach { interval ->
                                val durMins = ((interval.endMillis - interval.startMillis) / 60000L).coerceAtLeast(1L)
                                val startTime = DateFormat.format("HH:mm", Date(interval.startMillis)).toString()
                                val endTime = DateFormat.format("HH:mm", Date(interval.endMillis)).toString()
                                val isAway = interval.isAway

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
                                            .background(if (isAway) Color(0xFF69E094) else Color(0xFFFF8A65))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "$startTime – $endTime",
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = if (isAway) "${durMins}m away" else "${durMins}m screen",
                                        color = if (isAway) Color(0xFF69E094) else Color(0xFFFF8A65),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Allowed Apps Selection Dialog
        if (showAppPickerDialog) {
            AllowedAppsDialog(
                allowedPackages = allowedPackages,
                onDismiss = { showAppPickerDialog = false },
                onSave = { updated ->
                    allowedPackages = updated
                    FocusSessionManager.setAllowedPackages(context, updated)
                    showAppPickerDialog = false
                    Toast.makeText(context, "${updated.size} apps allowed during Focus", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun ActiveFocusView(
    remainingMillis: Long,
    durationMinutes: Int,
    allowedCount: Int,
    onStop: () -> Unit
) {
    val totalMillis = (durationMinutes * 60 * 1000L).coerceAtLeast(1L)
    val progress = (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)

    val minutes = (remainingMillis / 1000L) / 60L
    val seconds = (remainingMillis / 1000L) % 60L
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Focus Session in Progress",
            color = Color(0xFF4DA2FF),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Restricting all apps except $allowedCount allowed app(s).",
            color = TextMutedGray,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Large Circular Timer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(170.dp)
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF222633),
                strokeWidth = 10.dp
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF4DA2FF),
                strokeWidth = 10.dp,
                trackColor = Color.Transparent
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "REMAINING",
                    color = TextMutedGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Surface(
            color = Color(0xFF1E222D),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "PATIENCE IS THE KEY TO SUCCESS",
                color = Color(0xFF69E094),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Only way to stop the timer is from Awaytime app
        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Stop Focus Session",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InactiveFocusView(
    selectedDurationMinutes: Int,
    onDurationSelect: (Int) -> Unit,
    allowedCount: Int,
    onOpenAppPicker: () -> Unit,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onStart: () -> Unit
) {
    val presets = listOf(15, 25, 45, 60, 90, 120)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Deep Focus Timer",
            color = Color(0xFF4DA2FF),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Block distracting apps and stay locked into your work.",
            color = TextMutedGray,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Focus Duration",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Duration Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.take(3).forEach { mins ->
                val isSelected = selectedDurationMinutes == mins
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDurationSelect(mins) },
                    color = if (isSelected) Color(0xFF1E3A5F) else Color(0xFF1E222D),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4DA2FF)) else null
                ) {
                    Text(
                        text = if (mins == 25) "25m ★" else "${mins}m",
                        color = if (isSelected) Color(0xFF4DA2FF) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.drop(3).forEach { mins ->
                val isSelected = selectedDurationMinutes == mins
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDurationSelect(mins) },
                    color = if (isSelected) Color(0xFF1E3A5F) else Color(0xFF1E222D),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4DA2FF)) else null
                ) {
                    Text(
                        text = "${mins}m",
                        color = if (isSelected) Color(0xFF4DA2FF) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Allowed Apps Selector Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onOpenAppPicker() },
            color = Color(0xFF1E222D),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_shield),
                    contentDescription = null,
                    tint = Color(0xFF69E094),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Allowed Apps During Focus",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$allowedCount app(s) permitted • Tap to customize",
                        color = TextMutedGray,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = TextMutedGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Overlay permission check banner
        if (!hasOverlayPermission) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                color = Color(0xFF2E1F12),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Permission Required",
                        color = Color(0xFFFFB74D),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Grant 'Display over other apps' so Awaytime can restrict access to blocked apps and show the focus screen.",
                        color = Color(0xFFFFCC80),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRequestOverlayPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Grant Permission", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start Button
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388AF6)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_hourglass),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Start Focus Session ($selectedDurationMinutes min)",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AllowedAppsDialog(
    allowedPackages: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    var appsList by remember { mutableStateOf<List<FocusAppInfo>>(emptyList()) }
    var selectedSet by remember { mutableStateOf(allowedPackages.toMutableSet()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        appsList = FocusSessionManager.getInstalledLaunchableApps(context)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF151821),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Select Allowed Apps",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Only apps checked here can be opened during Focus mode.",
                    color = TextMutedGray,
                    fontSize = 12.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...", color = TextMutedGray, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4DA2FF),
                        unfocusedBorderColor = Color(0xFF2A2E3D),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        val essentials = FocusSessionManager.getDefaultEssentialPackages(context)
                        selectedSet = essentials.toMutableSet()
                    }) {
                        Text("Essentials Only", color = Color(0xFF4DA2FF), fontSize = 12.sp)
                    }

                    TextButton(onClick = {
                        selectedSet.clear()
                    }) {
                        Text("Clear All", color = Color(0xFFFF8A65), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Apps list
                val filtered = appsList.filter {
                    searchQuery.isBlank() || it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        val isChecked = selectedSet.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) {
                                        selectedSet = (selectedSet - app.packageName).toMutableSet()
                                    } else {
                                        selectedSet = (selectedSet + app.packageName).toMutableSet()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedSet = (selectedSet + app.packageName).toMutableSet()
                                    } else {
                                        selectedSet = (selectedSet - app.packageName).toMutableSet()
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF388AF6),
                                    uncheckedColor = Color(0xFF5A6072)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (app.isSystemEssential) {
                                    Text(
                                        text = "System Essential (Calls/Phone)",
                                        color = Color(0xFF69E094),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMutedGray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(selectedSet) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388AF6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save (${selectedSet.size})", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
