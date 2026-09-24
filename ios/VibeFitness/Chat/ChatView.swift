import SwiftUI

/// Chat with your PT - the iOS counterpart of the Android app's `AllievoChatScreen`, talking to
/// the same `messages` table the PT gestionale reads and writes.
struct ChatView: View {
    @StateObject private var viewModel = ChatViewModel()
    @State private var draft = ""

    var body: some View {
        NavigationStack {
            ZStack {
                VibeBackground()

                VStack(spacing: 0) {
                    if viewModel.isLoading {
                        Spacer()
                        ProgressView().tint(.vibeOrange)
                        Spacer()
                    } else if viewModel.messages.isEmpty {
                        Spacer()
                        VStack(spacing: 10) {
                            Image(systemName: "bubble.left.and.bubble.right")
                                .font(.system(size: 32))
                                .foregroundStyle(.white.opacity(0.3))
                            Text("Nessun messaggio ancora. Scrivi al tuo PT!")
                                .foregroundStyle(.white.opacity(0.55))
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 40)
                        }
                        Spacer()
                    } else {
                        ScrollViewReader { proxy in
                            ScrollView {
                                LazyVStack(spacing: 8) {
                                    ForEach(viewModel.messages) { message in
                                        MessageBubble(message: message, isMine: message.senderId == viewModel.myUserId)
                                            .id(message.id)
                                    }
                                }
                                .padding(16)
                            }
                            .onChange(of: viewModel.messages.count) { _, _ in
                                if let last = viewModel.messages.last {
                                    withAnimation { proxy.scrollTo(last.id, anchor: .bottom) }
                                }
                            }
                        }
                    }

                    HStack(spacing: 10) {
                        TextField("Scrivi un messaggio…", text: $draft)
                            .textFieldStyle(VibeTextFieldStyle())
                        Button {
                            let text = draft
                            draft = ""
                            Task { await viewModel.send(text) }
                        } label: {
                            Image(systemName: "arrow.up.circle.fill")
                                .font(.system(size: 32))
                                .foregroundStyle(.vibeOrange)
                        }
                        .disabled(draft.trimmingCharacters(in: .whitespaces).isEmpty)
                    }
                    .padding(12)
                }
            }
            .navigationTitle(viewModel.ptName ?? "Chat")
            .toolbarBackground(.hidden, for: .navigationBar)
        }
        .onAppear { viewModel.start() }
        .onDisappear { viewModel.stop() }
    }
}

private struct MessageBubble: View {
    let message: ChatMessage
    let isMine: Bool

    var body: some View {
        HStack {
            if isMine { Spacer(minLength: 40) }
            Text(message.content)
                .font(.subheadline)
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(
                    isMine
                        ? AnyShapeStyle(LinearGradient(colors: [.vibeOrange, .vibeOrangeDeep], startPoint: .top, endPoint: .bottom))
                        : AnyShapeStyle(Color.white.opacity(0.1)),
                    in: RoundedRectangle(cornerRadius: 18, style: .continuous)
                )
            if !isMine { Spacer(minLength: 40) }
        }
    }
}
