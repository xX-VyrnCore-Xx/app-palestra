import SwiftUI

/// The real Home dashboard: greeting, streak, membership status, completed-workouts count and the
/// next assigned plan - the iOS equivalent of the Android app's `HomeScreen` core cards (the full
/// medal collection screen and volume-based badges are still Android-only, ported in a later round).
struct HomeView: View {
    @EnvironmentObject private var auth: AuthViewModel
    @StateObject private var viewModel = DashboardViewModel()
    @State private var appeared = false

    var body: some View {
        NavigationStack {
            ZStack {
                VibeBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Bentornato 👋")
                            .font(.title2.bold())
                            .foregroundStyle(.white)
                            .padding(.top, 12)

                        HStack(spacing: 12) {
                            StatTile(value: "\(viewModel.completedSessions)", label: "Allenamenti")
                            StatTile(value: "\(viewModel.plans.count)", label: "Schede attive")
                        }

                        if viewModel.streakDays > 0 {
                            StreakCard(streakDays: viewModel.streakDays)
                        }

                        if let membership = viewModel.membership {
                            MembershipCard(membership: membership)
                        }

                        Text("Medagliere")
                            .font(.headline)
                            .foregroundStyle(.white)
                            .padding(.top, 8)
                        BadgeRow(icon: "flame.fill", milestones: Milestones.streakDays, currentValue: viewModel.longestStreakDays, unit: "gg")
                        BadgeRow(icon: "medal.fill", milestones: Milestones.workoutCount, currentValue: viewModel.completedSessions, unit: "")

                        Text("Prossima scheda")
                            .font(.headline)
                            .foregroundStyle(.white)
                            .padding(.top, 8)

                        if viewModel.isLoading {
                            ProgressView().tint(.vibeOrange).frame(maxWidth: .infinity).padding(.top, 20)
                        } else if let next = viewModel.plans.first {
                            PlanRow(plan: next)
                        } else {
                            GlassCard {
                                Text("Nessuna scheda assegnata ancora. Il tuo PT te ne assegnerà una a breve.")
                                    .font(.subheadline)
                                    .foregroundStyle(.white.opacity(0.6))
                                    .padding(16)
                            }
                        }

                        Spacer(minLength: 40)
                    }
                    .padding(.horizontal, 20)
                }
                .opacity(appeared ? 1 : 0)
                .animation(.easeOut(duration: 0.4), value: appeared)
                .refreshable { await viewModel.load() }
            }
            .navigationTitle("Vibe Fitness")
            .toolbarBackground(.hidden, for: .navigationBar)
        }
        .task {
            await viewModel.load()
            appeared = true
        }
    }
}

private struct StatTile: View {
    let value: String
    let label: String

    var body: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 4) {
                Text(value)
                    .font(.title2.bold())
                    .foregroundStyle(.white)
                Text(label)
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.55))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
        }
    }
}

private struct MembershipCard: View {
    let membership: Membership

    private var accent: Color {
        switch membership.status {
        case .active: return .green
        case .expiringSoon: return .yellow
        case .expired: return .red
        }
    }

    private var headline: String {
        switch membership.status {
        case .active: return "Abbonamento attivo"
        case .expiringSoon: return "In scadenza"
        case .expired: return "Abbonamento scaduto"
        }
    }

    var body: some View {
        GlassCard {
            HStack(spacing: 12) {
                Circle()
                    .fill(accent.opacity(0.18))
                    .frame(width: 40, height: 40)
                    .overlay(Circle().strokeBorder(accent.opacity(0.4), lineWidth: 1))
                    .overlay(Image(systemName: "creditcard.fill").font(.subheadline).foregroundStyle(accent))

                VStack(alignment: .leading, spacing: 2) {
                    Text(headline)
                        .font(.subheadline.bold())
                        .foregroundStyle(.white)
                    if let plan = membership.planLabel, !plan.isEmpty {
                        Text(plan)
                            .font(.caption)
                            .foregroundStyle(.white.opacity(0.55))
                    }
                }
                Spacer()
            }
            .padding(16)
        }
    }
}

private struct StreakCard: View {
    let streakDays: Int

    var body: some View {
        GlassCard {
            HStack(spacing: 12) {
                Circle()
                    .fill(LinearGradient(colors: [.vibeOrange, .vibeOrangeDeep], startPoint: .top, endPoint: .bottom))
                    .frame(width: 40, height: 40)
                    .overlay(Image(systemName: "flame.fill").font(.subheadline).foregroundStyle(.white))
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(streakDays) \(streakDays == 1 ? "giorno" : "giorni") di fila")
                        .font(.subheadline.bold())
                        .foregroundStyle(.white)
                    Text("Continua così per non perdere la streak")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.55))
                }
                Spacer()
            }
            .padding(16)
        }
    }
}

/// Every badge in one track (streak length or workout count), locked or unlocked - the compact
/// iOS counterpart of the Android app's dedicated Traguardi screen: a single row per track here
/// instead of a whole extra screen, since only two of the three tracks are ported yet.
private struct BadgeRow: View {
    let icon: String
    let milestones: [Int]
    let currentValue: Int
    let unit: String

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 14) {
                ForEach(milestones, id: \.self) { milestone in
                    let unlocked = currentValue >= milestone
                    VStack(spacing: 6) {
                        Circle()
                            .fill(
                                unlocked
                                    ? AnyShapeStyle(LinearGradient(colors: [.vibeOrange, .vibeOrangeDeep], startPoint: .top, endPoint: .bottom))
                                    : AnyShapeStyle(Color.white.opacity(0.08))
                            )
                            .frame(width: 48, height: 48)
                            .overlay(
                                Image(systemName: icon)
                                    .font(.subheadline)
                                    .foregroundStyle(unlocked ? .white : .white.opacity(0.3))
                            )
                        Text("\(milestone)\(unit)")
                            .font(.caption2.bold())
                            .foregroundStyle(unlocked ? .white.opacity(0.85) : .white.opacity(0.35))
                    }
                }
            }
            .padding(.vertical, 4)
        }
    }
}

private struct PlanRow: View {
    let plan: WorkoutPlan

    var body: some View {
        GlassCard {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(LinearGradient(colors: [.vibeOrange, .vibeOrangeDeep], startPoint: .top, endPoint: .bottom))
                        .frame(width: 44, height: 44)
                    Image(systemName: "dumbbell.fill")
                        .foregroundStyle(.white)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(plan.name)
                        .font(.subheadline.bold())
                        .foregroundStyle(.white)
                    if let category = plan.category {
                        Text([category, plan.estimatedMinutes.map { "~\($0) min" }].compactMap { $0 }.joined(separator: " · "))
                            .font(.caption)
                            .foregroundStyle(.white.opacity(0.55))
                    }
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(.white.opacity(0.3))
            }
            .padding(14)
        }
    }
}
