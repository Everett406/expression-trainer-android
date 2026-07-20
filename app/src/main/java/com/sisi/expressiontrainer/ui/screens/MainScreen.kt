package com.sisi.expressiontrainer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sisi.expressiontrainer.R
import com.sisi.expressiontrainer.ui.components.FeedbackPane
import androidx.compose.material.icons.outlined.Assessment
import com.sisi.expressiontrainer.ui.components.RecordControls
import com.sisi.expressiontrainer.ui.components.StatsPane
import com.sisi.expressiontrainer.ui.components.SubtitlePane
import com.sisi.expressiontrainer.viewmodel.TrainerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TrainerViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToPromptEditor: () -> Unit,
    onNavigateToPasteText: () -> Unit,
    onNavigateToReport: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("表达训练")
                        Text(
                            text = viewModel.formatDuration(viewModel.elapsedSeconds.value),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("设置") },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("训练规则") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToPromptEditor()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("粘贴逐字稿") },
                            leadingIcon = { Icon(Icons.Default.Description, null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToPasteText()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
        },
        bottomBar = {
            Column {
                RecordControls(
                    isRecording = viewModel.isRecording.value,
                    isPaused = viewModel.isPaused.value,
                    canReport = viewModel.subtitles.isNotEmpty() && !viewModel.isRecording.value,
                    onStart = { viewModel.startRecording() },
                    onPause = { viewModel.pauseRecording() },
                    onResume = { viewModel.resumeRecording() },
                    onStop = { viewModel.stopRecording() },
                    onReport = { onNavigateToReport() }
                )
                NavigationBar {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selectedTab == 0) Icons.Default.Subtitles else Icons.Outlined.Subtitles,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(R.string.tab_subtitle)) },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selectedTab == 1) Icons.Default.Assessment else Icons.Outlined.Assessment,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(R.string.tab_stats)) },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selectedTab == 2) Icons.Default.ChatBubbleOutline else Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(R.string.tab_feedback)) },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> SubtitlePane(subtitles = viewModel.subtitles)
                1 -> StatsPane(
                    stats = viewModel.reportStats.value,
                    analysisResults = viewModel.analysisResults
                )
                2 -> FeedbackPane(feedbackItems = viewModel.feedbackItems)
            }
        }
    }
}
