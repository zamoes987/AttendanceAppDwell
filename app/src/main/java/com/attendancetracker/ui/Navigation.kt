package com.attendancetracker.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.attendancetracker.data.auth.AuthManager
import com.attendancetracker.data.auth.BiometricHelper
import com.attendancetracker.data.notifications.NotificationHelper
import com.attendancetracker.data.repository.PreferencesRepository
import com.attendancetracker.ui.screens.HistoryScreen
import com.attendancetracker.ui.screens.HomeScreen
import com.attendancetracker.ui.screens.MembersScreen
import com.attendancetracker.ui.screens.SettingsScreen
import com.attendancetracker.ui.screens.StatisticsScreen
import com.attendancetracker.viewmodel.AttendanceViewModel
import com.attendancetracker.viewmodel.SettingsViewModel

/**
 * Navigation routes for the app.
 */
private const val ROUTE_HOME = "home"
private const val ROUTE_HISTORY = "history"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_MEMBERS = "members"
private const val ROUTE_STATISTICS = "statistics"

/**
 * Main navigation setup for the Attendance Tracker app.
 *
 * Defines navigation graph with screens:
 * - Home: Main attendance marking screen (start destination)
 * - History: Past attendance records screen
 * - Settings: App configuration screen
 * - Members: Member management screen
 *
 * @param viewModel The AttendanceViewModel shared across screens
 * @param preferencesRepository Repository for settings
 * @param onSignOut Callback to sign out the current user
 * @param authManager Authentication manager for session handling
 * @param biometricHelper Helper for biometric authentication
 */
@Composable
fun Navigation(
    viewModel: AttendanceViewModel,
    preferencesRepository: PreferencesRepository,
    onSignOut: () -> Unit,
    authManager: AuthManager,
    biometricHelper: BiometricHelper
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Use remember to prevent ViewModel recreation on recomposition
    val settingsViewModel = remember {
        val notificationHelper = NotificationHelper(context, preferencesRepository)
        SettingsViewModel(preferencesRepository, notificationHelper)
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME
    ) {
        // Home screen route
        composable(ROUTE_HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToHistory = {
                    navController.navigate(ROUTE_HISTORY)
                },
                onNavigateToSettings = {
                    navController.navigate(ROUTE_SETTINGS)
                },
                onNavigateToMembers = {
                    navController.navigate(ROUTE_MEMBERS)
                },
                onNavigateToStatistics = {
                    navController.navigate(ROUTE_STATISTICS)
                }
            )
        }

        // History screen route
        composable(ROUTE_HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings screen route
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignOut = onSignOut,
                authManager = authManager,
                biometricHelper = biometricHelper
            )
        }

        // Members screen route
        composable(ROUTE_MEMBERS) {
            MembersScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Statistics screen route
        composable(ROUTE_STATISTICS) {
            StatisticsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
