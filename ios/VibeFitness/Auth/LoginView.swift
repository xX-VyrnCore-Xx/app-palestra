import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var auth: AuthViewModel
    @State private var email = ""
    @State private var password = ""
    @State private var passwordVisible = false
    @State private var appeared = false
    var onNavigateToRegister: () -> Void

    var body: some View {
        ZStack {
            VibeBackground()

            ScrollView {
                VStack(spacing: 20) {
                    Spacer().frame(height: 48)

                    VStack(spacing: 8) {
                        Image(systemName: "figure.strengthtraining.traditional")
                            .font(.system(size: 36, weight: .semibold))
                            .foregroundStyle(
                                LinearGradient(colors: [.vibeOrangeLight, .vibeOrange], startPoint: .top, endPoint: .bottom)
                            )
                        Text("Vibe Fitness")
                            .font(.largeTitle.bold())
                            .foregroundStyle(.white)
                        Text("Bentornato. Ogni ripetizione conta.")
                            .font(.subheadline)
                            .foregroundStyle(.white.opacity(0.7))
                    }
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : -12)
                    .animation(.easeOut(duration: 0.5), value: appeared)

                    GlassCard {
                        VStack(spacing: 14) {
                            Text("Accedi")
                                .font(.title3.bold())
                                .foregroundStyle(.white)
                                .frame(maxWidth: .infinity, alignment: .leading)

                            TextField("Email", text: $email)
                                .textContentType(.emailAddress)
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                                .textFieldStyle(VibeTextFieldStyle())

                            HStack {
                                Group {
                                    if passwordVisible {
                                        TextField("Password", text: $password)
                                    } else {
                                        SecureField("Password", text: $password)
                                    }
                                }
                                .textContentType(.password)

                                Button {
                                    passwordVisible.toggle()
                                } label: {
                                    Image(systemName: passwordVisible ? "eye.slash" : "eye")
                                        .foregroundStyle(.white.opacity(0.6))
                                }
                            }
                            .textFieldStyle(VibeTextFieldStyle())

                            if let error = auth.errorMessage {
                                Text(error)
                                    .font(.footnote)
                                    .foregroundStyle(.red.opacity(0.9))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }

                            Button {
                                Task { await auth.signIn(email: email, password: password) }
                            } label: {
                                if auth.isLoading {
                                    ProgressView().tint(.white)
                                } else {
                                    Text("Accedi")
                                }
                            }
                            .buttonStyle(VibeButtonStyle(isLoading: auth.isLoading, enabled: !email.isEmpty && !password.isEmpty))
                            .disabled(auth.isLoading || email.isEmpty || password.isEmpty)
                            .padding(.top, 4)
                        }
                        .padding(24)
                    }
                    .padding(.horizontal, 24)
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 16)
                    .animation(.easeOut(duration: 0.5).delay(0.1), value: appeared)

                    Button("Non hai un account? Registrati", action: onNavigateToRegister)
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(0.8))

                    Spacer().frame(height: 24)
                }
                .padding(.horizontal, 4)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .onAppear { appeared = true }
    }
}
