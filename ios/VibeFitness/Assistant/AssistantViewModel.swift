import Foundation
import Supabase

/// Talks to the same `ai-chat` Supabase Edge Function the Android app's `AiAssistantRepository`
/// uses - the NVIDIA NIM API key never ships in either app, only in the Edge Function. The
/// function streams its reply as Server-Sent Events; unlike Android this iOS port waits for the
/// full response and decodes it in one pass instead of drip-feeding the UI token by token, to keep
/// this first port simple.
@MainActor
final class AssistantViewModel: ObservableObject {
    @Published var messages: [AssistantMessage] = []
    @Published var isSending = false
    @Published var errorMessage: String?

    private let client = SupabaseService.client

    func loadHistory() async {
        guard let userId = try? await client.auth.session.user.id.uuidString else { return }
        let history: [AssistantMessage] = (try? await client.from("ai_messages")
            .select()
            .eq("user_id", value: userId)
            .order("created_at", ascending: true)
            .execute()
            .value) ?? []
        messages = history
    }

    func send(_ text: String) async {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, !isSending else { return }
        errorMessage = nil
        messages.append(AssistantMessage(role: "user", content: trimmed, createdAt: nil))
        isSending = true
        defer { isSending = false }

        do {
            let data = try await client.functions.invoke(
                "ai-chat",
                options: FunctionInvokeOptions(body: AiChatRequestBody(message: trimmed))
            )
            let reply = parseSSE(data)
            guard !reply.isEmpty else {
                errorMessage = "Risposta AI vuota"
                return
            }
            messages.append(AssistantMessage(role: "assistant", content: reply, createdAt: nil))
        } catch {
            errorMessage = "Errore sconosciuto dell'assistente AI"
        }
    }

    /// Same line-by-line "data: {...}" parsing as the Android app, just applied to the fully
    /// downloaded body instead of a live channel.
    private func parseSSE(_ data: Data) -> String {
        guard let text = String(data: data, encoding: .utf8) else { return "" }
        var full = ""
        for line in text.split(separator: "\n") {
            guard line.hasPrefix("data:") else { continue }
            let payload = line.dropFirst(5).trimmingCharacters(in: .whitespaces)
            guard !payload.isEmpty, payload != "[DONE]" else { continue }
            guard let payloadData = payload.data(using: .utf8),
                  let chunk = try? JSONDecoder().decode(AiChatStreamChunk.self, from: payloadData) else { continue }
            full += chunk.content
        }
        return full
    }
}
