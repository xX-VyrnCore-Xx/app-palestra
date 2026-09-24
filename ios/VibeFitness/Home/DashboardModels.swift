import Foundation

/// Mirrors the Android app's `workout_plans` row (`WorkoutPlanDto`) - only the fields the Schede
/// list and Home's "next workout" card actually render.
struct WorkoutPlan: Codable, Identifiable {
    let id: String
    let name: String
    let category: String?
    let estimatedMinutes: Int?
    let createdAt: String

    enum CodingKeys: String, CodingKey {
        case id, name, category
        case estimatedMinutes = "estimated_minutes"
        case createdAt = "created_at"
    }
}

/// Mirrors `memberships` - same status thresholds as the Android `MembershipInfo.status` (expired
/// once past end date, "in scadenza" inside the last 7 days) so both apps agree on what counts as
/// urgent.
struct Membership: Codable {
    let planLabel: String?
    let startDate: String
    let endDate: String

    enum CodingKeys: String, CodingKey {
        case planLabel = "plan_label"
        case startDate = "start_date"
        case endDate = "end_date"
    }

    enum Status {
        case active, expiringSoon, expired
    }

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.timeZone = TimeZone(identifier: "UTC")
        return formatter
    }()

    var endDateValue: Date? { Self.dateFormatter.date(from: endDate) }

    var daysUntilEnd: Int? {
        guard let end = endDateValue else { return nil }
        let startOfToday = Calendar.current.startOfDay(for: Date())
        return Calendar.current.dateComponents([.day], from: startOfToday, to: end).day
    }

    var status: Status {
        guard let days = daysUntilEnd else { return .active }
        if days < 0 { return .expired }
        if days <= 7 { return .expiringSoon }
        return .active
    }
}

/// One `workout_sessions` row - just enough to count completed workouts, show the last date, and
/// compute the streak (a session only counts once it's finished, matching the Android app).
struct WorkoutSession: Codable {
    let startedAt: String
    let endedAt: String?

    enum CodingKeys: String, CodingKey {
        case startedAt = "started_at"
        case endedAt = "ended_at"
    }
}

/// Same streak-length milestones as the Android app's `BADGE_MILESTONES`/`WORKOUT_COUNT_MILESTONES`
/// (`HomeViewModel.kt`) - kept in sync by hand since this is a separate codebase, so a badge that
/// unlocks in the app unlocks here too. Volume-based badges aren't ported yet (need per-set weight
/// data this app doesn't fetch).
enum Milestones {
    static let streakDays = [3, 7, 14, 30, 60, 100]
    static let workoutCount = [5, 10, 25, 50, 100, 250]
}
