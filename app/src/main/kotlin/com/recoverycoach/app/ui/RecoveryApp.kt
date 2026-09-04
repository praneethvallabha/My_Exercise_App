package com.recoverycoach.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recoverycoach.app.data.RecoveryViewModel
import com.recoverycoach.app.ui.components.BottomNavBar
import com.recoverycoach.app.ui.navigation.RecoveryDestination
import com.recoverycoach.app.ui.screens.CheckInScreen
import com.recoverycoach.app.ui.screens.InsightsScreen
import com.recoverycoach.app.ui.screens.LogActivitySheetContent
import com.recoverycoach.app.ui.screens.SettingsScreen
import com.recoverycoach.app.ui.screens.TodayScreen
import com.recoverycoach.app.ui.screens.WeekScreen
import com.recoverycoach.app.ui.theme.RecoveryColors
import com.recoverycoach.app.ui.theme.RecoveryMotion
import com.recoverycoach.app.ui.theme.recoveryTween
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryApp(viewModel: RecoveryViewModel = viewModel()) {
    var currentDestination by remember { mutableStateOf(RecoveryDestination.TODAY) }
    var showLogSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // The app is usually left open overnight rather than relaunched, so the day
    // rollover has to be re-checked on resume, not only on first composition.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshForToday()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            // transitionSpec runs outside composition, so the specs are built here
            // and captured — calling recoveryTween() inside it would not compile.
            val slideSpec = recoveryTween<IntOffset>(RecoveryMotion.EMPHASIZED_MS)
            val fadeSpec = recoveryTween<Float>(RecoveryMotion.EMPHASIZED_MS)
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    // Slide toward the tab the user moved to, so the bottom bar's
                    // left-to-right order stays legible as spatial direction.
                    val direction = if (targetState.ordinal > initialState.ordinal) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                    (slideIntoContainer(direction, animationSpec = slideSpec) +
                        fadeIn(animationSpec = fadeSpec)) togetherWith
                        (slideOutOfContainer(direction, animationSpec = slideSpec) +
                            fadeOut(animationSpec = fadeSpec))
                },
                label = "destination",
            ) { destination ->
                when (destination) {
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
                    RecoveryDestination.INSIGHTS -> InsightsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    RecoveryDestination.WEEK -> WeekScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    RecoveryDestination.SETTINGS -> SettingsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                }
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
