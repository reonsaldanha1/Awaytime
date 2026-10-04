package com.example.awaytime.ui.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.awaytime.R
import com.example.awaytime.data.AppTimerManager
import com.example.awaytime.data.AwayTimeManager
import com.example.awaytime.data.FocusSessionManager

class AppLimitBlockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_TIMER_MINUTES = "extra_timer_minutes"
        const val EXTRA_USED_MILLIS = "extra_used_millis"
        const val EXTRA_IS_DISTRACTION_LOCK = "extra_is_distraction_lock"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val timerMinutes = intent.getIntExtra(EXTRA_TIMER_MINUTES, 30)
        val usedMillis = intent.getLongExtra(EXTRA_USED_MILLIS, 0L)
        val isDistractionLock = intent.getBooleanExtra(EXTRA_IS_DISTRACTION_LOCK, false)

        val pm = packageManager
        val appName = try {
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName
        }

        setContent {
            AppLimitBlockScreen(
                appName = appName,
                timerMinutes = timerMinutes,
                usedMillis = usedMillis,
                isDistractionLock = isDistractionLock,
                onGoHome = {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)
                    finish()
                }
            )
        }
    }
}

@Composable
fun AppLimitBlockScreen(
    appName: String,
    timerMinutes: Int,
    usedMillis: Long,
    isDistractionLock: Boolean = false,
    onGoHome: () -> Unit
) {
    val context = LocalContext.current
    val hasOverlayPerm = remember { FocusSessionManager.canDrawOverlays(context) }

    BackHandler {
        onGoHome()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Glowing Warning Lock Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .scale(scale)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFF5252).copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF261414)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_timer),
                        contentDescription = "App Locked",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = if (isDistractionLock) "APP LOCKED" else "DAILY LIMIT REACHED",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isDistractionLock) {
                    "$appName is locked by Awaytime to protect your time and prevent distractions."
                } else {
                    "You've reached your daily screen time limit for $appName."
                },
                color = Color(0xFFB0B7C6),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Info Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF1C1F28), RoundedCornerShape(20.dp)),
                color = Color(0xFF0C0D12)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    if (isDistractionLock) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Status",
                                color = Color(0xFF8C92A4),
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "Blocked in Distractions",
                                color = Color(0xFFFF5252),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Protection",
                                color = Color(0xFF8C92A4),
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "App & Notification Blocked",
                                color = Color(0xFF69E094),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Manage Lock",
                                color = Color(0xFF8C92A4),
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "Unlock in Distractions Page",
                                color = Color(0xFF64B5F6),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Daily Limit",
                                color = Color(0xFF8C92A4),
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = AppTimerManager.formatTimerMinutes(timerMinutes),
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Screen Time Today",
                                color = Color(0xFF8C92A4),
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = AwayTimeManager.formatDuration(usedMillis),
                                color = Color(0xFFFF7043),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Limit Resets",
                                color = Color(0xFF8C92A4),
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "Tonight at Midnight (00:00)",
                                color = Color(0xFF69E094),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (!hasOverlayPerm) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF5A3022), RoundedCornerShape(14.dp)),
                    color = Color(0xFF261815)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grant 'Display over other apps' for instant screen lock overlay.",
                            color = Color(0xFFFFCC80),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { FocusSessionManager.openOverlaySettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Grant", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Take a break and put your phone away. Disconnecting from screens helps you recharge and stay focused on real life.",
                color = Color(0xFF7A8398),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Close & Go to Home Screen",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
