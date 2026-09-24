import Foundation

/// Mirrors the `ai_messages` table and the Android app's `AiMessage` DTO.
struct AssistantMessage: Codable, Identifiable {
    var id: String { createdAt + role + String(content.prefix(8)) }
    let role: String
    let content: String
    let createdAt: String?

    enum CodingKeys: String, CodingKey {
        case role, content
        case createdAt = "created_at"
    }

    var isUser: Bool { role == "user" }
}

struct AiChatRequestBody: Encodable {
    let message: String
}

/// One chunk of the ai-chat Edge Function's SSE stream: `data: {"content":"..."}` per line, same
/// shape the Android app's `AiAssistantRepository` parses.
struct AiChatStreamChunk: Decodable {
    let content: String
}
