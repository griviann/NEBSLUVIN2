package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CommentsScreen
import com.example.ui.screens.ContentScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.KisumuEmerald
import com.example.ui.theme.KisumuForestGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusActionFailed
import com.example.ui.theme.StatusReady
import com.example.ui.viewmodel.MainViewModel

enum class NavigationTab(val title: String, val icon: ImageVector, val tag: String) {
    CHAT("AI Chat", Icons.Default.ChatBubble, "nav_chat"),
    CONTENT("Content", Icons.Default.PostAdd, "nav_content"),
    COMMENTS("Comments", Icons.Default.Forum, "nav_comments"),
    SCHEDULE("Schedule", Icons.Default.Schedule, "nav_schedule"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(NavigationTab.CHAT) }
                val snackbarHostState = remember { SnackbarHostState() }

                val feedbackMessage by viewModel.userFeedbackMessage.collectAsState()
                val healthReport by viewModel.healthReport.collectAsState()
                val postDrafts by viewModel.postDrafts.collectAsState()
                val pageComments by viewModel.pageComments.collectAsState()

                val pendingApprovalCount = postDrafts.count { it.status == "AWAITING APPROVAL" }
                val pendingCommentsCount = pageComments.count { it.replyStatus == "PENDING_REVIEW" }

                LaunchedEffect(feedbackMessage) {
                    feedbackMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearFeedback()
                    }
                }

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (healthReport?.facebookConnected == true) StatusReady else StatusActionFailed
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "RealSatisfaction AI",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            },
                            actions = {
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Text(
                                        text = "Kisumu, KE",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = KisumuForestGreen
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            NavigationTab.values().forEach { tab ->
                                val badgeCount = when (tab) {
                                    NavigationTab.CONTENT -> pendingApprovalCount
                                    NavigationTab.COMMENTS -> pendingCommentsCount
                                    else -> 0
                                }

                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    label = { Text(tab.title, fontSize = 11.sp) },
                                    icon = {
                                        if (badgeCount > 0) {
                                            BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
                                                Icon(tab.icon, contentDescription = tab.title)
                                            }
                                        } else {
                                            Icon(tab.icon, contentDescription = tab.title)
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = KisumuForestGreen,
                                        selectedTextColor = KisumuForestGreen,
                                        indicatorColor = KisumuForestGreen.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
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
                        when (currentTab) {
                            NavigationTab.CHAT -> ChatScreen(
                                viewModel = viewModel,
                                onNavigateToContent = { currentTab = NavigationTab.CONTENT }
                            )
                            NavigationTab.CONTENT -> ContentScreen(viewModel = viewModel)
                            NavigationTab.COMMENTS -> CommentsScreen(viewModel = viewModel)
                            NavigationTab.SCHEDULE -> ScheduleScreen(viewModel = viewModel)
                            NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
