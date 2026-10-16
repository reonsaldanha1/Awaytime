package com.example.awaytime.ui.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awaytime.R
import com.example.awaytime.data.WidgetPreferences
import com.example.awaytime.model.WidgetAccent
import com.example.awaytime.model.WidgetFont

private val DarkCardBg = Color(0xFF0C0D12)
private val DarkCardBorder = Color(0xFF1C1F28)
private val TextMutedGray = Color(0xFFA2A9B8)

@Composable
fun CustomizeWidgetScreen(
    prefs: WidgetPreferences,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var selectedAccent by remember { mutableStateOf(prefs.accent) }
    var selectedFont by remember { mutableStateOf(prefs.widgetFont) }
    var isMinimalMode by remember { mutableStateOf(prefs.isMinimalMode) }
    var showTimeline by remember { mutableStateOf(prefs.showTimeline) }
    var showTimelineLines by remember { mutableStateOf(prefs.showTimelineLines) }
    var showSparkle by remember { mutableStateOf(prefs.showSparkle) }

    fun applyChanges() {
        prefs.accent = selectedAccent
        prefs.widgetFont = selectedFont
        prefs.isMinimalMode = isMinimalMode
        prefs.showTimeline = showTimeline
        prefs.showTimelineLines = showTimelineLines
        prefs.showSparkle = showSparkle
        triggerWidgetUpdate(context)
    }

    BackHandler {
        onDismiss()
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
                    text = "Customize Widget",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    painter = painterResource(id = R.drawable.ic_palette),
                    contentDescription = null,
                    tint = if (isMinimalMode) Color.White else Color(selectedAccent.colorLong),
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(22.dp)
                )
            }

            // Scrollable Settings
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 100.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Live Interactive Widget Preview
                Text(
                    text = "LIVE WIDGET PREVIEW",
                    color = TextMutedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                WidgetPreviewCard(
                    accent = selectedAccent,
                    font = selectedFont,
                    isMinimal = isMinimalMode,
                    showTimeline = showTimeline,
                    showTimelineLines = showTimelineLines,
                    showSparkle = showSparkle
                )

                Spacer(modifier = Modifier.height(26.dp))

                // 1. Minimal Black & White Option
                Text(
                    text = "MINIMAL MODE",
                    color = TextMutedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = if (isMinimalMode) 1.5.dp else 1.dp,
                            color = if (isMinimalMode) Color.White else DarkCardBorder,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    color = DarkCardBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Minimal (Black & White)",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Removes colors and displays widgets in pure monochrome black and white.",
                                color = TextMutedGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        SmoothToggleSwitch(
                            checked = isMinimalMode,
                            onCheckedChange = {
                                isMinimalMode = it
                                applyChanges()
                            },
                            activeTrackColor = Color.White,
                            activeThumbColor = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // 2. Accent Color Customization
                Text(
                    text = if (isMinimalMode) "ACCENT COLOR (DISABLED IN MINIMAL MODE)" else "ACCENT COLOR",
                    color = if (isMinimalMode) Color(0xFF6B7280) else TextMutedGray,
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
                    WidgetAccent.values().forEach { acc ->
                        val isSelected = selectedAccent == acc && !isMinimalMode
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isMinimalMode) Color(acc.colorLong).copy(alpha = 0.35f)
                                    else Color(acc.colorLong)
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable(enabled = !isMinimalMode) {
                                    selectedAccent = acc
                                    applyChanges()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // 3. Font Customization
                Text(
                    text = "WIDGET FONT",
                    color = TextMutedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WidgetFont.values().forEach { font ->
                        val isSelected = selectedFont == font
                        val (composeFontFamily, composeFontWeight) = getComposeFontFamilyAndWeight(font)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) (if (isMinimalMode) Color.White else Color(selectedAccent.colorLong)) else DarkCardBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    selectedFont = font
                                    applyChanges()
                                },
                            color = if (isSelected) Color(0xFF1E2330) else DarkCardBg
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = font.displayName,
                                        color = if (isSelected) Color.White else Color(0xFFD1D5DB),
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Sample: 19h 45m away",
                                        color = TextMutedGray,
                                        fontSize = 13.sp,
                                        fontFamily = composeFontFamily,
                                        fontWeight = composeFontWeight
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedFont = font
                                        applyChanges()
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = if (isMinimalMode) Color.White else Color(selectedAccent.colorLong),
                                        unselectedColor = Color(0xFF4B5563)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // 4. Widget Elements (Timeline Bar, Lines Toggle & Sparkle)
                Text(
                    text = "WIDGET ELEMENTS",
                    color = TextMutedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
                    color = DarkCardBg
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Timeline Bar toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("24-Hour Timeline Bar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text("Displays active screen intervals up to current hour", color = TextMutedGray, fontSize = 12.sp)
                            }
                            SmoothToggleSwitch(
                                checked = showTimeline,
                                onCheckedChange = {
                                    showTimeline = it
                                    applyChanges()
                                },
                                activeTrackColor = if (isMinimalMode) Color.White else Color(selectedAccent.colorLong),
                                activeThumbColor = if (isMinimalMode) Color.Black else Color.White
                            )
                        }

                        AnimatedVisibility(
                            visible = showTimeline,
                            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
                        ) {
                            Column {
                                Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                                // Option to disable the various lines in the widget bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Bar Divider Lines & Markers", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = if (showTimelineLines) "Dividers & cursor active (turn off for solid bar)" else "Disabled (clean solid continuous progress bar)",
                                            color = if (showTimelineLines) TextMutedGray else Color(0xFF69E094),
                                            fontSize = 12.sp
                                        )
                                    }
                                    SmoothToggleSwitch(
                                        checked = showTimelineLines,
                                        onCheckedChange = {
                                            showTimelineLines = it
                                            applyChanges()
                                        },
                                        activeTrackColor = if (isMinimalMode) Color.White else Color(selectedAccent.colorLong),
                                        activeThumbColor = if (isMinimalMode) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                        // Sparkle Badge toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sparkle Badge", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text("8-point accent star badge on corner", color = TextMutedGray, fontSize = 12.sp)
                            }
                            SmoothToggleSwitch(
                                checked = showSparkle,
                                onCheckedChange = {
                                    showSparkle = it
                                    applyChanges()
                                },
                                activeTrackColor = if (isMinimalMode) Color.White else Color(selectedAccent.colorLong),
                                activeThumbColor = if (isMinimalMode) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmoothToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeTrackColor: Color = Color.White,
    inactiveTrackColor: Color = Color(0xFF222530),
    thumbColor: Color = Color.White,
    activeThumbColor: Color = Color.Black
) {
    val trackWidth = 50.dp
    val trackHeight = 28.dp
    val thumbSize = 22.dp
    val padding = 3.dp

    val animatedProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "switchProgress"
    )

    val currentTrackColor by animateColorAsState(
        targetValue = if (!enabled) Color(0xFF1A1C24)
                      else if (checked) activeTrackColor
                      else inactiveTrackColor,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "trackColor"
    )

    val currentThumbColor by animateColorAsState(
        targetValue = if (!enabled) Color(0xFF555A68)
                      else if (checked) activeThumbColor
                      else thumbColor,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "thumbColor"
    )

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(CircleShape)
            .background(currentTrackColor)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onCheckedChange(!checked)
            }
            .padding(padding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = (trackWidth - thumbSize - padding * 2) * animatedProgress)
                .size(thumbSize)
                .clip(CircleShape)
                .background(currentThumbColor)
        )
    }
}

@Composable
fun getComposeFontFamilyAndWeight(font: WidgetFont): Pair<FontFamily, FontWeight> {
    return when (font) {
        WidgetFont.DOT_MATRIX -> Pair(FontFamily(Font(R.font.dot_matrix)), FontWeight.Normal)
        WidgetFont.MONOSPACE -> Pair(FontFamily.Monospace, FontWeight.Normal)
        WidgetFont.CONDENSED -> Pair(FontFamily.SansSerif, FontWeight.Normal)
        WidgetFont.SERIF -> Pair(FontFamily.Serif, FontWeight.Normal)
        WidgetFont.CASUAL -> Pair(FontFamily.Cursive, FontWeight.Normal)
        WidgetFont.CURSIVE -> Pair(FontFamily.Cursive, FontWeight.Normal)
        WidgetFont.LIGHT -> Pair(FontFamily.SansSerif, FontWeight.Light)
        WidgetFont.BLACK -> Pair(FontFamily.SansSerif, FontWeight.Black)
        WidgetFont.MEDIUM -> Pair(FontFamily.SansSerif, FontWeight.Medium)
        WidgetFont.SANS_SERIF -> Pair(FontFamily.Default, FontWeight.Normal)
    }
}

@Composable
fun WidgetPreviewCard(
    accent: WidgetAccent,
    font: WidgetFont,
    isMinimal: Boolean,
    showTimeline: Boolean,
    showTimelineLines: Boolean,
    showSparkle: Boolean
) {
    val activeColor by animateColorAsState(
        targetValue = if (isMinimal) Color.White else Color(accent.colorLong),
        animationSpec = tween(durationMillis = 220),
        label = "previewActiveColor"
    )
    val (composeFontFamily, composeFontWeight) = getComposeFontFamilyAndWeight(font)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .border(
                width = if (isMinimal) 1.5.dp else 1.dp,
                color = if (isMinimal) Color(0xFF444444) else Color(0xFF2A2C35),
                shape = RoundedCornerShape(26.dp)
            ),
        color = if (isMinimal) Color(0xFF000000) else Color(0xFF17181C)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Timeline bar preview with fluid animation
            AnimatedVisibility(
                visible = showTimeline,
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(220))
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isMinimal) Color(0xFF1C1D22) else Color(0xFF2A2B33))
                    ) {
                        if (showTimelineLines) {
                            // Segmented intervals with divider lines and cursor
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(0.35f)
                                        .background(activeColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(2.dp)
                                        .background(if (isMinimal) Color(0xFF1C1D22) else Color(0xFF2A2B33))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(0.30f)
                                        .background(activeColor)
                                )
                                // Cursor marker line
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(3.dp)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.weight(0.35f))
                            }
                        } else {
                            // Clean solid continuous progress fill without lines
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.65f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(activeColor)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Bottom row with Away label, Away Time, and Sparkle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Away",
                        color = if (isMinimal) Color(0xFFAAAAAA) else Color(0xFF9EACB9),
                        fontSize = 15.sp,
                        fontFamily = composeFontFamily,
                        fontWeight = composeFontWeight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "19h 25m",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = if (composeFontWeight != FontWeight.Normal) composeFontWeight else FontWeight.Bold,
                        fontFamily = composeFontFamily
                    )
                }

                AnimatedVisibility(
                    visible = showSparkle,
                    enter = fadeIn(tween(200)) + scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)),
                    exit = fadeOut(tween(150)) + scaleOut(tween(150))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sparkle),
                        contentDescription = "Sparkle",
                        tint = activeColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
