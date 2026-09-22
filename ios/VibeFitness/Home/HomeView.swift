import SwiftUI

/// Placeholder post-login shell. The Android app's real Home (streak, weekly goal, next workout,
/// PT dashboard, chat, plan editor, ...) isn't ported yet - this view exists so the iOS pipeline
/// (XcodeGen -> SPM -> Codemagic build -> TestFlight) has something real to build and sign while
/// the rest of the feature set gets ported screen by screen.
struct HomeView: View {
    @EnvironmentObject private var auth: AuthViewModel

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Image(systemName: "figure.strengthtraining.traditional")
                    .font(.system(size: 48))
                    .foregroundStyle(.tint)
                Text("Sei dentro!")
                    .font(.title2.bold())
                Text("Il resto delle schermate arriva nei prossimi giri di porting.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)

                Button("Esci", role: .destructive) {
                    Task { await auth.signOut() }
                }
                .padding(.top, 24)
            }
            .navigationTitle("Vibe Fitness")
        }
    }
}
