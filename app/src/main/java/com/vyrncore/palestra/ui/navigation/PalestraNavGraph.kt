package com.vyrncore.palestra.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.ui.RootViewModel
import com.vyrncore.palestra.ui.auth.LoginScreen
import com.vyrncore.palestra.ui.auth.RegisterScreen
import com.vyrncore.palestra.ui.bodymetrics.BodyMetricsScreen
import com.vyrncore.palestra.ui.chat.ChatThreadScreen
import com.vyrncore.palestra.ui.dashboard.AllievoDashboardScreen
import com.vyrncore.palestra.ui.profile.ProfileScreen
import com.vyrncore.palestra.ui.pt.PlanEditorScreen
import com.vyrncore.palestra.ui.pt.PtClientDetailScreen
import com.vyrncore.palestra.ui.pt.PtDashboardScreen
import com.vyrncore.palestra.ui.timer.RestTimerScreen
import com.vyrncore.palestra.ui.welcome.WelcomeScreen
import com.vyrncore.palestra.ui.workout.ActiveWorkoutScreen

@Composable
fun PalestraNavGraph(rootViewModel: RootViewModel) {
    val navController = rememberNavController()
    val role by rootViewModel.role.collectAsState()
    val needsOnboarding by rootViewModel.needsOnboarding.collectAsState()

    val startDestination = if (rootViewModel.startUserId != null) "home" else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) + slideInHorizontally(initialOffsetX = { it / 6 }) },
        exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) },
        popEnterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) },
        popExitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) + slideOutHorizontally(targetOffsetX = { it / 6 }) },
    ) {
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
                    onOpenChat = { clientId -> navController.navigate(Routes.chatThread(clientId)) },
                    onSignedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } },
                )
                UserRole.ALLIEVO -> if (needsOnboarding) {
                    WelcomeScreen(onFinished = { rootViewModel.markOnboardingComplete() })
                } else {
                    AllievoDashboardScreen(
                        onOpenSession = { sessionId, planId ->
                            navController.navigate(Routes.activeWorkout(sessionId, planId))
                        },
                        onOpenBodyMetrics = { navController.navigate(Routes.BODY_METRICS) },
                        onSignedOut = {
                            navController.navigate(Routes.LOGIN) { popUpTo(0) }
                        },
                    )
                }
                null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        composable(Routes.BODY_METRICS) {
            BodyMetricsScreen()
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onOpenBodyMetrics = { navController.navigate(Routes.BODY_METRICS) },
                onSignedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } },
            )
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
                onOpenChat = { clientId -> navController.navigate(Routes.chatThread(clientId)) },
            )
        }
        composable(
            Routes.PLAN_EDITOR,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType }),
        ) {
            PlanEditorScreen(onSaved = { navController.popBackStack() })
        }
        composable(
            Routes.CHAT_THREAD,
            arguments = listOf(navArgument("peerId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val peerId = checkNotNull(backStackEntry.arguments?.getString("peerId"))
            ChatThreadScreen(peerId = peerId, onBack = { navController.popBackStack() })
        }
    }
}
