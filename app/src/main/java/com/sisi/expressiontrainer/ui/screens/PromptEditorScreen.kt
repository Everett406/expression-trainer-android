package com.sisi.expressiontrainer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sisi.expressiontrainer.data.model.CustomPrompt
import com.sisi.expressiontrainer.viewmodel.TrainerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptEditorScreen(
    viewModel: TrainerViewModel,
    onBack: () -> Unit
) {
    val currentPrompt = viewModel.customPrompt.value
    var goals by remember { mutableStateOf(currentPrompt.goals) }
    var customRules by remember { mutableStateOf(currentPrompt.customRules) }
    var styleRef by remember { mutableStateOf(currentPrompt.styleRef) }
    var customWords by remember { mutableStateOf(currentPrompt.customWords) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("训练规则") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            goals = ""
                            customRules = ""
                            styleRef = ""
                            customWords = ""
                            viewModel.updateCustomPrompt(CustomPrompt())
                            scope.launch {
                                snackbarHostState.showSnackbar("已恢复默认")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "恢复默认")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = goals,
                onValueChange = { goals = it },
                label = { Text("训练目标") },
                placeholder = { Text("例如：减少填充词、提高结论先行能力") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = customRules,
                onValueChange = { customRules = it },
                label = { Text("自定义规则") },
                placeholder = { Text("额外希望 AI 教练关注的规则") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = styleRef,
                onValueChange = { styleRef = it },
                label = { Text("参考风格") },
                placeholder = { Text("例如：像乔布斯发布会那样简洁有力") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = customWords,
                onValueChange = { customWords = it },
                label = { Text("额外口癖词") },
                placeholder = { Text("用换行或逗号分隔，例如：其实吧 / 说实话") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.updateCustomPrompt(
                        CustomPrompt(
                            goals = goals.trim(),
                            customRules = customRules.trim(),
                            styleRef = styleRef.trim(),
                            customWords = customWords.trim()
                        )
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar("已保存")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}
