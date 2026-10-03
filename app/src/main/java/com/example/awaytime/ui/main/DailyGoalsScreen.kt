package com.example.awaytime.ui.main

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awaytime.R
import com.example.awaytime.data.DailyGoalsManager
import com.example.awaytime.model.DailyGoal
import com.example.awaytime.theme.PastelMint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DarkCardBg = Color(0xFF0C0D12)
private val DarkCardBorder = Color(0xFF1C1F28)
private val TextMutedGray = Color(0xFFA2A9B8)

@Composable
fun DailyGoalsScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var goals by remember { mutableStateOf(DailyGoalsManager.getGoals(context)) }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Active", "Completed"
    var showAddDialog by remember { mutableStateOf(false) }

    fun refresh() {
        goals = DailyGoalsManager.getGoals(context)
    }

    BackHandler {
        onDismiss()
    }

    val totalGoals = goals.size
    val completedGoals = goals.count { it.isCompleted }
    val progress = if (totalGoals > 0) completedGoals.toFloat() / totalGoals.toFloat() else 0f

    val filteredGoals = when (selectedFilter) {
        "Active" -> goals.filter { !it.isCompleted }
        "Completed" -> goals.filter { it.isCompleted }
        else -> goals
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
                    text = "Daily Goals",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Add Goal Button in Top Bar
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F2430))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sparkle),
                        contentDescription = "Add Goal",
                        tint = PastelMint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Summary Progress Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
                        color = DarkCardBg
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Today's Progress",
                                        color = TextMutedGray,
                                        fontSize = 13.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$completedGoals of $totalGoals Completed",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    color = if (progress >= 1f && totalGoals > 0) Color(0xFF1E3A2B) else Color(0xFF1B2433),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        color = if (progress >= 1f && totalGoals > 0) PastelMint else Color(0xFF64B5F6),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Linear Progress Indicator
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = PastelMint,
                                trackColor = Color(0xFF232733)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // 10-Minute Reminder info banner
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF11141B),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_bell),
                                        contentDescription = null,
                                        tint = PastelMint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Awaytime sends notifications 10 min prior and right on your deadline",
                                        color = TextMutedGray,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Filter Tabs: All, Active, Completed
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoalFilterTab(
                            title = "All ($totalGoals)",
                            isSelected = selectedFilter == "All",
                            onClick = { selectedFilter = "All" },
                            modifier = Modifier.weight(1f)
                        )
                        GoalFilterTab(
                            title = "Active (${totalGoals - completedGoals})",
                            isSelected = selectedFilter == "Active",
                            onClick = { selectedFilter = "Active" },
                            modifier = Modifier.weight(1f)
                        )
                        GoalFilterTab(
                            title = "Done ($completedGoals)",
                            isSelected = selectedFilter == "Completed",
                            onClick = { selectedFilter = "Completed" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Goals List
                if (filteredGoals.isEmpty()) {
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
                                    .padding(vertical = 40.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_target),
                                    contentDescription = null,
                                    tint = Color(0xFF4A5568),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (selectedFilter == "All") "No Daily Goals Yet" else "No $selectedFilter Goals",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Add goals with target completion times to get reminders before deadlines.",
                                    color = TextMutedGray,
                                    fontSize = 13.5.sp,
                                    lineHeight = 18.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PastelMint),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("+ Add First Goal", color = Color(0xFF0F1A15), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(filteredGoals, key = { it.id }) { goal ->
                        GoalItemCard(
                            goal = goal,
                            onToggle = {
                                DailyGoalsManager.toggleGoalCompleted(context, goal.id)
                                refresh()
                            },
                            onDelete = {
                                DailyGoalsManager.deleteGoal(context, goal.id)
                                refresh()
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button at Bottom
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = PastelMint,
            contentColor = Color(0xFF0A1E14),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_target),
                    contentDescription = "Add Goal",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Goal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                )
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            onDismiss = { showAddDialog = false },
            onAddGoal = { title, targetMillis ->
                DailyGoalsManager.addGoal(context, title, targetMillis)
                refresh()
                showAddDialog = false
            }
        )
    }
}

@Composable
fun GoalItemCard(
    goal: DailyGoal,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isOverdue = goal.isOverdue()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (goal.isCompleted) Color(0xFF1B3828) else if (isOverdue) Color(0xFF4A201A) else DarkCardBorder,
                RoundedCornerShape(20.dp)
            ),
        color = DarkCardBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox Circle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (goal.isCompleted) PastelMint else Color.Transparent
                    )
                    .border(
                        2.dp,
                        if (goal.isCompleted) PastelMint else Color(0xFF4A5568),
                        CircleShape
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (goal.isCompleted) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sparkle),
                        contentDescription = "Completed",
                        tint = Color(0xFF0F1A15),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = goal.title,
                    color = if (goal.isCompleted) TextMutedGray else Color.White,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Deadline Badge
                    Surface(
                        color = when {
                            goal.isCompleted -> Color(0xFF192A20)
                            isOverdue -> Color(0xFF381B16)
                            else -> Color(0xFF172436)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    goal.isCompleted -> "✓ Completed"
                                    isOverdue -> "⚠️ ${goal.remainingTimeText()}"
                                    else -> "⏰ By ${goal.formattedTargetTime()} (${goal.remainingTimeText()})"
                                },
                                color = when {
                                    goal.isCompleted -> PastelMint
                                    isOverdue -> Color(0xFFFF8A65)
                                    else -> Color(0xFF64B5F6)
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Dual Reminder Alerts Chip
                    if (!goal.isCompleted && !isOverdue) {
                        Surface(
                            color = Color(0xFF161F29),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "🔔 10m & due alerts",
                                color = TextMutedGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete Action Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = "Delete Goal",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun GoalFilterTab(
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
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onAddGoal: (title: String, targetMillis: Long) -> Unit
) {
    val context = LocalContext.current
    var goalTitle by remember { mutableStateOf("") }

    // Default target time: 1 hour from now
    var targetCalendar by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, 1)
            }
        )
    }

    val suggestions = listOf(
        "Read book 30m",
        "Finish work assignment",
        "Exercise workout",
        "Phone-free study session",
        "Offline meditation",
        "Walk outside"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C0D12),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_target),
                    contentDescription = null,
                    tint = PastelMint,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Daily Goal",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Goal Title",
                    color = TextMutedGray,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = goalTitle,
                    onValueChange = { goalTitle = it },
                    placeholder = { Text("e.g. Read 30 minutes", color = Color(0xFF5A6275)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PastelMint,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = Color(0xFF060709),
                        unfocusedContainerColor = Color(0xFF060709)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick suggestions
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(suggestions) { sug ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { goalTitle = sug },
                            color = Color(0xFF202633),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = sug,
                                color = Color(0xFF8294B0),
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Target Completion Time",
                    color = TextMutedGray,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Quick Presets Row: +30m, +1h, +2h, +3h
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(30 to "+30m", 60 to "+1h", 120 to "+2h", 180 to "+3h").forEach { (mins, label) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.MINUTE, mins)
                                    targetCalendar = cal
                                },
                            color = Color(0xFF1E2433),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = Color(0xFF64B5F6),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pick Exact Time Button
                val formattedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(targetCalendar.time)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF283244), RoundedCornerShape(12.dp))
                        .clickable {
                            val curHour = targetCalendar.get(Calendar.HOUR_OF_DAY)
                            val curMin = targetCalendar.get(Calendar.MINUTE)
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        set(Calendar.MINUTE, minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                        // If selected time is earlier today, set to tomorrow or keep
                                        if (timeInMillis <= System.currentTimeMillis()) {
                                            add(Calendar.DAY_OF_YEAR, 1)
                                        }
                                    }
                                    targetCalendar = newCal
                                },
                                curHour,
                                curMin,
                                false
                            ).show()
                        },
                    color = Color(0xFF11151D)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_timer),
                            contentDescription = null,
                            tint = PastelMint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Due at $formattedTime",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Change Time",
                            color = PastelMint,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reminder preview
                val reminderCalendar = (targetCalendar.clone() as Calendar).apply {
                    add(Calendar.MINUTE, -10)
                }
                val reminderFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(reminderCalendar.time)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0C0D12),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1C1F28))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔔 2 notifications will be sent:\n• 10 min prior at $reminderFormatted\n• On deadline at $formattedTime",
                            color = Color(0xFF90A4AE),
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (goalTitle.isNotBlank()) {
                        onAddGoal(goalTitle.trim(), targetCalendar.timeInMillis)
                    }
                },
                enabled = goalTitle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PastelMint),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Goal", color = Color(0xFF0F1A15), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMutedGray)
            }
        }
    )
}
