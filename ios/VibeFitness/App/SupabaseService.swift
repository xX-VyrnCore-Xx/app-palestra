import Foundation
import Supabase

/// Single shared Supabase client for the whole app, mirroring the Android app's SupabaseProvider -
/// one client, injected wherever it's needed, instead of each screen creating its own.
enum SupabaseService {
    static let client = SupabaseClient(
        supabaseURL: Secrets.supabaseURL,
        supabaseKey: Secrets.supabaseAnonKey
    )
}

/// Minimal mirror of the `profiles` row this app cares about at launch - role decides which
/// dashboard shell to show, matching the Android app's PT/Allievo split.
struct Profile: Codable {
    let id: String
    let fullName: String
    let email: String
    let role: String
    let inviteCode: String?
    let ptId: String?

    enum CodingKeys: String, CodingKey {
        case id
        case fullName = "full_name"
        case email
        case role
        case inviteCode = "invite_code"
        case ptId = "pt_id"
    }
}
