package com.example.jobsheetbook.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.jobsheetbook.ui.screens.*
import com.example.jobsheetbook.ui.viewmodel.JobViewModel

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object EntryFlow : Screen("entry_flow?jobId={jobId}") {
        fun createRoute(jobId: Long?) = if (jobId != null) "entry_flow?jobId=$jobId" else "entry_flow"
    }
    object EntryForm : Screen("entry_form")
    object Camera : Screen("camera")
    object Scanner : Screen("scanner")
    object Preview : Screen("preview/{jobId}") {
        fun createRoute(jobId: Long) = "preview/$jobId"
    }
    object ChipLevelTracker : Screen("chip_level_tracker")
    object Reports : Screen("reports")
    object Customers : Screen("customers")
    object Settings : Screen("settings")
    object TrashBin : Screen("trash_bin")
}

enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Jobs(Screen.Dashboard.route, "Jobs", Icons.Default.Home),
    ChipLevel(Screen.ChipLevelTracker.route, "Chip-Level", Icons.Default.Build),
    Settings(Screen.Settings.route, "Settings", Icons.Default.Settings)
}

@Composable
fun JobSheetNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNewJobClick = { navController.navigate(Screen.EntryFlow.createRoute(null)) },
                onJobClick = { jobId -> navController.navigate(Screen.Preview.createRoute(jobId)) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onOpenTrash = { navController.navigate(Screen.TrashBin.route) })
        }

        composable(Screen.TrashBin.route) {
            TrashBinScreen(onBack = { navController.popBackStack() })
        }
        
        composable(Screen.ChipLevelTracker.route) {
            ChipLevelTrackerScreen(
                onBack = { navController.popBackStack() },
                onNavigateToJob = { jobId -> navController.navigate(Screen.Preview.createRoute(jobId)) }
            )
        }

        navigation(
            startDestination = Screen.EntryForm.route, 
            route = Screen.EntryFlow.route
        ) {
            composable(Screen.EntryForm.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.EntryFlow.route)
                }
                val viewModel = hiltViewModel<JobViewModel>(parentEntry)
                
                val jobId = parentEntry.arguments?.getString("jobId")?.toLongOrNull()
                val resetPending = rememberSaveable(jobId) { mutableStateOf(true) }
                LaunchedEffect(jobId) {
                    if (jobId != null) {
                        viewModel.loadJobForEditing(jobId)
                    } else if (resetPending.value) {
                        resetPending.value = false
                        viewModel.resetForm()
                    }
                }
                
                EntryFormScreen(
                    viewModel = viewModel,
                    onSave = { savedId ->
                        if (jobId != null) {
                            if (savedId == jobId) {
                                navController.popBackStack()
                            } else {
                                val popped = navController.popBackStack(Screen.Dashboard.route, false)
                                if (!popped) {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.EntryFlow.route) { inclusive = true }
                                    }
                                }
                            }
                        } else {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.EntryFlow.route) { inclusive = true }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onCaptureImages = { navController.navigate(Screen.Camera.route) }
                )
            }
            composable(Screen.Camera.route) {
                val viewModel = hiltViewModel<JobViewModel>(
                    remember(it) { navController.getBackStackEntry(Screen.EntryFlow.route) }
                )
                CameraScreen(
                    onImageCaptured = { uri ->
                        viewModel.addCapturedImage(uri)
                        navController.popBackStack()
                    },
                    onError = { /* Handle error */ },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Scanner.route) {
            ScannerScreen(
                onQrCodeDetected = { jobIdStr ->
                    val scannedId = jobIdStr.toLongOrNull()
                    if (scannedId != null) {
                        navController.navigate(Screen.Preview.createRoute(scannedId)) {
                            popUpTo(Screen.Scanner.route) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Preview.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")?.toLongOrNull() ?: 0L
            JobPreviewScreen(
                jobId = jobId,
                onBack = { navController.popBackStack() },
                onEditClick = { id -> 
                    navController.navigate(Screen.EntryFlow.createRoute(id))
                },
                onDeleteClick = { _ ->
                    navController.popBackStack()
                }
            )
        }
    }
}
