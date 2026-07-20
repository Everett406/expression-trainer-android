package com.sisi.expressiontrainer.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sisi.expressiontrainer.ui.screens.MainScreen
import com.sisi.expressiontrainer.ui.screens.PasteTextScreen
import com.sisi.expressiontrainer.ui.screens.PromptEditorScreen
import com.sisi.expressiontrainer.ui.screens.ReportScreen
import com.sisi.expressiontrainer.ui.screens.SettingsScreen
import com.sisi.expressiontrainer.viewmodel.TrainerViewModel

@Composable
fun ExpressionTrainerApp(viewModel: TrainerViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToPromptEditor = { navController.navigate("prompt_editor") },
                onNavigateToPasteText = { navController.navigate("paste_text") },
                onNavigateToReport = { navController.navigate("report") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("prompt_editor") {
            PromptEditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("paste_text") {
            PasteTextScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("report") {
            ReportScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
