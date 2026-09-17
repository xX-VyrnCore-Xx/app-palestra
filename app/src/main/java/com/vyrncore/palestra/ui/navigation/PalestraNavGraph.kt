package com.vyrncore.palestra.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.ui.RootViewModel
import com.vyrncore.palestra.ui.auth.LoginScreen
import com.vyrncore.palestra.ui.auth.RegisterScreen
import com.vyrncore.palestra.ui.dashboard.AllievoDashboardScreen
import com.vyrncore.palestra.ui.pt.PlanEditorScreen
import com.vyrncore.palestra.ui.pt.PtClientDetailScreen
import com.vyrncore.palestra.ui.pt.PtDashboardScreen
import com.vyrncore.palestra.ui.timer.RestTimerScreen
import com.vyrncore.palestra.ui.workout.ActiveWorkoutScreen

@Composable
fun PalestraNavGraph() {
    val navController = rememberNavController()
    val rootViewModel: RootViewModel = hiltViewModel()
    val role by rootViewModel.role.collectAsState()

    val startDestination = if (rootViewModel.startUserId != null) "home" else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = { userId ->
                    rootViewModel.setLoggedInUser(userId)
                    navController.navigate("home") { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { userId ->
                    rootViewModel.setLoggedInUser(userId)
                    navController.navigate("home") { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
            )
        }
        composable("home") {
            when (role) {
                UserRole.PT -> PtDashboardScreen(
                    onOpenClient = { clientId -> navController.navigate(Routes.ptClientDetail(clientId)) },
                )
                UserRole.ALLIEVO -> AllievoDashboardScreen(
                    onOpenSession = { sessionId, planId ->
                        navController.navigate(Routes.activeWorkout(sessionId, planId))
                    },
                )
                null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        composable(
            Routes.ACTIVE_WORKOUT,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }, navArgument("planId") { type = NavType.StringType }),
        ) {
            ActiveWorkoutScreen(
                onFinished = { navController.popBackStack() },
                onOpenRestTimer = { seconds -> navController.navigate(Routes.restTimer(seconds)) },
            )
        }
        composable(
            Routes.REST_TIMER,
            arguments = listOf(navArgument("seconds") { type = NavType.IntType }),
        ) {
            RestTimerScreen(onClose = { navController.popBackStack() })
        }
        composable(
            Routes.PT_CLIENT_DETAIL,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType }),
        ) {
            PtClientDetailScreen(
                onCreatePlan = { clientId -> navController.navigate(Routes.planEditor(clientId)) },
            )
        }
        composable(
            Routes.PLAN_EDITOR,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType }),
        ) {
            PlanEditorScreen(onSaved = { navController.popBackStack() })
        }
    }
}
