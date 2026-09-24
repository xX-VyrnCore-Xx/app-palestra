import SwiftUI

/// AI training assistant - the iOS counterpart of the Android app's `AiAssistantScreen`.
struct AssistantView: View {
    @StateObject private var viewModel = AssistantViewModel()
    @State private var draft = ""

    var body: some View {
        NavigationStack {
            ZStack {
                VibeBackground()

                VStack(spacing: 0) {
                    if viewModel.messages.isEmpty {
                        Spacer()
                        VStack(spacing: 10) {
                            Image(systemName: "sparkles")
                                .font(.system(size: 32))
                                .foregroundStyle(.vibeOrange)
                            Text("Chiedi consigli sull'allenamento, sulla scheda o sui progressi.")
                                .foregroundStyle(.white.opacity(0.55))
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 40)
                        }
                        Spacer()
                    } else {
                        ScrollViewReader { proxy in
                            ScrollView {
                                LazyVStack(spacing: 8) {
                                    ForEach(Array(viewModel.messages.enumerated()), id: \.offset) { index, message in
                                        AssistantBubble(message: message)
                                            .id(index)
                                    }
                                    if viewModel.isSending {
                                        HStack {
                                            ProgressView().tint(.white.opacity(0.6))
                                            Spacer()
                                        }
                                        .padding(.horizontal, 16)
                                    }
                                }
                                .padding(16)
                            }
                            .onChange(of: viewModel.messages.count) { _, _ in
                                withAnimation { proxy.scrollTo(viewModel.messages.count - 1, anchor: .bottom) }
                            }
                        }
                    }

                    if let error = viewModel.errorMessage {
                        Text(error)
                            .font(.caption)
                            .foregroundStyle(.red)
                            .padding(.horizontal, 16)
                    }

                    HStack(spacing: 10) {
                        TextField("Chiedi qualcosa…", text: $draft)
                            .textFieldStyle(VibeTextFieldStyle())
                            .disabled(viewModel.isSending)
                        Button {
                            let text = draft
                            draft = ""
                            Task { await viewModel.send(text) }
                        } label: {
                            Image(systemName: "arrow.up.circle.fill")
                                .font(.system(size: 32))
                                .foregroundStyle(.vibeOrange)
                        }
                        .disabled(draft.trimmingCharacters(in: .whitespaces).isEmpty || viewModel.isSending)
                    }
                    .padding(12)
                }
            }
            .navigationTitle("Assistente")
            .toolbarBackground(.hidden, for: .navigationBar)
        }
        .task { await viewModel.loadHistory() }
    }
}

private struct AssistantBubble: View {
    let message: AssistantMessage

    var body: some View {
        HStack {
            if message.isUser { Spacer(minLength: 40) }
            Text(message.content)
                .font(.subheadline)
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(
                    message.isUser
                        ? AnyShapeStyle(LinearGradient(colors: [.vibeOrange, .vibeOrangeDeep], startPoint: .top, endPoint: .bottom))
                        : AnyShapeStyle(Color.white.opacity(0.1)),
                    in: RoundedRectangle(cornerRadius: 18, style: .continuous)
                )
            if !message.isUser { Spacer(minLength: 40) }
        }
    }
}
