package com.example.awaytime.ui.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.example.awaytime.data.FocusSessionManager
import com.example.awaytime.data.MotivationalQuotes
import kotlinx.coroutines.delay

class FocusBlockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }

    private var quoteRefreshTrigger by mutableStateOf(0)
    private var blockedPackage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        blockedPackage = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        quoteRefreshTrigger++

        setContent {
            FocusBlockScreen(
                blockedPackage = blockedPackage,
                quoteTrigger = quoteRefreshTrigger,
                onRefreshQuote = { quoteRefreshTrigger++ },
                onGoHome = {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)
                    finish()
                },
                onSessionExpired = {
                    finish()
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        quoteRefreshTrigger++
    }
}

@Composable
fun FocusBlockScreen(
    blockedPackage: String = "",
    quoteTrigger: Int = 0,
    onRefreshQuote: () -> Unit = {},
    onGoHome: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val context = LocalContext.current
    var remainingMillis by remember { mutableStateOf(FocusSessionManager.getRemainingMillis(context)) }

    // Each time the screen is opened or quoteTrigger updates, pick a fresh quote
    val currentQuote = remember(quoteTrigger) {
        MotivationalQuotes.getRandomQuote()
    }

    val appLabel = remember(blockedPackage) {
        if (blockedPackage.isNotBlank()) {
            try {
                val pm = context.packageManager
                val ai = pm.getApplicationInfo(blockedPackage, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // Tapping back button returns to the home screen rather than falling back into the restricted app
    BackHandler {
        onGoHome()
    }

    LaunchedEffect(Unit) {
        while (true) {
            val rem = FocusSessionManager.getRemainingMillis(context)
            val isActive = FocusSessionManager.isFocusActive(context)
            if (!isActive || rem <= 0L) {
                onSessionExpired()
                break
            }
            remainingMillis = rem
            delay(1000L)
        }
    }

    val minutes = (remainingMillis / 1000L) / 60L
    val seconds = (remainingMillis / 1000L) % 60L
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

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
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onGoHome
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent click-through on inner column */ }
                )
        ) {
            // Glowing Focus Icon emblem
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
                                colors = listOf(Color(0xFF4DA2FF).copy(alpha = glowAlpha), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141926)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_hourglass),
                        contentDescription = "Focus",
                        tint = Color(0xFF4DA2FF),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Dynamic Motivational Quote (smoothly animated when refreshed)
            AnimatedContent(
                targetState = currentQuote,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))
                        .togetherWith(fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)))
                },
                label = "quoteTransition"
            ) { quote ->
                Text(
                    text = quote,
                    color = Color.White,
                    fontSize = if (quote.length > 50) 19.sp else 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = if (quote.length > 50) 28.sp else 32.sp,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onRefreshQuote
                        )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Remaining Focus Time Indicator
            Surface(
                color = Color(0xFF161A24),
                shape = CircleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF69E094))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$timeFormatted remaining",
                        color = Color(0xFF69E094),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Specific app restriction message
            Text(
                text = if (appLabel != null) "$appLabel is restricted during your Focus session."
                else "This app is restricted during your Focus session.",
                color = Color(0xFF9AA0B2),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Clean single-tap exit button to return to home
            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151922)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(46.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Return to Home",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
