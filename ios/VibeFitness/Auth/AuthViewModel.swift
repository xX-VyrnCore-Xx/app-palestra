import Foundation
import Supabase

/// Session state for the whole app: nil while unresolved/signed-out, set once Supabase confirms
/// a session on launch or after sign-in. Mirrors the Android app's AuthViewModel/RootViewModel
/// split at a much smaller scale - this is the iOS starting point, not full feature parity yet.
@MainActor
final class AuthViewModel: ObservableObject {
    @Published var userId: String?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let client = SupabaseService.client

    init() {
        Task { await restoreSession() }
    }

    private func restoreSession() async {
        do {
            let session = try await client.auth.session
            userId = session.user.id.uuidString
        } catch {
            userId = nil
        }
    }

    func signIn(email: String, password: String) async {
        guard !email.isEmpty, !password.isEmpty else {
            errorMessage = "Inserisci email e password."
            return
        }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            let session = try await client.auth.signIn(email: email, password: password)
            userId = session.user.id.uuidString
        } catch {
            errorMessage = "Accesso non riuscito. Controlla email e password."
        }
    }

    func signUp(email: String, password: String, fullName: String, role: String) async {
        guard !email.isEmpty, password.count >= 8, !fullName.isEmpty else {
            errorMessage = "Compila tutti i campi (password: almeno 8 caratteri)."
            return
        }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            let session = try await client.auth.signUp(email: email, password: password)
            let userId = session.user.id.uuidString
            try await client.from("profiles").upsert([
                "id": userId,
                "email": email,
                "full_name": fullName,
                "role": role,
            ]).execute()
            self.userId = userId
        } catch {
            errorMessage = "Registrazione non riuscita. Riprova."
        }
    }

    func signOut() async {
        try? await client.auth.signOut()
        userId = nil
    }
}
