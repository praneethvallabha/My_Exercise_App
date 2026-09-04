package com.recoverycoach.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recoverycoach.app.data.RecoveryViewModel
import com.recoverycoach.app.ui.components.BottomNavBar
import com.recoverycoach.app.ui.navigation.RecoveryDestination
import com.recoverycoach.app.ui.screens.CheckInScreen
import com.recoverycoach.app.ui.screens.LogActivitySheetContent
import com.recoverycoach.app.ui.screens.SettingsScreen
import com.recoverycoach.app.ui.screens.TodayScreen
import com.recoverycoach.app.ui.screens.WeekScreen
import com.recoverycoach.app.ui.theme.RecoveryColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryApp(viewModel: RecoveryViewModel = viewModel()) {
    var currentDestination by remember { mutableStateOf(RecoveryDestination.TODAY) }
    var showLogSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = RecoveryColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavBar(current = currentDestination, onSelect = { currentDestination = it })
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(RecoveryColors.Background),
        ) {
            when (currentDestination) {
                RecoveryDestination.TODAY -> TodayScreen(
                    viewModel = viewModel,
                    onEditActivity = { showLogSheet = true },
                    modifier = Modifier.fillMaxSize(),
                )
                RecoveryDestination.CHECK_IN -> CheckInScreen(
                    viewModel = viewModel,
                    onSave = {
                        currentDestination = RecoveryDestination.TODAY
                        scope.launch { snackbarHostState.showSnackbar("Check-in saved.") }
                    },
                    onReportWarningSymptom = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Warning symptom flow isn't part of this build yet.")
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                RecoveryDestination.WEEK -> WeekScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                RecoveryDestination.SETTINGS -> SettingsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            }
        }
    }

    if (showLogSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLogSheet = false },
            containerColor = RecoveryColors.Surface,
        ) {
            LogActivitySheetContent(
                current = viewModel.activity,
                onCancel = { showLogSheet = false },
                onSave = { updated ->
                    viewModel.updateActivity(updated)
                    showLogSheet = false
                },
            )
        }
    }
}
