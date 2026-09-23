import Foundation

/// Fetches the allievo's own dashboard data - assigned plans, current membership, completed
/// session count - the same three queries the Android `HomeScreen`/`WorkoutPlansScreen` make,
/// scoped by the same RLS policies (a user only ever sees rows where `user_id`/`assigned_to_user_id`
/// is their own auth uid).
@MainActor
final class DashboardViewModel: ObservableObject {
    @Published var plans: [WorkoutPlan] = []
    @Published var membership: Membership?
    @Published var completedSessions = 0
    @Published var isLoading = true

    private let client = SupabaseService.client

    func load() async {
        isLoading = true
        defer { isLoading = false }

        guard let userId = try? await client.auth.session.user.id.uuidString else { return }

        async let plansTask: [WorkoutPlan] = (try? client.from("workout_plans")
            .select()
            .eq("assigned_to_user_id", value: userId)
            .order("created_at", ascending: false)
            .execute()
            .value) ?? []

        async let membershipTask: [Membership] = (try? client.from("memberships")
            .select()
            .eq("user_id", value: userId)
            .order("end_date", ascending: false)
            .limit(1)
            .execute()
            .value) ?? []

        async let sessionsTask: [WorkoutSession] = (try? client.from("workout_sessions")
            .select()
            .eq("user_id", value: userId)
            .execute()
            .value) ?? []

        plans = await plansTask
        membership = await membershipTask.first
        completedSessions = await sessionsTask.count
    }
}
