import SwiftUI

/// Full list of assigned workout plans - the iOS counterpart of the Android `WorkoutPlansScreen`.
/// Session tracking/logging itself isn't ported yet, so rows are read-only for now.
struct WorkoutPlansView: View {
    @StateObject private var viewModel = DashboardViewModel()

    var body: some View {
        NavigationStack {
            ZStack {
                VibeBackground()

                if viewModel.isLoading {
                    ProgressView().tint(.vibeOrange)
                } else if viewModel.plans.isEmpty {
                    VStack(spacing: 10) {
                        Image(systemName: "dumbbell")
                            .font(.system(size: 32))
                            .foregroundStyle(.white.opacity(0.3))
                        Text("Nessuna scheda assegnata ancora.")
                            .foregroundStyle(.white.opacity(0.6))
                    }
                } else {
                    ScrollView {
                        VStack(spacing: 10) {
                            ForEach(viewModel.plans) { plan in
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
                                            Text([plan.category, plan.estimatedMinutes.map { "~\($0) min" }].compactMap { $0 }.joined(separator: " · "))
                                                .font(.caption)
                                                .foregroundStyle(.white.opacity(0.55))
                                        }
                                        Spacer()
                                    }
                                    .padding(14)
                                }
                            }
                        }
                        .padding(20)
                    }
                    .refreshable { await viewModel.load() }
                }
            }
            .navigationTitle("Schede")
            .toolbarBackground(.hidden, for: .navigationBar)
        }
        .task { await viewModel.load() }
    }
}
