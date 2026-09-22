import Foundation

/// Reads Supabase credentials out of Resources/Secrets.plist instead of hardcoding them in source,
/// mirroring the Android app's local.properties pattern (see repo README). The real Secrets.plist
/// is gitignored; Codemagic writes it from its own encrypted environment variables just before
/// `xcodegen generate` runs (see codemagic.yaml). A missing/incomplete file fails loudly at
/// startup rather than silently pointing the app at an empty backend.
enum Secrets {
    static let supabaseURL: URL = {
        guard let value = plist["SUPABASE_URL"] as? String, let url = URL(string: value) else {
            fatalError("Missing/invalid SUPABASE_URL in Secrets.plist - copy Secrets.plist.example and fill it in.")
        }
        return url
    }()

    static let supabaseAnonKey: String = {
        guard let value = plist["SUPABASE_ANON_KEY"] as? String, !value.isEmpty, value != "REPLACE_ME" else {
            fatalError("Missing SUPABASE_ANON_KEY in Secrets.plist - copy Secrets.plist.example and fill it in.")
        }
        return value
    }()

    private static let plist: [String: Any] = {
        guard let path = Bundle.main.path(forResource: "Secrets", ofType: "plist"),
              let data = FileManager.default.contents(atPath: path),
              let parsed = try? PropertyListSerialization.propertyList(from: data, format: nil) as? [String: Any]
        else {
            fatalError("Secrets.plist not found in bundle - copy VibeFitness/Resources/Secrets.plist.example to Secrets.plist (or let Codemagic generate it) before building.")
        }
        return parsed
    }()
}
