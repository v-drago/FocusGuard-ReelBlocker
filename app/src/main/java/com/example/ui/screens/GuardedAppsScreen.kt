package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GuardedApp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate800
import com.example.ui.theme.Teal400
import com.example.ui.theme.Teal600
import com.example.ui.viewmodel.FocusViewModel

@Composable
fun GuardedAppsScreen(
    viewModel: FocusViewModel
) {
    val guardedApps by viewModel.guardedApps.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Column {
                Text(
                    text = "App Distraction Guards",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                )
                Text(
                    text = "Configure direct Reels button removal, element masking, and guard modes",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
            }
        }

        items(
            items = guardedApps,
            key = { it.packageName }
        ) { app ->
            GuardedAppCard(
                app = app,
                onToggleGuard = { viewModel.toggleAppGuard(app) },
                onToggleReelsBlock = { viewModel.toggleReelsBlock(app) },
                onReelsTimerChanged = { seconds -> viewModel.updateReelsTimer(app, seconds) },
                onToggleMainFeedNudge = { viewModel.toggleMainFeedNudge(app) },
                onMainFeedTimerChanged = { minutes -> viewModel.updateMainFeedTimer(app, minutes) },
                onEnforcementModeChanged = { mode -> viewModel.updateEnforcementMode(app, mode) },
                onLimitChanged = { newLimit -> viewModel.updateDailyLimit(app, newLimit) }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun GuardedAppCard(
    app: GuardedApp,
    onToggleGuard: () -> Unit,
    onToggleReelsBlock: () -> Unit,
    onReelsTimerChanged: (Int) -> Unit,
    onToggleMainFeedNudge: () -> Unit,
    onMainFeedTimerChanged: (Int) -> Unit,
    onEnforcementModeChanged: (String) -> Unit,
    onLimitChanged: (Int) -> Unit
) {
    val appIcon: ImageVector = when (app.iconType) {
        "INSTAGRAM" -> Icons.Default.CameraAlt
        "TIKTOK" -> Icons.Default.Videocam
        "YOUTUBE" -> Icons.Default.OndemandVideo
        else -> Icons.Default.Smartphone
    }

    val iconBgColor: Color = when (app.iconType) {
        "INSTAGRAM" -> Color(0xFFE1306C)
        "TIKTOK" -> Color(0xFF00F2FE)
        "YOUTUBE" -> Color(0xFFFF0000)
        else -> Teal600
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guarded_app_card_${app.appName.lowercase()}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isGuarded) DarkSurface else DarkSurfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // App Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(iconBgColor.copy(alpha = 0.25f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = appIcon,
                        contentDescription = app.appName,
                        tint = if (app.isGuarded) iconBgColor else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = if (app.isGuarded) "Shield Active" else "Shield Paused",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (app.isGuarded) Teal400 else Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Switch(
                    checked = app.isGuarded,
                    onCheckedChange = { onToggleGuard() },
                    modifier = Modifier.testTag("guard_switch_${app.appName.lowercase()}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Teal600,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Slate800
                    )
                )
            }

            if (app.isGuarded) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Slate800)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Enforcement Action Chooser
                Text(
                    text = "Enforcement Action",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEnforcementModeChanged("REMOVE_ELEMENT") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = app.enforcementMode == "REMOVE_ELEMENT",
                        onClick = { onEnforcementModeChanged("REMOVE_ELEMENT") },
                        colors = RadioButtonDefaults.colors(selectedColor = Teal400)
                    )
                    Column {
                        Text(
                            text = "Direct Element Removal (Invisible Reels Button)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Masks & hides Reels button in Instagram entirely so users cannot open it",
                            style = MaterialTheme.typography.bodySmall.copy(color = Teal400)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEnforcementModeChanged("OVERLAY") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = app.enforcementMode == "OVERLAY",
                        onClick = { onEnforcementModeChanged("OVERLAY") },
                        colors = RadioButtonDefaults.colors(selectedColor = Teal400)
                    )
                    Column {
                        Text(
                            text = "Mindfulness Pop-Up Overlay",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Displays gentle breath prompt over feed after custom timer",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEnforcementModeChanged("BACK_GESTURE") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = app.enforcementMode == "BACK_GESTURE",
                        onClick = { onEnforcementModeChanged("BACK_GESTURE") },
                        colors = RadioButtonDefaults.colors(selectedColor = Teal400)
                    )
                    Column {
                        Text(
                            text = "Smart Back Gesture",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Executes back gesture when Reels viewer is entered",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Reels / Short Feed Blocker & Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = Teal400,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Guard Reels / Shorts Feed",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Trigger prompt after watching Reels",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                        }
                    }
                    Switch(
                        checked = app.isReelsBlockEnabled,
                        onCheckedChange = { onToggleReelsBlock() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Teal600
                        )
                    )
                }

                if (app.isReelsBlockEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate800.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Reels Watch Time Limit before Pop-up",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                            )
                            Text(
                                text = "${app.reelsTimerSeconds}s",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Teal400
                                )
                            )
                        }
                        Slider(
                            value = app.reelsTimerSeconds.toFloat(),
                            onValueChange = { onReelsTimerChanged(it.toInt()) },
                            valueRange = 10f..180f,
                            steps = 16,
                            colors = SliderDefaults.colors(
                                thumbColor = Teal400,
                                activeTrackColor = Teal600,
                                inactiveTrackColor = Slate800
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle Main Feed Nudge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = Teal400,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Main Feed Mindfulness Pop-Up",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Pop-up reminder on main scrolling feed",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                        }
                    }
                    Switch(
                        checked = app.enableMainFeedNudge,
                        onCheckedChange = { onToggleMainFeedNudge() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Teal600
                        )
                    )
                }

                if (app.enableMainFeedNudge) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate800.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Main Feed Scroll Limit before Pop-up",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                            )
                            Text(
                                text = "${app.mainFeedTimerMinutes} mins",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Teal400
                                )
                            )
                        }
                        Slider(
                            value = app.mainFeedTimerMinutes.toFloat(),
                            onValueChange = { onMainFeedTimerChanged(it.toInt()) },
                            valueRange = 1f..15f,
                            steps = 13,
                            colors = SliderDefaults.colors(
                                thumbColor = Teal400,
                                activeTrackColor = Teal600,
                                inactiveTrackColor = Slate800
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Limit Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Daily Screen Limit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.LightGray
                            )
                        )
                        Text(
                            text = "${app.dailyLimitMinutes} mins / day",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Teal400
                            )
                        )
                    }

                    Slider(
                        value = app.dailyLimitMinutes.toFloat(),
                        onValueChange = { onLimitChanged(it.toInt()) },
                        valueRange = 5f..60f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = Teal400,
                            activeTrackColor = Teal600,
                            inactiveTrackColor = Slate800
                        )
                    )
                }
            }
        }
    }
}
