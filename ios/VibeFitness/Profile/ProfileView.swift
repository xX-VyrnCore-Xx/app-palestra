import SwiftUI

/// Read-only profile summary + sign out - the iOS counterpart of the Android `ProfileScreen`'s
/// core (avatar, name, email, logout). Editing profile fields isn't ported yet.
struct ProfileView: View {
    @EnvironmentObject private var auth: AuthViewModel
    @State private var profile: Profile?
    @State private var isLoading = true

    var body: some View {
        NavigationStack {
            ZStack {
                VibeBackground()

                VStack(spacing: 20) {
                    if isLoading {
                        ProgressView().tint(.vibeOrange)
                    } else if let profile {
                        VStack(spacing: 12) {
                            ZStack {
                                Circle()
                                    .fill(LinearGradient(colors: [.vibeOrange, .vibeOrangeDeep], startPoint: .top, endPoint: .bottom))
                                    .frame(width: 76, height: 76)
                                Text(initials(for: profile.fullName))
                                    .font(.title2.bold())
                                    .foregroundStyle(.white)
                            }
                            Text(profile.fullName)
                                .font(.title3.bold())
                                .foregroundStyle(.white)
                            Text(profile.email)
                                .font(.subheadline)
                                .foregroundStyle(.white.opacity(0.55))
                        }
                        .padding(.top, 24)

                        if let inviteCode = profile.inviteCode, !inviteCode.isEmpty {
                            GlassCard {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("CODICE INVITO")
                                        .font(.caption2.bold())
                                        .foregroundStyle(.white.opacity(0.4))
                                    Text(inviteCode)
                                        .font(.title3.bold())
                                        .tracking(2)
                                        .foregroundStyle(.vibeOrange)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(16)
                            }
                            .padding(.horizontal, 20)
                        }
                    }

                    Spacer()

                    Button("Esci", role: .destructive) {
                        Task { await auth.signOut() }
                    }
                    .buttonStyle(.bordered)
                    .tint(.red)
                    .padding(.bottom, 32)
                }
            }
            .navigationTitle("Profilo")
            .toolbarBackground(.hidden, for: .navigationBar)
        }
        .task { await load() }
    }

    private func load() async {
        isLoading = true
        defer { isLoading = false }
        guard let userId = try? await SupabaseService.client.auth.session.user.id.uuidString else { return }
        profile = try? await SupabaseService.client.from("profiles")
            .select()
            .eq("id", value: userId)
            .single()
            .execute()
            .value
    }

    private func initials(for name: String) -> String {
        let parts = name.split(separator: " ")
        let letters = parts.prefix(2).compactMap { $0.first }
        return letters.isEmpty ? "?" : String(letters).uppercased()
    }
}
