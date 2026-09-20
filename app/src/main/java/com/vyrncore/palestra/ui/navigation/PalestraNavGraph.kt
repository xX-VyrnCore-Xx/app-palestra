package com.vyrncore.palestra.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import com.vyrncore.palestra.ui.calendar.CalendarScreen
import com.vyrncore.palestra.ui.chat.ChatThreadScreen
import com.vyrncore.palestra.ui.dashboard.AllievoDashboardScreen
import com.vyrncore.palestra.ui.history.HistoryScreen
import com.vyrncore.palestra.ui.profile.ProfileScreen
import com.vyrncore.palestra.ui.pt.PlanEditorScreen
import com.vyrncore.palestra.ui.pt.ProgramEditorScreen
import com.vyrncore.palestra.ui.pt.PtClientDetailScreen
import com.vyrncore.palestra.ui.pt.PtDashboardScreen
import com.vyrncore.palestra.ui.search.GlobalSearchScreen
import com.vyrncore.palestra.ui.timer.RestTimerScreen
import com.vyrncore.palestra.ui.welcome.WelcomeScreen
import com.vyrncore.palestra.ui.workout.ActiveWorkoutScreen
import com.vyrncore.palestra.ui.workout.WorkoutSummaryScreen

@Composable
fun PalestraNavGraph(rootViewModel: RootViewModel) {
    val navController = rememberNavController()
    val role by rootViewModel.role.collectAsState()
    val needsOnboarding by rootViewModel.needsOnboarding.collectAsState()

    val startDestination = if (rootViewModel.startUserId != null) "home" else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination,
        // A sober, macOS-like fluidity for screen transitions: motion follows a critically-damped
        // spring instead of a fixed-duration tween, so it settles naturally rather than stopping abruptly.
        enterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideInHorizontally(
                    initialOffsetX = { it / 6 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                )
        },
        exitTransition = { fadeOut(animationSpec = tween(160)) },
        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
        popExitTransition = {
            fadeOut(animationSpec = tween(160)) +
                slideOutHorizontally(
                    targetOffsetX = { it / 6 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                )
        },
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = { userId ->
                    // Login never onboards: a returning user goes straight to their dashboard.
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
                    // A fresh ALLIEVO lands on the Welcome wizard; a PT lands on their dashboard.
                    rootViewModel.requestOnboarding()
                    navController.navigate("home") { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
        composable("home") {
            when (role) {
                UserRole.PT -> PtDashboardScreen(
                    onOpenClient = { clientId -> navController.navigate(Routes.ptClientDetail(clientId)) },
                    onOpenChat = { clientId -> navController.navigate(Routes.chatThread(clientId)) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
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
                        onOpenHistory = { navController.navigate(Routes.HISTORY) },
                        onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                        onOpenSearch = { navController.navigate(Routes.SEARCH) },
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
                onFinished = { sessionId, planId ->
                    navController.navigate(Routes.workoutSummary(sessionId, planId)) {
                        popUpTo(Routes.ACTIVE_WORKOUT) { inclusive = true }
                    }
                },
                onOpenRestTimer = { seconds -> navController.navigate(Routes.restTimer(seconds)) },
            )
        }
        composable(
            Routes.WORKOUT_SUMMARY,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }, navArgument("planId") { type = NavType.StringType }),
        ) {
            WorkoutSummaryScreen(onDone = { navController.popBackStack() })
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
                onCreateProgram = { clientId -> navController.navigate(Routes.programEditor(clientId)) },
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
            Routes.PROGRAM_EDITOR,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType }),
        ) {
            ProgramEditorScreen(onSaved = { navController.popBackStack() })
        }
        composable(
            Routes.CHAT_THREAD,
            arguments = listOf(navArgument("peerId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val peerId = checkNotNull(backStackEntry.arguments?.getString("peerId"))
            ChatThreadScreen(peerId = peerId, onBack = { navController.popBackStack() })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CALENDAR) {
            CalendarScreen(
                onOpenSession = { sessionId, planId -> navController.navigate(Routes.activeWorkout(sessionId, planId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SEARCH) {
            GlobalSearchScreen(
                onBack = { navController.popBackStack() },
                onStartSession = { sessionId, planId ->
                    navController.navigate(Routes.activeWorkout(sessionId, planId)) { popUpTo(Routes.SEARCH) { inclusive = true } }
                },
                onOpenClient = { clientId ->
                    navController.navigate(Routes.ptClientDetail(clientId)) { popUpTo(Routes.SEARCH) { inclusive = true } }
                },
                onOpenChat = { peerId ->
                    navController.navigate(Routes.chatThread(peerId)) { popUpTo(Routes.SEARCH) { inclusive = true } }
                },
            )
        }
    }
}
