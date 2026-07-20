package com.sisi.expressiontrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sisi.expressiontrainer.ui.ExpressionTrainerApp
import com.sisi.expressiontrainer.ui.theme.ExpressionTrainerTheme
import com.sisi.expressiontrainer.viewmodel.TrainerViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ExpressionTrainerApplication

        setContent {
            ExpressionTrainerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val audioPermissionState = rememberPermissionState(
                        android.Manifest.permission.RECORD_AUDIO
                    )

                    val trainerViewModel: TrainerViewModel = viewModel(
                        factory = TrainerViewModel.Factory(
                            application = app,
                            settingsDataStore = app.settingsDataStore,
                            lexiconAnalyzer = app.lexiconAnalyzer
                        )
                    )

                    if (audioPermissionState.status.isGranted) {
                        ExpressionTrainerApp(viewModel = trainerViewModel)
                    } else {
                        PermissionRequestScreen(
                            onRequestPermission = { audioPermissionState.launchPermissionRequest() }
                        )
                    }
                }
            }
        }
    }
}
