import Foundation

/// Mirrors the `messages` table - the same one the Android app's `ChatRepository` and the PT
/// gestionale's inbox both read and write, so a message sent from any of the three surfaces shows
/// up in the other two.
struct ChatMessage: Codable, Identifiable {
    let id: String
    let senderId: String
    let recipientId: String
    let content: String
    let createdAt: String
    let readAt: String?

    enum CodingKeys: String, CodingKey {
        case id
        case senderId = "sender_id"
        case recipientId = "recipient_id"
        case content
        case createdAt = "created_at"
        case readAt = "read_at"
    }
}
