import Foundation

/// Chat with the allievo's own PT. Simple 5s polling instead of a realtime subscription - keeps
/// this first iOS port of chat small and dependency-light; the Android app and the PT gestionale
/// already use realtime, so this is the one surface that lags a few seconds, not the other way
/// around. Reading the thread here marks the PT's messages as read, same as the gestionale.
@MainActor
final class ChatViewModel: ObservableObject {
    @Published var messages: [ChatMessage] = []
    @Published var ptName: String?
    @Published var isLoading = true

    private let client = SupabaseService.client
    private var myId: String?
    private var ptId: String?
    private var pollTask: Task<Void, Never>?

    func start() {
        pollTask?.cancel()
        pollTask = Task {
            await loadPeer()
            while !Task.isCancelled {
                await reload()
                try? await Task.sleep(nanoseconds: 5_000_000_000)
            }
        }
    }

    func stop() {
        pollTask?.cancel()
        pollTask = nil
    }

    private func loadPeer() async {
        guard let userId = try? await client.auth.session.user.id.uuidString else { return }
        myId = userId
        guard let profile: Profile = try? await client.from("profiles")
            .select()
            .eq("id", value: userId)
            .single()
            .execute()
            .value else { return }
        ptId = profile.ptId
        if let ptId = profile.ptId {
            let pt: Profile? = try? await client.from("profiles")
                .select()
                .eq("id", value: ptId)
                .single()
                .execute()
                .value
            ptName = pt?.fullName
        }
    }

    private func reload() async {
        guard let myId, let ptId else { isLoading = false; return }
        defer { isLoading = false }

        let mine: [ChatMessage] = (try? await client.from("messages")
            .select()
            .eq("sender_id", value: myId)
            .eq("recipient_id", value: ptId)
            .execute()
            .value) ?? []
        let theirs: [ChatMessage] = (try? await client.from("messages")
            .select()
            .eq("sender_id", value: ptId)
            .eq("recipient_id", value: myId)
            .execute()
            .value) ?? []

        messages = (mine + theirs).sorted { $0.createdAt < $1.createdAt }

        let unreadIds = theirs.filter { $0.readAt == nil }.map { $0.id }
        if !unreadIds.isEmpty {
            try? await client.from("messages")
                .update(["read_at": ISO8601DateFormatter().string(from: Date())])
                .in("id", values: unreadIds)
                .execute()
        }
    }

    func send(_ text: String) async {
        guard let myId, let ptId else { return }
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        struct NewMessage: Encodable {
            let sender_id: String
            let recipient_id: String
            let content: String
        }
        try? await client.from("messages")
            .insert(NewMessage(sender_id: myId, recipient_id: ptId, content: trimmed))
            .execute()
        await reload()
    }

    var myUserId: String? { myId }
}
