import Foundation

/// Fetches the allievo's own dashboard data - assigned plans, current membership, completed
/// sessions, streak - the same queries the Android `HomeScreen`/`WorkoutPlansScreen` make, scoped
/// by the same RLS policies (a user only ever sees rows where `user_id`/`assigned_to_user_id` is
/// their own auth uid).
@MainActor
final class DashboardViewModel: ObservableObject {
    @Published var plans: [WorkoutPlan] = []
    @Published var membership: Membership?
    @Published var completedSessions = 0
    @Published var streakDays = 0
    @Published var longestStreakDays = 0
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
        let sessions = await sessionsTask
        let doneDates = completedDates(from: sessions)
        completedSessions = doneDates.count
        streakDays = currentStreak(doneDates)
        longestStreakDays = longestStreak(doneDates)
    }

    /// One calendar day per finished session (matches the Android `HomeViewModel`: a session only
    /// counts once `ended_at` is set, and multiple sessions the same day collapse to one date).
    private func completedDates(from sessions: [WorkoutSession]) -> Set<DateComponents> {
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let isoFormatterNoFraction = ISO8601DateFormatter()

        var dates = Set<DateComponents>()
        for session in sessions {
            guard let endedAt = session.endedAt else { continue }
            let date = isoFormatter.date(from: endedAt) ?? isoFormatterNoFraction.date(from: endedAt)
            guard let date else { continue }
            dates.insert(Calendar.current.dateComponents([.year, .month, .day], from: date))
        }
        return dates
    }

    private func currentStreak(_ doneDates: Set<DateComponents>) -> Int {
        var streak = 0
        var day = Calendar.current.startOfDay(for: Date())
        while doneDates.contains(Calendar.current.dateComponents([.year, .month, .day], from: day)) {
            streak += 1
            guard let previous = Calendar.current.date(byAdding: .day, value: -1, to: day) else { break }
            day = previous
        }
        return streak
    }

    private func longestStreak(_ doneDates: Set<DateComponents>) -> Int {
        let calendar = Calendar.current
        let sortedDays = doneDates.compactMap { calendar.date(from: $0) }.sorted()
        var longest = 0
        var current = 0
        var previousDay: Date?
        for day in sortedDays {
            if let previousDay, let expected = calendar.date(byAdding: .day, value: 1, to: previousDay), calendar.isDate(expected, inSameDayAs: day) {
                current += 1
            } else {
                current = 1
            }
            longest = max(longest, current)
            previousDay = day
        }
        return longest
    }
}
