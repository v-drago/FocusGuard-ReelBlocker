package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MindfulnessOverlayDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.GuardedAppsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SetupGuideScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FocusGuardTheme
import com.example.ui.theme.Teal400
import com.example.ui.theme.Teal600
import com.example.ui.viewmodel.FocusViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FocusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusGuardTheme {
                FocusGuardApp(viewModel = viewModel)
            }
        }
    }
}

enum class NavTab(
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    GUARDED_APPS("Apps", Icons.Filled.Apps, Icons.Outlined.Apps, "nav_guarded_apps"),
    FOCUS_TIMER("Breathe", Icons.Filled.SelfImprovement, Icons.Outlined.SelfImprovement, "nav_focus_timer"),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History, "nav_history"),
    GUIDE("Guide", Icons.Filled.HelpOutline, Icons.Outlined.HelpOutline, "nav_guide")
}

@Composable
fun FocusGuardApp(viewModel: FocusViewModel) {
    val selectedTabIndex by viewModel.selectedTab.collectAsState()
    val isSimulatingOverlay by viewModel.isSimulatingOverlay.collectAsState()

    val tabs = NavTab.entries.toTypedArray()

    if (isSimulatingOverlay) {
        MindfulnessOverlayDialog(
            appName = "Instagram",
            onDismiss = { viewModel.dismissSimulatedOverlay() }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar"),
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(index) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag(tab.testTag),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Teal400,
                            selectedTextColor = Teal400,
                            indicatorColor = Teal600.copy(alpha = 0.25f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTimer = { viewModel.selectTab(2) },
                    onNavigateToGuide = { viewModel.selectTab(4) }
                )
                1 -> GuardedAppsScreen(viewModel = viewModel)
                2 -> FocusTimerScreen(viewModel = viewModel)
                3 -> HistoryScreen(viewModel = viewModel)
                4 -> SetupGuideScreen(viewModel = viewModel)
            }
        }
    }
}
