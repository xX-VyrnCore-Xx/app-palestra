import SwiftUI

/// Placeholder post-login shell. The Android app's real Home (streak, weekly goal, next workout,
/// PT dashboard, chat, plan editor, ...) isn't ported yet - this view exists so the iOS pipeline
/// (XcodeGen -> SPM -> Codemagic build -> TestFlight) has something real to build and sign while
/// the rest of the feature set gets ported screen by screen.
struct HomeView: View {
    @EnvironmentObject private var auth: AuthViewModel
    @State private var appeared = false

    var body: some View {
        NavigationStack {
            ZStack {
                VibeBackground()

                VStack(spacing: 20) {
                    Spacer()

                    ZStack {
                        Circle()
                            .fill(
                                LinearGradient(colors: [.vibeOrange, .vibeOrangeDeep], startPoint: .top, endPoint: .bottom)
                            )
                            .frame(width: 88, height: 88)
                            .shadow(color: .vibeOrange.opacity(0.5), radius: 20, x: 0, y: 8)

                        Image(systemName: "figure.strengthtraining.traditional")
                            .font(.system(size: 38, weight: .semibold))
                            .foregroundStyle(.white)
                    }
                    .scaleEffect(appeared ? 1 : 0.6)
                    .opacity(appeared ? 1 : 0)
                    .animation(.spring(response: 0.5, dampingFraction: 0.65), value: appeared)

                    Text("Sei dentro!")
                        .font(.title2.bold())
                        .foregroundStyle(.white)

                    Text("Il resto delle schermate arriva nei prossimi giri di porting.")
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(0.7))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 40)

                    Spacer()

                    Button("Esci", role: .destructive) {
                        Task { await auth.signOut() }
                    }
                    .buttonStyle(.bordered)
                    .tint(.red)
                    .padding(.bottom, 32)
                }
                .opacity(appeared ? 1 : 0)
                .animation(.easeOut(duration: 0.4).delay(0.1), value: appeared)
            }
            .navigationTitle("Vibe Fitness")
            .toolbarBackground(.hidden, for: .navigationBar)
        }
        .onAppear { appeared = true }
    }
}
