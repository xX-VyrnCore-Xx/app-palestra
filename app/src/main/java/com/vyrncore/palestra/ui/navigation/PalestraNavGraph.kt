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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.vyrncore.palestra.ui.achievements.AchievementsScreen
import com.vyrncore.palestra.ui.auth.LoginScreen
import com.vyrncore.palestra.ui.auth.PtWebAppScreen
import com.vyrncore.palestra.ui.auth.RegisterScreen
import com.vyrncore.palestra.ui.auth.ResetPasswordScreen
import com.vyrncore.palestra.ui.bodymetrics.BodyMetricsScreen
import com.vyrncore.palestra.ui.calendar.CalendarScreen
import com.vyrncore.palestra.ui.chat.ChatThreadScreen
import com.vyrncore.palestra.ui.dashboard.AllievoDashboardScreen
import com.vyrncore.palestra.ui.history.HistoryScreen
import com.vyrncore.palestra.ui.locations.LocationsScreen
import com.vyrncore.palestra.ui.profile.ProfileScreen
import com.vyrncore.palestra.ui.search.GlobalSearchScreen
import com.vyrncore.palestra.ui.timer.RestTimerScreen
import com.vyrncore.palestra.ui.welcome.WelcomeScreen
import com.vyrncore.palestra.ui.workout.ActiveWorkoutScreen
import com.vyrncore.palestra.ui.workout.WorkoutSummaryScreen

@Composable
fun PalestraNavGraph(rootViewModel: RootViewModel) {
    val navController = rememberNavController()
    val role by rootViewModel.role.collectAsStateWithLifecycle()
    val needsOnboarding by rootViewModel.needsOnboarding.collectAsStateWithLifecycle()
    val pendingChatPeerId by rootViewModel.pendingChatPeerId.collectAsStateWithLifecycle()
    val pendingRecoveryAccessToken by rootViewModel.pendingRecoveryAccessToken.collectAsStateWithLifecycle()

    val startDestination = if (rootViewModel.startUserId != null) "home" else Routes.LOGIN

    // Tapping a chat notification: the allievo's chat is a tab on Home, so there's nothing to
    // navigate to - just clear the pending request once the role is known.
    androidx.compose.runtime.LaunchedEffect(pendingChatPeerId, role) {
        if (pendingChatPeerId != null && role != null) rootViewModel.consumeChatDeepLink()
    }

    // Fires from any screen (including the login/register flow, since a signed-out user is
    // exactly who taps a password-reset link) - navigating here doesn't depend on `role` the way
    // the chat deep link does, so it doesn't need to wait for one.
    androidx.compose.runtime.LaunchedEffect(pendingRecoveryAccessToken) {
        val token = pendingRecoveryAccessToken ?: return@LaunchedEffect
        navController.navigate(Routes.resetPassword(token))
        rootViewModel.consumePasswordRecovery()
    }

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
                    // A fresh allievo lands on the Welcome wizard.
                    rootViewModel.setNewlyRegisteredUser(userId)
                    navController.navigate("home") { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
        composable("home") {
            when (role) {
                // PTs work from the web management app: the Android app is for allievi only.
                UserRole.PT -> PtWebAppScreen(
                    onSignOut = {
                        rootViewModel.signOut { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
                    },
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
                        onOpenLocations = { navController.navigate(Routes.LOCATIONS) },
                        onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
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
        composable(Routes.LOCATIONS) {
            LocationsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ACHIEVEMENTS) {
            AchievementsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.RESET_PASSWORD,
            arguments = listOf(navArgument("accessToken") { type = NavType.StringType }),
        ) { backStackEntry ->
            val accessToken = checkNotNull(backStackEntry.arguments?.getString("accessToken"))
            ResetPasswordScreen(
                accessToken = accessToken,
                onPasswordUpdated = { userId ->
                    rootViewModel.setLoggedInUser(userId)
                    navController.navigate("home") { popUpTo(0) { inclusive = true } }
                },
            )
        }
        composable(Routes.SEARCH) {
            GlobalSearchScreen(
                onBack = { navController.popBackStack() },
                onStartSession = { sessionId, planId ->
                    navController.navigate(Routes.activeWorkout(sessionId, planId)) { popUpTo(Routes.SEARCH) { inclusive = true } }
                },
                onOpenChat = { peerId ->
                    navController.navigate(Routes.chatThread(peerId)) { popUpTo(Routes.SEARCH) { inclusive = true } }
                },
            )
        }
    }
}
