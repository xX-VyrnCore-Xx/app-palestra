package com.vyrncore.palestra.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val ALLIEVO_DASHBOARD = "allievo_dashboard"
    const val PT_DASHBOARD = "pt_dashboard"
    const val WORKOUT_PLANS = "workout_plans"
    const val ACTIVE_WORKOUT = "active_workout/{sessionId}/{planId}"
    const val STATS = "stats"
    const val BODY_METRICS = "body_metrics"
    const val PT_CLIENT_DETAIL = "pt_client/{clientId}"
    const val PLAN_EDITOR = "plan_editor/{clientId}"
    const val REST_TIMER = "rest_timer/{seconds}"

    fun activeWorkout(sessionId: String, planId: String) = "active_workout/$sessionId/$planId"
    fun ptClientDetail(clientId: String) = "pt_client/$clientId"
    fun planEditor(clientId: String) = "plan_editor/$clientId"
    fun restTimer(seconds: Int) = "rest_timer/$seconds"
}
