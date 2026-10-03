package com.example.awaytime.ui.main

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awaytime.R
import com.example.awaytime.data.AppTimerItem
import com.example.awaytime.data.AppTimerManager
import com.example.awaytime.data.FocusSessionManager
import com.example.awaytime.theme.PastelCoral
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DarkCardBg = Color(0xFF0C0D12)
private val DarkCardBorder = Color(0xFF1C1F28)
private val TextMutedGray = Color(0xFFA2A9B8)

@Composable
fun AppTimersScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var appsList by remember { mutableStateOf<List<AppTimerItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Limits Set", "Reached"
    var selectedAppForTimer by remember { mutableStateOf<AppTimerItem?>(null) }

    val hasOverlayPerm = remember { FocusSessionManager.canDrawOverlays(context) }

    suspend fun loadApps() {
        isLoading = true
        val list = withContext(Dispatchers.IO) {
            AppTimerManager.getInstalledAppsWithUsage(context)
        }
        appsList = list
        isLoading = false
    }

    LaunchedEffect(Unit) {
        loadApps()
    }

    BackHandler {
        onDismiss()
    }

    val totalLimited = appsList.count { it.isTimerSet }
    val totalReached = appsList.count { it.isLimitReached }

    val filteredApps = appsList.filter { item ->
        val matchesSearch = searchQuery.isBlank() || item.appName.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "Limits Set" -> item.isTimerSet
            "Reached" -> item.isLimitReached
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
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
                    text = "App Timers",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Banner / Status Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
                        color = DarkCardBg
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Daily App Limits",
                                        color = TextMutedGray,
                                        fontSize = 13.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$totalLimited Active Limits",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (totalReached > 0) {
                                    Surface(
                                        color = Color(0xFF3B1E19),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "$totalReached Limit Reached",
                                            color = Color(0xFFFF8A65),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFF1B2C24),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "Active Protection",
                                            color = Color(0xFF69E094),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Set daily screen time limits for addictive apps. Once an app reaches its limit, Awaytime locks it until tomorrow.",
                                color = TextMutedGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            if (!hasOverlayPerm) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { FocusSessionManager.openOverlaySettings(context) },
                                    color = Color(0xFF281F1A),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5D3622))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_shield),
                                            contentDescription = null,
                                            tint = Color(0xFFFFB74D),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Grant Display Over Other Apps for instant lock overlay",
                                            color = Color(0xFFFFCC80),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search installed apps...", color = Color(0xFF5A6275)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PastelCoral,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkCardBg,
                            unfocusedContainerColor = DarkCardBg
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_target),
                                contentDescription = null,
                                tint = TextMutedGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                // Filter Tabs: All, Limits Set, Reached
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppTimerFilterTab(
                            title = "All (${appsList.size})",
                            isSelected = selectedFilter == "All",
                            onClick = { selectedFilter = "All" },
                            modifier = Modifier.weight(1f)
                        )
                        AppTimerFilterTab(
                            title = "Limits Set ($totalLimited)",
                            isSelected = selectedFilter == "Limits Set",
                            onClick = { selectedFilter = "Limits Set" },
                            modifier = Modifier.weight(1f)
                        )
                        AppTimerFilterTab(
                            title = "Reached ($totalReached)",
                            isSelected = selectedFilter == "Reached",
                            onClick = { selectedFilter = "Reached" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Apps List
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PastelCoral)
                        }
                    }
                } else if (filteredApps.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
                            color = DarkCardBg
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No apps found matching \"$searchQuery\"" else "No apps in this filter",
                                    color = TextMutedGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppTimerRow(
                            item = app,
                            onSetTimerClick = { selectedAppForTimer = app }
                        )
                    }
                }
            }
        }
    }

    selectedAppForTimer?.let { app ->
        SetAppTimerDialog(
            app = app,
            onDismiss = { selectedAppForTimer = null },
            onSaveTimer = { mins ->
                AppTimerManager.setTimerMinutes(context, app.packageName, mins)
                selectedAppForTimer = null
                // Re-evaluate
                appsList = appsList.map {
                    if (it.packageName == app.packageName) {
                        it.copy(timerMinutes = mins)
                    } else it
                }.sortedWith(
                    compareByDescending<AppTimerItem> { it.isLimitReached }
                        .thenByDescending { it.isTimerSet }
                        .thenByDescending { it.usageTodayMillis }
                        .thenBy { it.appName.lowercase() }
                )
            },
            onRemoveTimer = {
                AppTimerManager.removeTimer(context, app.packageName)
                selectedAppForTimer = null
                appsList = appsList.map {
                    if (it.packageName == app.packageName) {
                        it.copy(timerMinutes = 0)
                    } else it
                }.sortedWith(
                    compareByDescending<AppTimerItem> { it.isLimitReached }
                        .thenByDescending { it.isTimerSet }
                        .thenByDescending { it.usageTodayMillis }
                        .thenBy { it.appName.lowercase() }
                )
            }
        )
    }
}

@Composable
fun AppTimerRow(
    item: AppTimerItem,
    onSetTimerClick: () -> Unit
) {
    val progress = if (item.isTimerSet) {
        (item.usageTodayMillis.toFloat() / (item.timerMinutes * 60_000L).toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (item.isLimitReached) Color(0xFF5A251D) else if (item.isTimerSet) Color(0xFF2E384D) else DarkCardBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable { onSetTimerClick() },
        color = DarkCardBg
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                AppIconBox(drawable = item.iconDrawable)

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Used ${item.formattedUsage()} today",
                        color = TextMutedGray,
                        fontSize = 12.5.sp
                    )
                }

                // Timer Badge or Button
                if (item.isLimitReached) {
                    Surface(
                        color = Color(0xFF4A1F1A),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "LOCKED",
                            color = Color(0xFFFF5252),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                } else if (item.isTimerSet) {
                    Surface(
                        color = Color(0xFF1E2838),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${AppTimerManager.formatTimerMinutes(item.timerMinutes)} limit",
                            color = Color(0xFF64B5F6),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onSetTimerClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF90CAF9)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B3A52)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Set Limit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Progress Bar if timer is set
            if (item.isTimerSet) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (item.isLimitReached) Color(0xFFFF5252) else Color(0xFF4DA2FF),
                    trackColor = Color(0xFF232733)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (item.isLimitReached) "Daily limit reached • App blocked" else "${item.formattedRemaining()}",
                        color = if (item.isLimitReached) Color(0xFFFF8A65) else TextMutedGray,
                        fontSize = 11.5.sp,
                        fontWeight = if (item.isLimitReached) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = TextMutedGray,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AppIconBox(drawable: Drawable?) {
    val bitmap = remember(drawable) {
        drawable?.let {
            try {
                val width = if (it.intrinsicWidth > 0) it.intrinsicWidth else 48
                val height = if (it.intrinsicHeight > 0) it.intrinsicHeight else 48
                val bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bm)
                it.setBounds(0, 0, canvas.width, canvas.height)
                it.draw(canvas)
                bm
            } catch (e: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF263238)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_widgets_grid),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AppTimerFilterTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) Color(0xFF232B3A) else Color(0xFF13161C),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = if (isSelected) Color.White else TextMutedGray,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun SetAppTimerDialog(
    app: AppTimerItem,
    onDismiss: () -> Unit,
    onSaveTimer: (minutes: Int) -> Unit,
    onRemoveTimer: () -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(if (app.timerMinutes > 0) app.timerMinutes else 30) }
    var customMinutesInput by remember { mutableStateOf("") }
    var isCustom by remember { mutableStateOf(false) }

    val presets = listOf(15, 30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C0D12),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconBox(drawable = app.iconDrawable)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Set Timer",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = app.appName,
                        color = TextMutedGray,
                        fontSize = 13.sp
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Daily Screen Time Limit",
                    color = TextMutedGray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Presets Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.take(3).forEach { mins ->
                            PresetButton(
                                label = AppTimerManager.formatTimerMinutes(mins),
                                isSelected = !isCustom && selectedMinutes == mins,
                                onClick = {
                                    isCustom = false
                                    selectedMinutes = mins
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.drop(3).forEach { mins ->
                            PresetButton(
                                label = AppTimerManager.formatTimerMinutes(mins),
                                isSelected = !isCustom && selectedMinutes == mins,
                                onClick = {
                                    isCustom = false
                                    selectedMinutes = mins
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Minutes Input
                Text(
                    text = "Or enter custom minutes:",
                    color = TextMutedGray,
                    fontSize = 12.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = customMinutesInput,
                    onValueChange = {
                        customMinutesInput = it.filter { ch -> ch.isDigit() }
                        val parsed = customMinutesInput.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            selectedMinutes = parsed
                            isCustom = true
                        }
                    },
                    placeholder = { Text("e.g. 20, 50, 75", color = Color(0xFF5A6275)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PastelCoral,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = Color(0xFF060709),
                        unfocusedContainerColor = Color(0xFF060709)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF121620),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Once you reach ${AppTimerManager.formatTimerMinutes(selectedMinutes)} on ${app.appName} today, Awaytime will prevent access until tomorrow.",
                        color = Color(0xFF8898AA),
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveTimer(selectedMinutes) },
                colors = ButtonDefaults.buttonColors(containerColor = PastelCoral),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Limit", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (app.isTimerSet) {
                    TextButton(
                        onClick = onRemoveTimer,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Text("Remove")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextMutedGray)
                }
            }
        }
    )
}

@Composable
fun PresetButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = if (isSelected) Color(0xFF2F3C54) else Color(0xFF1E2433),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64B5F6)) else null,
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else TextMutedGray,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
