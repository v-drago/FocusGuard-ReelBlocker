package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate800
import com.example.ui.theme.Teal400
import com.example.ui.theme.Teal500
import com.example.ui.theme.Teal600
import com.example.ui.viewmodel.FocusViewModel
import java.util.Locale

@Composable
fun FocusTimerScreen(
    viewModel: FocusViewModel
) {
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()

    val scale = remember { Animatable(1f) }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            scale.animateTo(
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            scale.snapTo(1f)
        }
    }

    val minutes = timerSeconds / 60
    val seconds = timerSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Mindful Focus Timer",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Breathe with the ring to calm dopamine craving",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )
        }

        // Animated Breathing Ring & Timer Display
        Box(
            modifier = Modifier
                .size(260.dp)
                .testTag("focus_timer_ring"),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing Outer Ring
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(if (isTimerRunning) scale.value else 1f)
                    .background(
                        Brush.radialGradient(
                            listOf(Teal500.copy(alpha = 0.35f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            // Inner Ring
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(DarkSurface, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = Teal400,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 42.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTimerRunning) "Inhale... Exhale" else "Deep Focus Mode",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Teal400,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        // Preset Chips Row
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Select Focus Session Duration",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.LightGray,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(10, 25, 45).forEach { duration ->
                    val isSelected = timerSeconds == duration * 60
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.startFocusSession(duration) },
                        label = { Text("$duration mins", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Teal600,
                            selectedLabelColor = Color.White,
                            containerColor = Slate800,
                            labelColor = Color.LightGray
                        )
                    )
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!isTimerRunning) {
                Button(
                    onClick = { viewModel.startFocusSession(timerSeconds / 60) },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("start_timer_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal600)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Focus",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            } else {
                Button(
                    onClick = { viewModel.pauseFocusSession() },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("pause_timer_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pause Session",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            OutlinedButton(
                onClick = { viewModel.resetFocusSession() },
                modifier = Modifier
                    .size(54.dp)
                    .testTag("reset_timer_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Teal400,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
